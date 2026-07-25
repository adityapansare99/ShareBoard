package com.cb.patterns.observer;

import com.cb.model.Board;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class BoardEventBroadcaster {

    private final Map<String, Map<String, WebSocketSession>> boardSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Register a session for a board.
     */
    public void subscribe(String boardCode, String sessionId, WebSocketSession session) {
        boardSessions
            .computeIfAbsent(boardCode, k -> new ConcurrentHashMap<>())
            .put(sessionId, session);
    }

    /**
     * Remove a session from a board.
     */
    public void unsubscribe(String boardCode, String sessionId) {
        Map<String, WebSocketSession> sessions = boardSessions.get(boardCode);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                boardSessions.remove(boardCode);
            }
        }
    }

    /**
     * Remove a session from all boards (on disconnect).
     */
    public void removeSession(String sessionId) {
        boardSessions.values().forEach(sessions -> sessions.remove(sessionId));
        boardSessions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * Broadcast an event to all sessions watching a board.
     * Silently removes dead sessions.
     */
    public void broadcast(String boardCode, Object event) {
        Map<String, WebSocketSession> sessions = boardSessions.get(boardCode);
        if (sessions == null || sessions.isEmpty()) return;

        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (IOException e) {
            return;
        }

        TextMessage message = new TextMessage(payload);
        sessions.entrySet().removeIf(entry -> {
            if (!entry.getValue().isOpen()) return true;
            try {
                entry.getValue().sendMessage(message);
            } catch (IOException e) {
                return true; // Remove dead session
            }
            return false;
        });
    }

    /**
     * Get count of active sessions for a board.
     */
    public int getSessionCount(String boardCode) {
        Map<String, WebSocketSession> sessions = boardSessions.get(boardCode);
        return sessions == null ? 0 : sessions.size();
    }

    public Set<String> getActiveBoardCodes() {
        return boardSessions.keySet();
    }
}
