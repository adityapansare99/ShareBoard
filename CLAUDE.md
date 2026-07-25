# Planar — Multi-user Kanban Board

A real-time collaborative Kanban board (like Trello) built with Java Spring Boot + React + Postgres.

## Stack
- **Backend:** Java 21 + Spring Boot 3 + Spring WebSocket + Postgres
- **Frontend:** React (JS) + Tailwind CSS
- **Auth:** Google OAuth2 (Spring Security)
- **Package:** com.cb

## Key Features
1. Multi-user real-time sync via WebSocket (Observer pattern)
2. Undo/redo with Command pattern (per-session stack)
3. Optimistic locking for concurrent drag-and-drop
4. Card lifecycle state machine (TODO → IN_PROGRESS → REVIEW → DONE)
5. Token bucket rate limiter (Proxy pattern)
6. Task dependency graph with topological sort
7. Deadline-aware priority queue
8. Google OAuth2 + audit logging

## Project Structure
```
docs/
├── plans/           # Implementation plan stages
├── architecture.md  # Full architecture doc
├── api.md          # API reference
backend/             # Spring Boot (com.cb)
frontend/            # React JS
docker-compose.yml   # Postgres + app
```

## Build Commands
```bash
cd backend && mvn clean install
cd frontend && npm install && npm start
```

## Implementation Stages
1. Models + Repos + DTOs
2. Design Patterns (Command → State → Strategy → Observer → Proxy)
3. Services + Controllers + OAuth
4. DSA (Topo Sort + Priority Queue + Audit)
5. Frontend (React JS)
6. Docker + Deployment
