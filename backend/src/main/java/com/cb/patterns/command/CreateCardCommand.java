package com.cb.patterns.command;

import com.cb.model.Card;
import com.cb.model.BoardColumn;
import com.cb.model.CardState;
import com.cb.repository.CardRepository;
import com.cb.repository.ColumnRepository;
import lombok.Getter;

// Creates a card; deletes it on undo.
@Getter
public class CreateCardCommand implements Command {
    private final String title;
    private final Long columnId;
    private final int position;
    private final CardRepository cardRepository;
    private final ColumnRepository columnRepository;

    private Long createdCardId;

    public CreateCardCommand(String title, Long columnId, int position,
                             CardRepository cardRepository, ColumnRepository columnRepository) {
        this.title = title;
        this.columnId = columnId;
        this.position = position;
        this.cardRepository = cardRepository;
        this.columnRepository = columnRepository;
    }

    @Override
    public void execute() {
        BoardColumn column = columnRepository.findById(columnId)
            .orElseThrow(() -> new RuntimeException("Column not found: " + columnId));

        Card card = Card.builder()
            .title(title)
            .state(CardState.TODO)
            .column(column)
            .position(position)
            .build();

        card = cardRepository.save(card);
        this.createdCardId = card.getId();
    }

    @Override
    public void undo() {
        if (createdCardId != null && cardRepository.existsById(createdCardId)) {
            cardRepository.deleteById(createdCardId);
        }
    }

    @Override
    public String getDescription() {
        return "Created card: " + title;
    }

    @Override
    public Long getTargetCardId() { return createdCardId; }
}
