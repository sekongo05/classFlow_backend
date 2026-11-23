package com.mosek.classFlow.repository;

import com.mosek.classFlow.entity.RosterImportHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RosterImportHistoryRepository extends JpaRepository<RosterImportHistory, UUID> {
    List<RosterImportHistory> findByAcademicClassIdOrderByCreatedAtDesc(UUID academicClassId);
}
