package com.mosek.classFlow.repository;

import com.mosek.classFlow.entity.CourseSession;
import com.mosek.classFlow.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface CourseSessionRepository extends JpaRepository<CourseSession, UUID> {
    List<CourseSession> findByAcademicClassIdOrderBySessionDateAscStartTimeAsc(UUID academicClassId);
    List<CourseSession> findByTeacherIdOrderBySessionDateAscStartTimeAsc(UUID teacherId);
    List<CourseSession> findByStatusAndSessionDate(SessionStatus status, LocalDate date);
    boolean existsByAcademicClassIdAndSubjectIdAndSessionDateAndStartTime(
            UUID academicClassId, UUID subjectId, LocalDate sessionDate, LocalTime startTime);
}
