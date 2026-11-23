package com.mosek.classFlow.dto;

import com.mosek.classFlow.entity.AttendanceMode;
import com.mosek.classFlow.entity.AttendanceStatus;
import com.mosek.classFlow.entity.SessionStatus;
import com.mosek.classFlow.entity.ValidationDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AttendanceDtos {

    private AttendanceDtos() {}

    public record CheckInRequest(
            @NotNull(message = "La séance est obligatoire.") UUID courseSessionId,
            @NotBlank(message = "Le QR Code de la salle est obligatoire.") String qrCodeToken,
            String wifiSsid,
            String networkProof
    ) {}

    public record CorrectAttendanceRequest(
            @NotNull(message = "Le nouveau statut est obligatoire.") AttendanceStatus newStatus,
            @NotBlank(message = "Le motif de la correction est obligatoire.") String reason
    ) {}

    public record RecordResponse(
            UUID id,
            UUID courseSessionId,
            UUID studentId,
            String matricule,
            String studentName,
            AttendanceStatus status,
            AttendanceMode mode,
            Instant checkedInAt,
            boolean regularized
    ) {}

    public record CorrectionResponse(
            UUID id,
            UUID attendanceRecordId,
            AttendanceStatus previousStatus,
            AttendanceStatus newStatus,
            UUID modifiedBy,
            String reason,
            Instant modifiedAt
    ) {}

    public record SheetResponse(
            UUID id,
            UUID courseSessionId,
            SessionStatus sessionStatus,
            int sheetVersion,
            int presentCount,
            int lateCount,
            int absentCount,
            boolean containsEmergencyRecords,
            Instant closedAt,
            UUID validatedBy,
            Instant validatedAt,
            List<RecordResponse> records
    ) {}

    public record LiveAttendanceEvent(
            String eventType,
            UUID courseSessionId,
            int presentCount,
            int lateCount,
            int absentCount,
            RecordResponse record
    ) {}

    public record ValidateSheetRequest(
            @NotNull(message = "La décision est obligatoire.") ValidationDecision decision,
            String comment
    ) {}

    public record ValidationResponse(
            UUID id,
            UUID courseSessionId,
            UUID sheetId,
            int sheetVersion,
            UUID teacherId,
            ValidationDecision decision,
            String comment,
            Instant createdAt
    ) {}

    public record StudentStatsResponse(
            UUID studentId,
            long totalSessions,
            long presentCount,
            long lateCount,
            long absentCount,
            double attendanceRate
    ) {}

    public record ClassStatsResponse(
            UUID classId,
            int rosterSize,
            long totalSessions,
            double averagePresent,
            long totalLate,
            long totalAbsent,
            double globalAttendanceRate
    ) {}
}
