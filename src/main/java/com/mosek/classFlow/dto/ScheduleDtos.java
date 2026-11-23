package com.mosek.classFlow.dto;

import com.mosek.classFlow.entity.AbsenceDeclarationType;
import com.mosek.classFlow.entity.SessionStatus;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public final class ScheduleDtos {

    private ScheduleDtos() {}

    public record TimetableSlotRequest(
            @NotNull(message = "La matière est obligatoire.") UUID subjectId,
            UUID teacherId,
            @NotNull(message = "La salle est obligatoire.") UUID roomId,
            @NotNull(message = "Le jour est obligatoire.") DayOfWeek dayOfWeek,
            LocalDate specificDate,
            @NotNull(message = "L'heure de début est obligatoire.") LocalTime startTime,
            @NotNull(message = "L'heure de fin est obligatoire.") LocalTime endTime
    ) {}

    public record TimetableSlotResponse(
            UUID id,
            UUID academicClassId,
            UUID subjectId,
            UUID teacherId,
            UUID roomId,
            DayOfWeek dayOfWeek,
            LocalDate specificDate,
            LocalTime startTime,
            LocalTime endTime
    ) {}

    public record GenerateSessionsRequest(
            @NotNull(message = "La date de début est obligatoire.") LocalDate fromDate,
            @NotNull(message = "La date de fin est obligatoire.") LocalDate toDate
    ) {}

    public record CreateSessionRequest(
            @NotNull(message = "La matière est obligatoire.") UUID subjectId,
            UUID teacherId,
            @NotNull(message = "La salle est obligatoire.") UUID roomId,
            @NotNull(message = "La date est obligatoire.") LocalDate sessionDate,
            @NotNull(message = "L'heure de début est obligatoire.") LocalTime startTime,
            @NotNull(message = "L'heure de fin est obligatoire.") LocalTime endTime,
            String notes
    ) {}

    public record UpdateSessionRequest(
            UUID roomId,
            UUID teacherId,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            String notes
    ) {}

    public record SessionResponse(
            UUID id,
            UUID academicClassId,
            UUID subjectId,
            UUID teacherId,
            UUID roomId,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            SessionStatus status,
            boolean emergencyMode,
            String notes
    ) {}

    public record TeacherAbsenceRequest(
            @NotNull(message = "Le type de déclaration est obligatoire.") AbsenceDeclarationType declarationType,
            LocalDate newDate,
            LocalTime newStartTime,
            LocalTime newEndTime,
            UUID replacementTeacherId,
            String comment
    ) {}

    public record TeacherAbsenceResponse(
            UUID id,
            UUID courseSessionId,
            UUID declaredBy,
            AbsenceDeclarationType declarationType,
            String comment,
            boolean active,
            Instant createdAt
    ) {}
}
