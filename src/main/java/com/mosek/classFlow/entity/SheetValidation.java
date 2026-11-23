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
@Table(name = "sheet_validations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SheetValidation extends BaseEntity {

    @Column(name = "course_session_id", nullable = false)
    private UUID courseSessionId;

    @Column(name = "sheet_id", nullable = false)
    private UUID sheetId;

    @Column(name = "sheet_version", nullable = false)
    private int sheetVersion;

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ValidationDecision decision;

    @Column(length = 500)
    private String comment;
}
