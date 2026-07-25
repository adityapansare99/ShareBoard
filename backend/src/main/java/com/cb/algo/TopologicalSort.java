package com.cb.algo;

import com.cb.model.Card;
import java.util.*;

// Kahn's algorithm for topological sort of card dependencies. A → B means "A depends on B".
public class TopologicalSort {

    private TopologicalSort() {}

    /** Topologically sort a set of cards by their dependsOn edges. */
    public static List<Card> sort(Set<Card> cards) {
        Map<Long, Card> cardMap = new HashMap<>();
        Map<Long, List<Long>> adjacency = new HashMap<>();
        Map<Long, Integer> inDegree = new HashMap<>();

        for (Card card : cards) {
            cardMap.put(card.getId(), card);
            adjacency.put(card.getId(), new ArrayList<>());
            inDegree.put(card.getId(), 0);
        }

        for (Card card : cards) {
            if (card.getDependsOn() != null) {
                for (Card dep : card.getDependsOn()) {
                    if (cardMap.containsKey(dep.getId())) {
                        adjacency.get(dep.getId()).add(card.getId());
                        inDegree.merge(card.getId(), 1, Integer::sum);
                    }
                }
            }
        }

        Queue<Long> queue = new LinkedList<>();
        for (var entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<Card> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            Long nodeId = queue.poll();
            sorted.add(cardMap.get(nodeId));
            for (Long neighbor : adjacency.get(nodeId)) {
                inDegree.merge(neighbor, -1, Integer::sum);
                if (inDegree.get(neighbor) == 0) {
                    queue.add(neighbor);
                }
            }
        }

        // If not all cards were processed, there's a cycle
        if (sorted.size() != cards.size()) {
            throw new IllegalStateException("Cycle detected in card dependencies");
        }

        return sorted;
    }

    /** True if adding edge cardId → dependsOnId would create a cycle. */
    public static boolean wouldCreateCycle(Set<Card> allCards, Long cardId, Long dependsOnId) {
        Map<Long, Set<Long>> graph = new HashMap<>();
        for (Card card : allCards) {
            graph.put(card.getId(), new HashSet<>());
        }
        for (Card card : allCards) {
            if (card.getDependsOn() != null) {
                for (Card dep : card.getDependsOn()) {
                    Set<Long> edges = graph.get(card.getId());
                    if (edges != null) edges.add(dep.getId());
                }
            }
        }

        // Add the proposed edge
        graph.computeIfAbsent(cardId, k -> new HashSet<>()).add(dependsOnId);

        // Detect cycle with DFS
        Set<Long> visited = new HashSet<>();
        Set<Long> recursionStack = new HashSet<>();

        for (Long node : graph.keySet()) {
            if (hasCycle(node, graph, visited, recursionStack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCycle(Long node, Map<Long, Set<Long>> graph,
                                     Set<Long> visited, Set<Long> recStack) {
        if (recStack.contains(node)) return true;
        if (visited.contains(node)) return false;

        visited.add(node);
        recStack.add(node);

        Set<Long> neighbors = graph.get(node);
        if (neighbors != null) {
            for (Long neighbor : neighbors) {
                if (hasCycle(neighbor, graph, visited, recStack)) {
                    return true;
                }
            }
        }

        recStack.remove(node);
        return false;
    }

    /** Find cards whose dependsOn are all DONE after completedCardId finishes. */
    public static List<Card> findUnblockedCards(Set<Card> allCards, Long completedCardId) {
        List<Card> unblocked = new ArrayList<>();
        for (Card card : allCards) {
            if (card.getDependsOn() == null || card.getDependsOn().isEmpty()) continue;
            if (card.getState() == com.cb.model.CardState.DONE) continue;

            boolean allDepsDone = card.getDependsOn().stream()
                .allMatch(dep -> dep.getState() == com.cb.model.CardState.DONE
                    || dep.getId().equals(completedCardId));

            if (allDepsDone) {
                unblocked.add(card);
            }
        }
        return unblocked;
    }
}
