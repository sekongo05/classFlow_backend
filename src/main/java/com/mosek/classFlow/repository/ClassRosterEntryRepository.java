package com.mosek.classFlow.repository;

import com.mosek.classFlow.entity.ClassRosterEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassRosterEntryRepository extends JpaRepository<ClassRosterEntry, UUID> {
    List<ClassRosterEntry> findByAcademicClassIdOrderByLastNameAscFirstNameAsc(UUID academicClassId);
    Optional<ClassRosterEntry> findByMatriculeIgnoreCase(String matricule);
    Optional<ClassRosterEntry> findByAcademicClassIdAndMatriculeIgnoreCase(UUID academicClassId, String matricule);
}
