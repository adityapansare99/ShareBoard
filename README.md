# Planar — Real-time Collaborative Kanban Board

Planar is a multi-user Kanban board (like Trello) where teams collaborate in real-time. Built as a teaching-grade project that demonstrates production Spring Boot engineering: design patterns, concurrency control, algorithmic thinking, and a polished React frontend.

```
React (Vite) ──HTTP + WebSocket──▶ Spring Boot 3 API ──▶ Postgres / H2
```

---

## Tech Stack

| Layer | Technology | What it does |
|---|---|---|
| **Backend** | Java 21 + Spring Boot 3.4 | REST API, WebSocket, JPA, Security |
| **Frontend** | React 18 + Vite + Tailwind CSS 3 | SPA with drag-and-drop Kanban |
| **Database** | PostgreSQL (prod) / H2 (dev) | JPA-managed relational store |
| **Auth** | Email + password + OTP | BCrypt hashing, session-based auth |
| **WebSocket** | Raw Spring WebSocket | Real-time broadcast of card mutations |

---

## Quick Start (2 minutes)

```bash
# Prerequisites: Java 21+, Node 18+, Maven

# 1. Start the backend (starts on port 8080 with H2 in-memory DB)
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 2. Start the frontend (in a separate terminal)
cd frontend
npm install
npm run dev

# 3. Open http://localhost:5173 — register an account, create a board, drag cards
```

> No OAuth client ID needed. No Postgres needed. The `dev` profile uses H2 in-memory and permits all requests.

---

## Spring Boot 101 — How Planar is Structured

### Layered Architecture

```
Controller (HTTP) → Service (Business Logic) → Repository (Database)
                         ↕
                   Design Patterns (Command, State, Observer, Strategy, Proxy)
                         ↕
                   WebSocket Broadcaster (real-time sync)
```

### 1. Controllers (`@RestController`)

Controllers receive HTTP requests and return JSON. They're thin — no business logic, just routing.

```java
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public ResponseEntity<List<BoardDTO>> listBoards() {
        return ResponseEntity.ok(boardService.listBoards());
        // Spring auto-serializes the List<BoardDTO> to JSON via Jackson
    }
}
```

**Key controllers:**
- `AuthController` — registration (with OTP), login, logout, session check
- `BoardController` — CRUD for boards + join-by-code
- `CardController` — CRUD for cards, move, state transitions, activity log
- `UndoController` — undo/redo per WebSocket session

### 2. Services (`@Service`)

Services contain business logic. They're injected into controllers via constructor injection (Lombok's `@RequiredArgsConstructor` generates the constructor).

```java
@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository boardRepository;
    // Spring injects the repository automatically

    @Transactional
    public Board createBoard(String name, String description) {
        // All DB operations in this method run in one transaction
        // If anything fails, everything rolls back
        Board board = boardRepository.save(Board.builder().name(name).build());
        // ... also creates default columns and adds creator as member
        return board;
    }
}
```

### 3. Repositories (`JpaRepository`)

Spring Data JPA generates the implementation at runtime. You just declare the interface:

```java
public interface BoardRepository extends JpaRepository<Board, Long> {
    Optional<Board> findByCode(String code);

    @Query("SELECT b FROM Board b WHERE b.code = :code LEFT JOIN FETCH b.columns")
    Optional<Board> findByCodeWithColumns(@Param("code") String code);

    @Query("SELECT b FROM Board b JOIN BoardMember m ON m.board = b WHERE m.userEmail = :email")
    List<Board> findAccessibleBoards(@Param("email") String email);
}
```

Method names like `findByCode` are parsed by Spring Data — it generates `WHERE code = ?` automatically. For complex queries, use `@Query` with JPQL.

### 4. JPA Entities (`@Entity`)

Each entity maps to a database table. JPA annotations define the mapping:

```java
@Entity
@Table(name = "cards")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;             // Auto-increment primary key

    @Column(nullable = false)
    private String title;        // Maps to VARCHAR column

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardState state;     // Stored as VARCHAR in DB (TODO, IN_PROGRESS, etc.)

    @Version
    private Long version;        // Optimistic locking — prevents lost updates on concurrent edits

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "column_id", nullable = false)
    private BoardColumn column;  // Foreign key to board_columns table

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();  // Set timestamp before first save
    }
}
```

