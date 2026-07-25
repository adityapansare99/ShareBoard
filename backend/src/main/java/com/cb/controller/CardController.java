package com.cb.controller;

import com.cb.config.UserContext;
import com.cb.dto.CardDTO;
import com.cb.dto.CardMoveRequest;
import com.cb.model.AuditLog;
import com.cb.model.CardState;
import com.cb.service.AuditService;
import com.cb.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/boards/{code}")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final UserContext userContext;
    private final AuditService auditService;

    @PostMapping("/cards")
    public ResponseEntity<CardDTO> createCard(
            @PathVariable String code,
            @RequestBody Map<String, Object> body) {
        Long columnId = Long.valueOf(body.get("columnId").toString());
        String title = (String) body.get("title");
        String sessionId = (String) body.getOrDefault("sessionId", "default");
        return ResponseEntity.ok(cardService.createCard(code, columnId, title, sessionId));
    }

    @PatchMapping("/cards/{cardId}")
    public ResponseEntity<CardDTO> editCard(
            @PathVariable String code,
            @PathVariable Long cardId,
            @RequestBody Map<String, Object> body) {
        String title = (String) body.getOrDefault("title", "");
        String description = (String) body.getOrDefault("description", "");
        String assignee = (String) body.getOrDefault("assignee", "");
        String priority = (String) body.getOrDefault("priority", null);
        String dueDateStr = (String) body.getOrDefault("dueDate", null);
        String sessionId = (String) body.getOrDefault("sessionId", "default");
        String email = userContext.getCurrentUserEmail();

        java.time.LocalDateTime dueDate = null;
        if (dueDateStr != null && !dueDateStr.trim().isEmpty()) {
            try {
                if (dueDateStr.contains("T")) {
                    dueDate = java.time.LocalDateTime.parse(dueDateStr);
                } else {
                    dueDate = java.time.LocalDate.parse(dueDateStr).atStartOfDay();
                }
            } catch (Exception e) {
                // Ignore parse error
            }
        }

        return ResponseEntity.ok(cardService.editCard(code, cardId, title, description, assignee, priority, dueDate, email, sessionId));
    }

    @DeleteMapping("/cards/{cardId}")
    public ResponseEntity<Void> deleteCard(
            @PathVariable String code,
            @PathVariable Long cardId,
            @RequestParam(defaultValue = "default") String sessionId) {
        String email = userContext.getCurrentUserEmail();
        cardService.deleteCard(code, cardId, email, sessionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cards/move")
    public ResponseEntity<CardDTO> moveCard(
            @PathVariable String code,
            @RequestBody CardMoveRequest request) {
        String email = userContext.getCurrentUserEmail();
        return ResponseEntity.ok(cardService.moveCard(code, request, email));
    }

    @PostMapping("/cards/{cardId}/state")
    public ResponseEntity<CardDTO> changeState(
            @PathVariable String code,
            @PathVariable Long cardId,
            @RequestBody Map<String, String> body) {
        CardState newState = CardState.valueOf(body.get("state"));
        String sessionId = body.getOrDefault("sessionId", "default");
        String email = userContext.getCurrentUserEmail();
        return ResponseEntity.ok(cardService.changeState(code, cardId, newState, email, sessionId));
    }

    @GetMapping("/activity")
    public ResponseEntity<List<AuditLog>> getBoardActivity(@PathVariable String code) {
        return ResponseEntity.ok(auditService.getBoardHistory(code));
    }
}
