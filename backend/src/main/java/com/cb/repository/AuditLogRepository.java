package com.cb.repository;

import com.cb.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByBoardCodeOrderByCreatedAtDesc(String boardCode);
    List<AuditLog> findByCardIdOrderByCreatedAtDesc(Long cardId);
}
