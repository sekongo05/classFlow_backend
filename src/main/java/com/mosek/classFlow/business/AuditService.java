package com.mosek.classFlow.business;

import com.mosek.classFlow.entity.AuditLog;
import com.mosek.classFlow.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void record(String action, String entityType, String entityId, UUID actorId, String actorEmail, String actorRole, String details) {
        auditLogRepository.save(AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .actorId(actorId)
                .actorEmail(actorEmail)
                .actorRole(actorRole)
                .details(details)
                .createdAt(Instant.now())
                .build());
    }
}
