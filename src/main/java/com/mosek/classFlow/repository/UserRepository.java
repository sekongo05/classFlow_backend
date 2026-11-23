package com.mosek.classFlow.repository;

import com.mosek.classFlow.entity.Role;
import com.mosek.classFlow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByMatriculeIgnoreCase(String matricule);
    List<User> findByAcademicClassIdAndActiveTrue(UUID academicClassId);
    List<User> findByRoleAndActiveTrue(Role role);
}
