package com.mosek.classFlow.repository;

import com.mosek.classFlow.entity.SyncOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SyncOperationLogRepository extends JpaRepository<SyncOperationLog, UUID> {
    Optional<SyncOperationLog> findByClientOperationId(UUID clientOperationId);
}