### 5. Security — Email + Password Auth

Planar uses email/password with BCrypt + OTP verification:

```
Register → Send OTP (6-digit code) → Verify OTP → BCrypt(password) → Save User
Login    → Load User by email → BCrypt.verify(password) → Create HTTP Session
```

- `SecurityConfig` (default profile) — requires auth for `/api/**` except `/api/auth/**`
- `DevSecurityConfig` (dev profile) — permits all requests for development
- `AuthService` — handles registration (validates OTP via `OtpService`, hashes password via BCrypt)
- `CustomUserDetailsService` — loads users by email for Spring Security's auth manager
- `SecurityContextRepository` — persists the authentication to the HTTP session

### 6. WebSockets — Real-time Sync

When a card is created/moved/edited/deleted, the server broadcasts the event to all connected clients for that board:

```
Client A moves card ──▶ POST /api/boards/{code}/cards/move
                            │
                    CardService.moveCard()
                            │
                    BoardEventBroadcaster.broadcast(code, event)
                            │
                    WebSocket ──▶ Client A ──▶ BoardView updates
                                 ──▶ Client B ──▶ BoardView updates
```

The WebSocket handler expects a subscription protocol:

```json
// Client → Server (on connect)
{ "type": "SUBSCRIBE", "boardCode": "XK4M9P", "sessionId": "abc123" }

// Server → Client (on events)
{ "type": "CARD_MOVED", "cardId": 42, "fromColumnId": 1, "toColumnId": 2, "userEmail": "alice@example.com" }
```

---

## Drag-and-Drop Deep Dive

This is the most complex interaction in Planar. Here's exactly how it works, line by line.

### Frontend (React)

**Step 1: Start dragging — `Card.jsx`**

```jsx
const handleDragStart = (e) => {
    e.dataTransfer.setData('text/plain', card.id.toString());
    e.currentTarget.classList.add('opacity-50', 'scale-95');  // Visual feedback
    onDragStart(card);  // Lift state: tells BoardView what card is being dragged
};
```

- The HTML5 Drag and Drop API fires `onDragStart` when the user starts dragging a `draggable` element
- `e.dataTransfer.setData()` stores the card ID for potential drop handlers — standard practice for DnD
- `onDragStart(card)` lifts the card object up to `BoardView` which stores it in `draggedCard` state

**Step 2: Allow dropping — `Column.jsx`**

```jsx
const handleDragOver = (e) => {
    e.preventDefault();  // Without this, the browser rejects the drop
    e.currentTarget.classList.add('column-drop-target');  // Visual highlight
};
```

- `e.preventDefault()` is required on `onDragOver` or `onDrop` will never fire — this is an HTML5 DnD requirement
- We add a CSS class that renders a dashed blue outline: `outline: 2px dashed #4338ca`

**Step 3: Drop — `Column.jsx`**

```jsx
const handleDrop = (e) => {
    e.preventDefault();
    e.currentTarget.classList.remove('column-drop-target');
    onDrop(column.id);  // Tell BoardView which column the card was dropped on
};
```

**Step 4: Optimistic update + API call — `BoardView.jsx`**

```jsx
function handleDrop(targetColumnId) {
    if (!draggedCard) return;
    const sourceColId = draggedCard.columnId;
    const cardToMove = draggedCard;
    setDraggedCard(null);  // Reset drag state

    // 1. OPTIMISTIC UPDATE: Update the UI immediately without waiting for the server
    setBoard(prev => {
        const updatedCols = prev.columns.map(col => {
            if (col.id === sourceColId) {
                return { ...col, cards: col.cards.filter(c => c.id !== cardToMove.id) };
            }
            if (col.id === targetColumnId) {
                const cardCopy = { ...cardToMove, columnId: targetColumnId };
                return { ...col, cards: [...col.cards, cardCopy] };
            }
            return col;
        });
        return { ...prev, columns: updatedCols };
    });

    // 2. ACTUAL API CALL — if it fails, reload to revert optimistic update
    moveCard(code, {
        cardId: cardToMove.id,
        toColumnId: targetColumnId,
        newPosition: newPos,
        sessionId,
    }).catch(() => reload());
}
```

### Backend (Spring Boot)

**Step 5: MoveCardCommand — the Command Pattern**

