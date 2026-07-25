package com.cb.dto;

import com.cb.model.Card;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data @NoArgsConstructor @AllArgsConstructor
public class CardDTO {
    private Long id;
    private String title;
    private String description;
    private String state;
    private int position;
    private String assignee;
    private LocalDateTime dueDate;
    private String priority;
    private Long columnId;
    private Long version;
    private Set<Long> dependsOn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CardDTO from(Card card) {
        Set<Long> depIds = card.getDependsOn() != null
            ? card.getDependsOn().stream().map(Card::getId).collect(Collectors.toSet())
            : Set.of();
        return new CardDTO(
            card.getId(), card.getTitle(), card.getDescription(),
            card.getState().name(), card.getPosition(), card.getAssignee(),
            card.getDueDate(), card.getPriority(),
            card.getColumn() != null ? card.getColumn().getId() : null,
            card.getVersion(), depIds,
            card.getCreatedAt(), card.getUpdatedAt()
        );
    }
}
