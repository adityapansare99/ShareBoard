package com.cb.algo;

import com.cb.model.Card;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

@Component
public class DeadlineQueue { // Priority queue of cards by due date; polls for overdue entries

    private final PriorityBlockingQueue<DeadlineEntry> queue = new PriorityBlockingQueue<>(
        11, Comparator.comparing(DeadlineEntry::dueDate)
    );

    private final Set<Long> notified = new HashSet<>();

    public void addCard(Card card) {
        if (card.getDueDate() != null && card.getState() != com.cb.model.CardState.DONE) {
            queue.offer(new DeadlineEntry(card.getId(), card.getDueDate(), card.getTitle()));
        }
    }

    public void removeCard(Long cardId) {
        queue.removeIf(e -> e.cardId().equals(cardId));
        notified.remove(cardId);
    }

    public void updateCard(Card card) {
        removeCard(card.getId());
        addCard(card);
    }

    /** Returns newly overdue cards since last check. */
    public List<DeadlineEntry> getOverdueCards() {
        List<DeadlineEntry> overdue = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        while (!queue.isEmpty() && queue.peek().dueDate().isBefore(now)) {
            DeadlineEntry entry = queue.poll();
            if (!notified.contains(entry.cardId())) {
                overdue.add(entry);
                notified.add(entry.cardId());
            }
        }

        return overdue;
    }

    public record DeadlineEntry(Long cardId, LocalDateTime dueDate, String title) {}
}