```java
CommandInvoker invoker = undoRedoService.getInvoker(sessionId);
MoveCardCommand command = new MoveCardCommand(cardId, toColumnId, newPosition, ...);
invoker.execute(command);
```

Inside `MoveCardCommand.execute()`:
1. Load the Card from DB
2. Snapshot `oldPosition`, `oldColumnId`, `oldState` — needed for undo
3. Set new column and position
4. Update `CardState` to match the target column name (e.g., dropped in "Done" → state becomes `DONE`)
5. Save — if the `@Version` field changed since we loaded, `ObjectOptimisticLockingFailureException` is thrown

**Step 6: Undo/Redo — per-session stack**

Each WebSocket session gets its own `CommandInvoker` stack. User A's undo never affects User B's actions.

```
Undo stack: [CreateCard, MoveCard, EditCard, MoveCard]
                                    ↑
User clicks Undo → pops MoveCard → calls command.undo()
→ card goes back to old column, old position, old state
```

### The Full Flow

```
User drags card "Fix login bug" from "In Progress" to "Done"
    │
    ▼
onDragStart(card)          — Card.jsx: records which card is being dragged
    │
    ▼
onDragOver(e)              — Column.jsx: e.preventDefault() + highlight border
    │
    ▼
onDrop("done-column-3")    — Column.jsx: tells BoardView the target column
    │
    ├─▶ setBoard(optimistic) — UI updates instantly (no waiting for server)
    │
    ▼
moveCard(API call)         — POST /api/boards/XK4M9P/cards/move
    │
    ▼
CommandInvoker.execute()   — Pushes MoveCardCommand onto session's undo stack
    │
    ├─▶ JPA save (optimistic locking check)
    │
    ▼
BoardEventBroadcaster     — WebSocket broadcast to ALL clients on that board
    │
    ├─▶ Other clients receive "CARD_MOVED" → reload board data
    │
    ▼
AuditService.log()         — "MOVE_CARD" entry in audit_log table
```

---

## Project Structure

```
planar/
├── backend/
│   ├── src/main/java/com/cb/
│   │   ├── algo/            # Topological sort, deadline priority queue
│   │   ├── config/          # Security configs, UserContext, WebSocket, Auth beans
│   │   ├── controller/      # REST endpoints (Auth, Board, Card, Undo)
│   │   ├── dto/              # Data Transfer Objects
│   │   ├── model/           # JPA entities (Board, Card, User, AuditLog)
│   │   ├── patterns/
│   │   │   ├── command/     # Command pattern (undo/redo stack)
│   │   │   ├── observer/    # WebSocket event broadcasting
│   │   │   ├── proxy/       # Token bucket rate limiter
│   │   │   ├── state/       # Card lifecycle state machine
│   │   │   └── strategy/    # View rendering (Kanban, Calendar, List)
│   │   ├── repository/      # Spring Data JPA interfaces
│   │   ├── security/        # AuthService, UserDetails, exception handler
│   │   ├── service/         # Business logic (Board, Card, Audit, OTP, Email)
│   │   └── websocket/       # WebSocket handler + subscription management
│   └── src/main/resources/
│       ├── application.yml      # Main config (H2 defaults, SMTP, OAuth fallbacks)
│       └── application-dev.yml  # Dev profile (permit-all security, H2)
├── frontend/
│   └── src/
│       ├── components/      # Login, Dashboard, BoardView, Column, Card, CardModal, ...
│       ├── services/        # api.js (axios client), ws.js (WebSocket client)
│       ├── hooks/           # useBoard custom hook
│       └── styles/          # index.css (tailwind + custom animations)
├── docs/                    # Architecture docs, API reference, implementation plans
└── README.md
```

---

## Design Patterns Used

| Pattern | Where | Why |
|---|---|---|
| **Command** | `command/` package | Every card mutation is a Command. The CommandInvoker maintains undo/redo stacks per session. `execute()` does the work, `undo()` reverses it. |
| **Observer** | `BoardEventBroadcaster` + WS clients | When a card changes, all connected clients are notified. The broadcaster is the subject; WebSocket sessions are observers. |
| **State Machine** | `CardStateMachine` | Cards follow TODO → IN_PROGRESS → REVIEW → DONE. Invalid transitions (TODO→DONE) are rejected. |
| **Strategy** | `viewStrategies` map | Board renders as Kanban, Calendar, or List. Each strategy implements `BoardViewStrategy.render(Board)`. |
| **Proxy** | `RateLimitingFilter` + `TokenBucket` | A `OncePerRequestFilter` checks a token bucket before mutation endpoints. If empty, returns 429. |

