package com.mosek.classFlow.repository;

import com.mosek.classFlow.entity.SheetValidation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SheetValidationRepository extends JpaRepository<SheetValidation, UUID> {
    List<SheetValidation> findByCourseSessionIdOrderByCreatedAtDesc(UUID courseSessionId);
    List<SheetValidation> findByTeacherIdOrderByCreatedAtDesc(UUID teacherId);
}
