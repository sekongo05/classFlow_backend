package com.mosek.classFlow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attendance_sheets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSheet extends BaseEntity {

    @Column(name = "course_session_id", nullable = false, unique = true)
    private UUID courseSessionId;

    @Column(name = "sheet_version", nullable = false)
    @Builder.Default
    private int sheetVersion = 1;

    @Column(name = "present_count", nullable = false)
    @Builder.Default
    private int presentCount = 0;

    @Column(name = "late_count", nullable = false)
    @Builder.Default
    private int lateCount = 0;

    @Column(name = "absent_count", nullable = false)
    @Builder.Default
    private int absentCount = 0;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "validated_by")
    private UUID validatedBy;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "contains_emergency_records", nullable = false)
    @Builder.Default
    private boolean containsEmergencyRecords = false;
}
