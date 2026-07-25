package com.cb.patterns.command;

import com.cb.model.Card;
import com.cb.repository.CardRepository;
import com.cb.repository.ColumnRepository;
import lombok.Getter;

// Deletes a card; stores snapshot to restore on undo.
@Getter
public class DeleteCardCommand implements Command {
    private final Long cardId;
    private final CardRepository cardRepository;
    private final ColumnRepository columnRepository;

    // Snapshotted before delete
    private CardData snapshot;

    public DeleteCardCommand(Long cardId, CardRepository cardRepository, ColumnRepository columnRepository) {
        this.cardId = cardId;
        this.cardRepository = cardRepository;
        this.columnRepository = columnRepository;
    }

    @Override
    public void execute() {
        Card card = cardRepository.findById(cardId)
            .orElseThrow(() -> new RuntimeException("Card not found: " + cardId));
        this.snapshot = CardData.from(card);
        cardRepository.delete(card);
    }

    @Override
    public void undo() {
        if (snapshot == null) return;
        Card card = Card.builder()
            .title(snapshot.title)
            .description(snapshot.description)
            .state(snapshot.state)
            .position(snapshot.position)
            .assignee(snapshot.assignee)
            .dueDate(snapshot.dueDate)
            .priority(snapshot.priority)
            .column(columnRepository.findById(snapshot.columnId)
                .orElseThrow(() -> new RuntimeException("Column not found: " + snapshot.columnId)))
            .build();
        cardRepository.save(card);
    }

    @Override
    public String getDescription() {
        return "Deleted card " + cardId;
    }

    @Override
    public Long getTargetCardId() { return cardId; }

    @Getter
    private static class CardData {
        String title, description, assignee, priority;
        com.cb.model.CardState state;
        int position;
        Long columnId;
        java.time.LocalDateTime dueDate;

        static CardData from(Card card) {
            CardData d = new CardData();
            d.title = card.getTitle();
            d.description = card.getDescription();
            d.assignee = card.getAssignee();
            d.priority = card.getPriority();
            d.state = card.getState();
            d.position = card.getPosition();
            d.columnId = card.getColumn().getId();
            d.dueDate = card.getDueDate();
            return d;
        }
    }
}
