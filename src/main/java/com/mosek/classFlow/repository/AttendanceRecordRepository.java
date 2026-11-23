package com.mosek.classFlow.repository;

import com.mosek.classFlow.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {
    List<AttendanceRecord> findByCourseSessionIdOrderByStudentNameAsc(UUID courseSessionId);
    List<AttendanceRecord> findByCourseSessionIdIn(Collection<UUID> courseSessionIds);
    List<AttendanceRecord> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
    Optional<AttendanceRecord> findByCourseSessionIdAndStudentId(UUID courseSessionId, UUID studentId);
    boolean existsByCourseSessionIdAndStudentId(UUID courseSessionId, UUID studentId);
}
