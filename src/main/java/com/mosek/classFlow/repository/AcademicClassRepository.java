package com.mosek.classFlow.repository;

import com.mosek.classFlow.entity.AcademicClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AcademicClassRepository extends JpaRepository<AcademicClass, UUID> {
    boolean existsByNameIgnoreCaseAndAcademicYearIgnoreCase(String name, String academicYear);
}
