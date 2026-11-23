package com.mosek.classFlow.repository;

import com.mosek.classFlow.entity.AttendanceSheet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AttendanceSheetRepository extends JpaRepository<AttendanceSheet, UUID> {
    Optional<AttendanceSheet> findByCourseSessionId(UUID courseSessionId);
}
