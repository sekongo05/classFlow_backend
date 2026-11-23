package com.mosek.classFlow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "academic_classes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicClass extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 120)
    private String track;

    @Column(nullable = false, length = 60)
    private String level;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Column(name = "expected_size", nullable = false)
    private int expectedSize;

    @Column(name = "delegate_id")
    private UUID delegateId;

    @Column(name = "deputy_delegate_id")
    private UUID deputyDelegateId;
}
