package com.mosek.classFlow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "class_roster_entries",
        uniqueConstraints = @UniqueConstraint(name = "uk_roster_class_matricule", columnNames = {"academic_class_id", "matricule"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassRosterEntry extends BaseEntity {

    @Column(name = "academic_class_id", nullable = false)
    private UUID academicClassId;

    @Column(nullable = false, length = 60)
    private String matricule;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(length = 150)
    private String email;

    @Column(name = "linked_user_id")
    private UUID linkedUserId;
}
