package com.mosek.classFlow.repository;

import com.mosek.classFlow.entity.TeacherAbsenceDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TeacherAbsenceDeclarationRepository extends JpaRepository<TeacherAbsenceDeclaration, UUID> {
    List<TeacherAbsenceDeclaration> findByCourseSessionIdOrderByCreatedAtDesc(UUID courseSessionId);
}
