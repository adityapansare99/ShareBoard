package com.cb.patterns.state;

import com.cb.model.CardState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CardStateMachineTest {

    @Test
    void testValidTransitions() {
        assertTrue(CardStateMachine.isValidTransition(CardState.TODO, CardState.IN_PROGRESS));
        assertTrue(CardStateMachine.isValidTransition(CardState.IN_PROGRESS, CardState.REVIEW));
        assertTrue(CardStateMachine.isValidTransition(CardState.REVIEW, CardState.DONE));
        assertTrue(CardStateMachine.isValidTransition(CardState.REVIEW, CardState.IN_PROGRESS));
        assertTrue(CardStateMachine.isValidTransition(CardState.DONE, CardState.REVIEW));
    }

    @Test
    void testInvalidTransitions() {
        assertFalse(CardStateMachine.isValidTransition(CardState.TODO, CardState.DONE));
        assertFalse(CardStateMachine.isValidTransition(CardState.TODO, CardState.REVIEW));
        assertFalse(CardStateMachine.isValidTransition(CardState.DONE, CardState.TODO));
        assertFalse(CardStateMachine.isValidTransition(CardState.DONE, CardState.IN_PROGRESS));
    }

    @Test
    void testGetNextStates() {
        var next = CardStateMachine.getNextValidStates(CardState.IN_PROGRESS);
        assertTrue(next.contains(CardState.REVIEW));
        assertTrue(next.contains(CardState.TODO));
        assertEquals(2, next.size());
    }
}