---

## API Reference

| Method | Endpoint | What it does |
|---|---|---|
| `POST` | `/api/auth/send-otp` | Send 6-digit OTP to email (for registration) |
| `POST` | `/api/auth/register` | Create account (email, name, password, OTP) |
| `POST` | `/api/auth/login` | Login, creates HTTP session |
| `GET` | `/api/auth/me` | Check if authenticated, returns user info |
| `POST` | `/api/auth/logout` | Destroy session |
| `GET` | `/api/boards` | List boards for current user |
| `POST` | `/api/boards` | Create a board |
| `GET` | `/api/boards/{code}?view=kanban` | Get board with columns + cards |
| `DELETE` | `/api/boards/{code}` | Delete board |
| `POST` | `/api/boards/join` | Join board by 6-character code |
| `POST` | `/api/boards/{code}/cards` | Create card in a column |
| `PATCH` | `/api/boards/{code}/cards/{id}` | Edit card fields |
| `DELETE` | `/api/boards/{code}/cards/{id}` | Delete card |
| `POST` | `/api/boards/{code}/cards/move` | Move card between columns |
| `POST` | `/api/boards/{code}/cards/{id}/state` | Change card state |
| `POST` | `/api/boards/{code}/undo` | Undo last action |
| `POST` | `/api/boards/{code}/redo` | Redo undone action |
| `GET` | `/api/boards/{code}/history` | Get undo/redo stack state |
| `GET` | `/api/boards/{code}/activity` | Get audit log for board |
| WebSocket | `/ws` | Real-time events (CARD_ADDED, CARD_MOVED, etc.) |

---

## Deployment

### Backend — Production JAR

```bash
cd backend
mvn clean package -DskipTests
java -jar target/planar-1.0.0.jar \
    --spring.profiles.active=default \
    --PLANAR_DB_URL=jdbc:postgresql://your-host:5432/planar \
    --PLANAR_DB_USERNAME=postgres \
    --PLANAR_DB_PASSWORD=your-password \
    --PLANAR_DB_DRIVER=org.postgresql.Driver
```

Or set environment variables:
```bash
export PLANAR_DB_URL=jdbc:postgresql://...
export SMTP_HOST=smtp.sendgrid.net
export SMTP_USERNAME=apikey
export SMTP_PASSWORD=your-sendgrid-key
java -jar target/planar-1.0.0.jar
```

### Backend — Docker

```bash
docker-compose up --build
```

### Frontend — Static Build

```bash
cd frontend
npm run build        # Produces dist/ folder

# Deploy to Vercel:      npx vercel --prod
# Deploy to Netlify:     npx netlify deploy --prod --dir=dist

# Set API origin if different:
VITE_API_URL=https://your-api.com npm run build
```

---

## Troubleshooting

### OTP email not arriving
1. Check the backend console — the OTP is logged as `[REGISTER OTP] Sending Verification OTP 377724 to user@example.com`
2. For production, set `SMTP_HOST`, `SMTP_USERNAME`, `SMTP_PASSWORD` in environment
3. For Gmail, use an [App Password](https://support.google.com/accounts/answer/185833) (requires 2FA enabled)

### Backend fails to start
- Run with dev profile: `mvn spring-boot:run -Dspring-boot.run.profiles=dev` — uses H2 in-memory DB
- Ensure `backend/.env` doesn't have uncommented production values

### Cards not syncing in real-time
- Check WebSocket connection indicator in the board header (green dot = live)
- Browser dev tools → Network → WS → ws://localhost:8080/ws

---

## Commit History

| Tag | Content |
|---|---|
| `stage-1` | JPA models, repositories, DTOs |
| `stage-2` | Design patterns (Command, State, Strategy, Observer, Proxy) |
| `stage-3` | Services, REST controllers, authentication |
| `stage-4` | DSA (topo sort, priority queue) + audit logging |
| `stage-5` | React frontend with real-time sync |
| `stage-6` | Docker, deployment, full integration |
