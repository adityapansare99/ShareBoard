package com.cb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class CardMoveRequest {
    private Long cardId;
    private Long toColumnId;
    private int newPosition;
    private Long expectedVersion;
    private String sessionId;
}
