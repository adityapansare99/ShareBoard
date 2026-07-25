package com.cb.patterns.strategy;

import com.cb.dto.*;
import com.cb.model.Board;
import com.cb.model.Card;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ListViewStrategy implements BoardViewStrategy {

    private static final List<String> PRIORITY_ORDER = List.of("HIGH", "MEDIUM", "LOW");

    @Override
    public BoardSnapshot render(Board board) {
        List<Card> allCards = board.getColumns().stream()
            .flatMap(col -> col.getCards().stream())
            .sorted(Comparator.comparingInt(this::priorityScore)
                .thenComparingInt(Card::getPosition))
            .collect(Collectors.toList());

        List<CardDTO> cardDTOs = allCards.stream()
            .map(CardDTO::from)
            .collect(Collectors.toList());

        ColumnDTO allCardsColumn = new ColumnDTO(null, "All Cards", 0, cardDTOs);
        return new BoardSnapshot(board.getId(), board.getName(), board.getCode(), List.of(allCardsColumn));
    }

    private int priorityScore(Card card) {
        if (card.getPriority() == null) return 999;
        int idx = PRIORITY_ORDER.indexOf(card.getPriority().toUpperCase());
        return idx < 0 ? 999 : idx;
    }

    @Override
    public String getViewName() {
        return "list";
    }
}
