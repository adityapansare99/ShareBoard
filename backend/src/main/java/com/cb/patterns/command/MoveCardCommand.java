package com.cb.patterns.command;

import com.cb.model.Card;
import com.cb.model.CardState;
import com.cb.model.BoardColumn;
import com.cb.repository.CardRepository;
import com.cb.repository.ColumnRepository;
import lombok.Getter;

import java.util.Map;

// Moves a card between columns; updates state to match column. Snapshots old state for undo.
@Getter
public class MoveCardCommand implements Command {

    private static final Map<String, CardState> COLUMN_STATE_MAP = Map.of(
        "To Do", CardState.TODO,
        "In Progress", CardState.IN_PROGRESS,
        "Review", CardState.REVIEW,
        "Done", CardState.DONE
    );

    private final Long cardId;
    private final Long toColumnId;
    private final int newPosition;
    private final CardRepository cardRepository;
    private final ColumnRepository columnRepository;

    // Captured before/during execute for undo & audit
    private int oldPosition;
    private Long oldColumnId;
    private CardState oldState;
    private String cardTitle;
    private String oldColumnName;
    private String newColumnName;

    public MoveCardCommand(Long cardId, Long toColumnId, int newPosition,
                           CardRepository cardRepository, ColumnRepository columnRepository) {
        this.cardId = cardId;
        this.toColumnId = toColumnId;
        this.newPosition = newPosition;
        this.cardRepository = cardRepository;
        this.columnRepository = columnRepository;
    }

    @Override
    public void execute() {
        Card card = cardRepository.findById(cardId)
            .orElseThrow(() -> new RuntimeException("Card not found: " + cardId));
        this.cardTitle = card.getTitle();
        this.oldPosition = card.getPosition();
        this.oldColumnId = card.getColumn().getId();
        this.oldColumnName = card.getColumn().getName();
        this.oldState = card.getState();

        BoardColumn targetColumn = columnRepository.findById(toColumnId)
            .orElseThrow(() -> new RuntimeException("Column not found: " + toColumnId));
        this.newColumnName = targetColumn.getName();

        card.setColumn(targetColumn);
        card.setPosition(newPosition);

        // Update card state to match the target column
        CardState targetState = COLUMN_STATE_MAP.get(targetColumn.getName());
        if (targetState != null) {
            card.setState(targetState);
        }

        cardRepository.save(card);
    }

    @Override
    public void undo() {
        Card card = cardRepository.findById(cardId)
            .orElseThrow(() -> new RuntimeException("Card not found: " + cardId));
        BoardColumn originalColumn = columnRepository.findById(oldColumnId)
            .orElseThrow(() -> new RuntimeException("Column not found: " + oldColumnId));

        card.setColumn(originalColumn);
        card.setPosition(oldPosition);
        card.setState(oldState);
        cardRepository.save(card);
    }

    @Override
    public String getDescription() {
        return "Moved card " + cardId;
    }

    @Override
    public Long getTargetCardId() { return cardId; }
}
