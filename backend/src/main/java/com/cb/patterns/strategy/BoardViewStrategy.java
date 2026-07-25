package com.cb.patterns.strategy;

import com.cb.dto.BoardSnapshot;
import com.cb.model.Board;


public interface BoardViewStrategy {
    BoardSnapshot render(Board board);
    String getViewName();
}
