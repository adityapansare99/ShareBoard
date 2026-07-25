package com.cb.websocket;

import com.cb.patterns.observer.BoardEventBroadcaster;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BoardWebSocketHandler extends TextWebSocketHandler {

    private final BoardEventBroadcaster broadcaster;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public BoardWebSocketHandler(BoardEventBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        JsonNode json = objectMapper.readTree(message.getPayload());
        String type = json.get("type").asText();

        switch (type) {
            case "SUBSCRIBE" -> {
                String boardCode = json.get("boardCode").asText();
                String sessionId = json.get("sessionId").asText();
                broadcaster.subscribe(boardCode, sessionId, session);
                sendMessage(session, Map.of("type", "SUBSCRIBED", "boardCode", boardCode, "sessionId", sessionId));
            }
            case "UNSUBSCRIBE" -> {
                String boardCode = json.get("boardCode").asText();
                String sessionId = json.get("sessionId").asText();
                broadcaster.unsubscribe(boardCode, sessionId);
            }
            default -> sendMessage(session, Map.of("type", "ERROR", "message", "Unknown message type: " + type));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        broadcaster.removeSession(session.getId());
    }

    private void sendMessage(WebSocketSession session, Object payload) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        }
    }
}
