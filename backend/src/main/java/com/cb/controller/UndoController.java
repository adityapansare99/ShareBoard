package com.cb.controller;

import com.cb.config.UserContext;
import com.cb.patterns.observer.BoardEventBroadcaster;
import com.cb.service.AuditService;
import com.cb.service.UndoRedoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/boards/{code}")
@RequiredArgsConstructor
public class UndoController {

    private final UndoRedoService undoRedoService;
    private final AuditService auditService;
    private final UserContext userContext;
    private final BoardEventBroadcaster broadcaster;

    @PostMapping("/undo")
    public ResponseEntity<Map<String, Object>> undo(@PathVariable String code, @RequestParam String sessionId) {
        boolean success = undoRedoService.undo(sessionId);
        if (success) {
            String email = userContext.getCurrentUserEmail();
            auditService.log("EDIT_CARD", null, "Undo Action", email, sessionId, code, "Current State", "Reverted State");
            broadcaster.broadcast(code, Map.of("type", "CARD_MOVED", "userEmail", email));
        }
        return ResponseEntity.ok(Map.of(
            "success", success,
            "history", undoRedoService.getHistory(sessionId)
        ));
    }

    @PostMapping("/redo")
    public ResponseEntity<Map<String, Object>> redo(@PathVariable String code, @RequestParam String sessionId) {
        boolean success = undoRedoService.redo(sessionId);
        if (success) {
            String email = userContext.getCurrentUserEmail();
            auditService.log("EDIT_CARD", null, "Redo Action", email, sessionId, code, "Previous State", "Re-applied State");
            broadcaster.broadcast(code, Map.of("type", "CARD_MOVED", "userEmail", email));
        }
        return ResponseEntity.ok(Map.of(
            "success", success,
            "history", undoRedoService.getHistory(sessionId)
        ));
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> history(@PathVariable String code, @RequestParam String sessionId) {
        return ResponseEntity.ok(undoRedoService.getHistory(sessionId));
    }
}
