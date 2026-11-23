package com.mosek.classFlow.repository;

import com.mosek.classFlow.entity.AttendanceCorrection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttendanceCorrectionRepository extends JpaRepository<AttendanceCorrection, UUID> {
    List<AttendanceCorrection> findByCourseSessionIdOrderByCreatedAtDesc(UUID courseSessionId);
}
