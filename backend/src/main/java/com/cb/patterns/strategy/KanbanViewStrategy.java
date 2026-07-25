package com.cb.patterns.strategy;

import com.cb.dto.BoardSnapshot;
import com.cb.model.Board;
import com.cb.model.BoardColumn;
import com.cb.model.Card;
import org.springframework.stereotype.Component;


@Component
public class KanbanViewStrategy implements BoardViewStrategy {

    @Override
    public BoardSnapshot render(Board board) {
        return BoardSnapshot.from(board);
    }

    @Override
    public String getViewName() {
        return "kanban";
    }
}
