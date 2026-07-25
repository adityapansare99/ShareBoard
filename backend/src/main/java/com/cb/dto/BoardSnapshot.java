package com.cb.dto;

import com.cb.model.Board;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.stream.Collectors;

@Data @NoArgsConstructor @AllArgsConstructor
public class BoardSnapshot {
    private Long id;
    private String name;
    private String code;
    private List<ColumnDTO> columns;

    public static BoardSnapshot from(Board board) {
        List<ColumnDTO> cols = board.getColumns().stream()
            .map(ColumnDTO::from)
            .collect(Collectors.toList());
        return new BoardSnapshot(board.getId(), board.getName(), board.getCode(), cols);
    }
}
