package com.cb.patterns.state;

import com.cb.model.CardState;
import java.util.*;

// State machine: TODO → IN_PROGRESS → REVIEW → DONE with backwards edges. Prevents skips.
public class CardStateMachine {

    private static final Map<CardState, Set<CardState>> VALID_TRANSITIONS = new EnumMap<>(CardState.class);

    static {
        VALID_TRANSITIONS.put(CardState.TODO, EnumSet.of(CardState.IN_PROGRESS));
        VALID_TRANSITIONS.put(CardState.IN_PROGRESS, EnumSet.of(CardState.REVIEW, CardState.TODO));
        VALID_TRANSITIONS.put(CardState.REVIEW, EnumSet.of(CardState.DONE, CardState.IN_PROGRESS));
        VALID_TRANSITIONS.put(CardState.DONE, EnumSet.of(CardState.REVIEW));
    }

    private CardStateMachine() {}

    public static boolean isValidTransition(CardState from, CardState to) {
        Set<CardState> allowed = VALID_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    public static Set<CardState> getNextValidStates(CardState current) {
        return VALID_TRANSITIONS.getOrDefault(current, Collections.emptySet());
    }
}
