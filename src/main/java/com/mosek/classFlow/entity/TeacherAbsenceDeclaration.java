package com.mosek.classFlow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "teacher_absence_declarations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAbsenceDeclaration extends BaseEntity {

    @Column(name = "course_session_id", nullable = false)
    private UUID courseSessionId;

    @Column(name = "declared_by", nullable = false)
    private UUID declaredBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "declaration_type", nullable = false, length = 30)
    private AbsenceDeclarationType declarationType;

    @Column(length = 500)
    private String comment;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
