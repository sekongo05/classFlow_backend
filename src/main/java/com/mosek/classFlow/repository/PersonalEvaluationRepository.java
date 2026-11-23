package com.mosek.classFlow.repository;

import com.mosek.classFlow.entity.PersonalEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonalEvaluationRepository extends JpaRepository<PersonalEvaluation, UUID> {
    List<PersonalEvaluation> findByStudentIdOrderByEvaluationDateDesc(UUID studentId);
    Optional<PersonalEvaluation> findByIdAndStudentId(UUID id, UUID studentId);
}
