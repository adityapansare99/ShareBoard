package com.cb.patterns.strategy;

import com.cb.dto.*;
import com.cb.model.Board;
import com.cb.model.BoardColumn;
import com.cb.model.Card;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CalendarViewStrategy implements BoardViewStrategy {

    @Override
    public BoardSnapshot render(Board board) {
        // Flatten all cards from all columns
        List<Card> allCards = board.getColumns().stream()
            .flatMap(col -> col.getCards().stream())
            .collect(Collectors.toList());

        // Group by due date
        Map<String, List<CardDTO>> grouped = new LinkedHashMap<>();
        List<CardDTO> unscheduled = new ArrayList<>();

        for (Card card : allCards) {
            CardDTO dto = CardDTO.from(card);
            if (card.getDueDate() != null) {
                String key = card.getDueDate().toLocalDate().toString();
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(dto);
            } else {
                unscheduled.add(dto);
            }
        }

        // Sort each date group by position
        grouped.values().forEach(list -> list.sort(Comparator.comparingInt(CardDTO::getPosition)));

        List<ColumnDTO> dateColumns = new ArrayList<>();
        int pos = 0;
        for (Map.Entry<String, List<CardDTO>> entry : grouped.entrySet()) {
            dateColumns.add(new ColumnDTO(null, entry.getKey(), pos++, entry.getValue()));
        }
        if (!unscheduled.isEmpty()) {
            unscheduled.sort(Comparator.comparingInt(CardDTO::getPosition));
            dateColumns.add(new ColumnDTO(null, "Unscheduled", pos, unscheduled));
        }

        return new BoardSnapshot(board.getId(), board.getName(), board.getCode(), dateColumns);
    }

    @Override
    public String getViewName() {
        return "calendar";
    }
}
