package com.cb.service;

import com.cb.config.UserContext;
import com.cb.dto.CardDTO;
import com.cb.dto.CardMoveRequest;
import com.cb.model.*;
import com.cb.patterns.command.*;
import com.cb.patterns.observer.BoardEventBroadcaster;
import com.cb.patterns.state.CardStateMachine;
import com.cb.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final ColumnRepository columnRepository;
    private final BoardRepository boardRepository;
    private final AuditService auditService;
    private final UserContext userContext;
    private final BoardEventBroadcaster broadcaster;
    private final UndoRedoService undoRedoService;

    @Transactional
    public CardDTO createCard(String boardCode, Long columnId, String title, String sessionId) {
        columnRepository.findById(columnId)
            .orElseThrow(() -> new RuntimeException("Column not found: " + columnId));

        // Find max position in column
        List<Card> existingCards = cardRepository.findByColumnIdOrderByPosition(columnId);
        int newPosition = existingCards.isEmpty() ? 0 : existingCards.getLast().getPosition() + 1;

        CommandInvoker invoker = getInvoker(sessionId);
        CreateCardCommand command = new CreateCardCommand(title, columnId, newPosition, cardRepository, columnRepository);
        invoker.execute(command);

        Card card = cardRepository.findById(command.getCreatedCardId())
            .orElseThrow(() -> new RuntimeException("Card creation failed"));

        String email = userContext.getCurrentUserEmail();
        auditService.log("CREATE_CARD", card.getId(), card.getTitle(), email, sessionId, boardCode, null, title);

        Map<String, Object> event = Map.of(
            "type", "CARD_ADDED",
            "card", CardDTO.from(card),
            "userEmail", email
        );
        broadcaster.broadcast(boardCode, event);

        return CardDTO.from(card);
    }

    @Transactional
    public CardDTO moveCard(String boardCode, CardMoveRequest request, String userEmail) {
        CommandInvoker invoker = getInvoker(request.getSessionId());

        MoveCardCommand command = new MoveCardCommand(
            request.getCardId(), request.getToColumnId(),
            request.getNewPosition(), cardRepository, columnRepository
        );

        try {
            invoker.execute(command);
        } catch (ObjectOptimisticLockingFailureException e) {
            Card latest = cardRepository.findById(request.getCardId())
                .orElseThrow(() -> new RuntimeException("Card not found"));

            Map<String, Object> conflict = Map.of(
                "type", "CONFLICT",
                "cardId", request.getCardId(),
                "message", "Card was modified by another user",
                "latestState", CardDTO.from(latest)
            );
            broadcaster.broadcast(boardCode, conflict);
            throw new RuntimeException("Optimistic lock conflict on card " + request.getCardId());
        }

        Card movedCard = cardRepository.findById(request.getCardId())
            .orElseThrow(() -> new RuntimeException("Card not found"));

        auditService.log("MOVE_CARD", request.getCardId(), command.getCardTitle(), userEmail,
            request.getSessionId(), boardCode,
            command.getOldColumnName(), command.getNewColumnName());

        Map<String, Object> event = Map.of(
            "type", "CARD_MOVED",
            "cardId", request.getCardId(),
            "fromColumnId", movedCard.getColumn().getId(),
            "toColumnId", request.getToColumnId(),
            "newPosition", request.getNewPosition(),
            "userEmail", userEmail
        );
        broadcaster.broadcast(boardCode, event);

        return CardDTO.from(movedCard);
    }

    @Transactional
    public CardDTO editCard(String boardCode, Long cardId, String title, String description,
                            String assignee, String priority, java.time.LocalDateTime dueDate,
                            String userEmail, String sessionId) {
        CommandInvoker invoker = getInvoker(sessionId);
        EditCardCommand command = new EditCardCommand(cardId, title, description, assignee, priority, dueDate, cardRepository);

        try {
            invoker.execute(command);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RuntimeException("Optimistic lock conflict on card " + cardId);
        }

        Card edited = cardRepository.findById(cardId)
            .orElseThrow(() -> new RuntimeException("Card not found"));

        auditService.log("EDIT_CARD", cardId, edited.getTitle(), userEmail, sessionId, boardCode, null, title);

        Map<String, Object> event = Map.of(
            "type", "CARD_EDITED",
            "cardId", cardId,
            "changes", Map.of(
                "title", title != null ? title : "",
                "description", description != null ? description : "",
                "assignee", assignee != null ? assignee : "",
                "priority", priority != null ? priority : ""
            ),
            "userEmail", userEmail
        );
        broadcaster.broadcast(boardCode, event);

        return CardDTO.from(edited);
    }

    @Transactional
    public void deleteCard(String boardCode, Long cardId, String userEmail, String sessionId) {
        Card card = cardRepository.findById(cardId).orElse(null);
        String cardTitle = card != null ? card.getTitle() : "Card #" + cardId;
        CommandInvoker invoker = getInvoker(sessionId);
        DeleteCardCommand command = new DeleteCardCommand(cardId, cardRepository, columnRepository);

        try {
            invoker.execute(command);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RuntimeException("Optimistic lock conflict on card " + cardId);
        }

        auditService.log("DELETE_CARD", cardId, cardTitle, userEmail, sessionId, boardCode, null, cardTitle);

        Map<String, Object> event = Map.of(
            "type", "CARD_DELETED",
            "cardId", cardId,
            "userEmail", userEmail
        );
        broadcaster.broadcast(boardCode, event);
    }

    @Transactional
    public CardDTO changeState(String boardCode, Long cardId, CardState newState, String userEmail, String sessionId) {
        Card card = cardRepository.findById(cardId)
            .orElseThrow(() -> new RuntimeException("Card not found"));

        if (!CardStateMachine.isValidTransition(card.getState(), newState)) {
            throw new IllegalStateException(
                "Cannot transition from " + card.getState() + " to " + newState);
        }

        CardState oldState = card.getState();
        card.setState(newState);

        // Move card to matching column if available
        Map<CardState, String> stateColumnMap = Map.of(
            CardState.TODO, "To Do",
            CardState.IN_PROGRESS, "In Progress",
            CardState.REVIEW, "Review",
            CardState.DONE, "Done"
        );
        String targetColName = stateColumnMap.get(newState);
        if (targetColName != null) {
            Board board = boardRepository.findByCodeWithColumns(boardCode).orElse(null);
            if (board != null && board.getColumns() != null) {
                BoardColumn targetCol = board.getColumns().stream()
                    .filter(col -> col.getName().equalsIgnoreCase(targetColName))
                    .findFirst().orElse(null);
                if (targetCol != null && !targetCol.getId().equals(card.getColumn().getId())) {
                    card.setColumn(targetCol);
                    List<Card> existingCards = cardRepository.findByColumnIdOrderByPosition(targetCol.getId());
                    int newPos = existingCards.isEmpty() ? 0 : existingCards.getLast().getPosition() + 1;
                    card.setPosition(newPos);
                }
            }
        }

        try {
            card = cardRepository.save(card);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RuntimeException("Optimistic lock conflict on card " + cardId + " during state change");
        }

        auditService.log("CHANGE_STATE", cardId, card.getTitle(), userEmail, null, boardCode,
            oldState.name(), newState.name());

        Map<String, Object> event = Map.of(
            "type", "STATE_CHANGED",
            "cardId", cardId,
            "from", oldState.name(),
            "to", newState.name(),
            "userEmail", userEmail
        );
        broadcaster.broadcast(boardCode, event);

        return CardDTO.from(card);
    }

    public CommandInvoker getInvoker(String sessionId) {
        return undoRedoService.getInvoker(sessionId);
    }

    @Transactional(readOnly = true)
    public List<CardDTO> getBoardCards(String boardCode) {
        return cardRepository.findByBoardCodeWithDependencies(boardCode).stream()
            .map(CardDTO::from)
            .toList();
    }
}
