package com.mosek.classFlow.repository;

import com.mosek.classFlow.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    List<Subject> findByAcademicClassIdOrderByNameAsc(UUID academicClassId);
    Optional<Subject> findByAcademicClassIdAndCodeIgnoreCase(UUID academicClassId, String code);
    Optional<Subject> findByAcademicClassIdAndNameIgnoreCase(UUID academicClassId, String name);
}
