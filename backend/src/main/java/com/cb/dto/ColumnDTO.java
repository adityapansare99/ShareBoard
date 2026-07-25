package com.cb.dto;

import com.cb.model.BoardColumn;
import com.cb.model.Card;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.stream.Collectors;

@Data @NoArgsConstructor @AllArgsConstructor
public class ColumnDTO {
    private Long id;
    private String name;
    private int position;
    private List<CardDTO> cards;

    static ColumnDTO from(BoardColumn col) {
        List<CardDTO> cardDTOs = col.getCards().stream()
            .map(CardDTO::from)
            .collect(Collectors.toList());
        return new ColumnDTO(col.getId(), col.getName(), col.getPosition(), cardDTOs);
    }
}
