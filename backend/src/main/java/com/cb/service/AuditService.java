package com.cb.service;

import com.cb.model.AuditLog;
import com.cb.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(String action, Long cardId, String cardTitle, String userEmail, String sessionId,
                    String boardCode, String oldValue, String newValue) {
        AuditLog log = AuditLog.builder()
            .action(action)
            .cardId(cardId)
            .cardTitle(cardTitle)
            .userEmail(userEmail)
            .sessionId(sessionId)
            .boardCode(boardCode)
            .oldValue(oldValue)
            .newValue(newValue)
            .createdAt(LocalDateTime.now())
            .build();
        auditLogRepository.save(log);
    }

    public void log(String action, Long cardId, String userEmail, String sessionId,
                    String boardCode, String oldValue, String newValue) {
        log(action, cardId, null, userEmail, sessionId, boardCode, oldValue, newValue);
    }

    public List<AuditLog> getBoardHistory(String boardCode) {
        return auditLogRepository.findByBoardCodeOrderByCreatedAtDesc(boardCode);
    }

    public List<AuditLog> getCardHistory(Long cardId) {
        return auditLogRepository.findByCardIdOrderByCreatedAtDesc(cardId);
    }
}
