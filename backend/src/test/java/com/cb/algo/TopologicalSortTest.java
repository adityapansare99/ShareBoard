package com.cb.algo;

import com.cb.model.Card;
import com.cb.model.CardState;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TopologicalSortTest {

    @Test
    void testLinearChain() {
        Card a = Card.builder().id(1L).state(CardState.TODO).build();
        Card b = Card.builder().id(2L).state(CardState.TODO).build();
        Card c = Card.builder().id(3L).state(CardState.TODO).build();

        // a depends on b, b depends on c (c → b → a)
        b.setDependsOn(Set.of(c));
        a.setDependsOn(Set.of(b));

        List<Card> sorted = TopologicalSort.sort(Set.of(a, b, c));
        assertEquals(List.of(c, b, a), sorted);
    }

    @Test
    void testDiamondDependency() {
        Card a = Card.builder().id(1L).build();
        Card b = Card.builder().id(2L).build();
        Card c = Card.builder().id(3L).build();
        Card d = Card.builder().id(4L).build();

        // a depends on b and c; b and c depend on d (d → b,c → a)
        a.setDependsOn(Set.of(b, c));
        b.setDependsOn(Set.of(d));
        c.setDependsOn(Set.of(d));

        List<Card> sorted = TopologicalSort.sort(Set.of(a, b, c, d));
        // d must come first, b and c can be in any order after d, a last
        assertEquals(4L, sorted.get(0).getId()); // d first
        assertEquals(1L, sorted.get(3).getId()); // a last
    }

    @Test
    void testCycleDetection() {
        Card a = Card.builder().id(1L).build();
        Card b = Card.builder().id(2L).build();

        // Create cycle: a depends on b, b depends on a
        a.setDependsOn(Set.of(b));
        b.setDependsOn(Set.of(a));

        assertThrows(IllegalStateException.class, () ->
            TopologicalSort.sort(Set.of(a, b))
        );
    }

    @Test
    void testWouldCreateCycle() {
        Card a = Card.builder().id(1L).build();
        Card b = Card.builder().id(2L).build();
        Card c = Card.builder().id(3L).build();

        a.setDependsOn(Set.of(b));
        b.setDependsOn(Set.of(c));

        Set<Card> all = Set.of(a, b, c);

        // Adding c depends on a would create cycle
        assertTrue(TopologicalSort.wouldCreateCycle(all, 3L, 1L));

        // Adding c depends on b is fine (already there)
        assertFalse(TopologicalSort.wouldCreateCycle(all, 1L, 3L));
    }
}