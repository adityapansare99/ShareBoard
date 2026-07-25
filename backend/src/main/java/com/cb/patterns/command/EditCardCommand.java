package com.cb.patterns.command;

import com.cb.model.Card;
import com.cb.repository.CardRepository;
import lombok.Getter;
import java.time.LocalDateTime;

// Edits a card's fields; snapshots old values for undo.
@Getter
public class EditCardCommand implements Command {
    private final Long cardId;
    private final String newTitle;
    private final String newDescription;
    private final String newAssignee;
    private final String newPriority;
    private final LocalDateTime newDueDate;
    private final CardRepository cardRepository;

    // Snapshotted before execute
    private String oldTitle;
    private String oldDescription;
    private String oldAssignee;
    private String oldPriority;
    private LocalDateTime oldDueDate;

    public EditCardCommand(Long cardId, String newTitle, String newDescription,
                           String newAssignee, String newPriority, LocalDateTime newDueDate,
                           CardRepository cardRepository) {
        this.cardId = cardId;
        this.newTitle = newTitle;
        this.newDescription = newDescription;
        this.newAssignee = newAssignee;
        this.newPriority = newPriority;
        this.newDueDate = newDueDate;
        this.cardRepository = cardRepository;
    }

    @Override
    public void execute() {
        Card card = cardRepository.findById(cardId)
            .orElseThrow(() -> new RuntimeException("Card not found: " + cardId));
        this.oldTitle = card.getTitle();
        this.oldDescription = card.getDescription();
        this.oldAssignee = card.getAssignee();
        this.oldPriority = card.getPriority();
        this.oldDueDate = card.getDueDate();

        card.setTitle(newTitle);
        card.setDescription(newDescription);
        card.setAssignee(newAssignee);
        card.setPriority(newPriority);
        card.setDueDate(newDueDate);
        cardRepository.save(card);
    }

    @Override
    public void undo() {
        Card card = cardRepository.findById(cardId)
            .orElseThrow(() -> new RuntimeException("Card not found: " + cardId));
        card.setTitle(oldTitle);
        card.setDescription(oldDescription);
        card.setAssignee(oldAssignee);
        card.setPriority(oldPriority);
        card.setDueDate(oldDueDate);
        cardRepository.save(card);
    }

    @Override
    public String getDescription() {
        return "Edited card " + cardId + ": " + oldTitle + " → " + newTitle;
    }

    @Override
    public Long getTargetCardId() { return cardId; }
}
