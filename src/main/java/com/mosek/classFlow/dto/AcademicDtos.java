package com.mosek.classFlow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AcademicDtos {

    private AcademicDtos() {}

    public record CreateClassRequest(
            @NotBlank(message = "Le nom de la classe est obligatoire.") String name,
            @NotBlank(message = "La filière est obligatoire.") String track,
            @NotBlank(message = "Le niveau d'étude est obligatoire.") String level,
            @NotBlank(message = "L'année académique est obligatoire.") String academicYear,
            @Min(value = 1, message = "L'effectif doit être supérieur à 0.") int expectedSize,
            UUID delegateId,
            UUID deputyDelegateId
    ) {}

    public record AssignDelegatesRequest(UUID delegateId, UUID deputyDelegateId) {}

    public record ClassResponse(
            UUID id,
            String name,
            String track,
            String level,
            String academicYear,
            int expectedSize,
            UUID delegateId,
            UUID deputyDelegateId
    ) {}

    public record CreateSubjectRequest(
            @NotNull(message = "La classe est obligatoire.") UUID academicClassId,
            @NotBlank(message = "Le code matière est obligatoire.") String code,
            @NotBlank(message = "Le nom de la matière est obligatoire.") String name,
            @Positive(message = "Le coefficient doit être positif.") Double defaultCoefficient,
            UUID teacherId
    ) {}

    public record SubjectResponse(
            UUID id,
            UUID academicClassId,
            String code,
            String name,
            double defaultCoefficient,
            UUID teacherId
    ) {}

    public record RosterRow(
            @NotBlank(message = "Le matricule est obligatoire.") String matricule,
            @NotBlank(message = "Le nom est obligatoire.") String lastName,
            @NotBlank(message = "Le prénom est obligatoire.") String firstName,
            String email
    ) {}

    public record RosterPreviewResponse(
            String fileName,
            int totalRows,
            int validCount,
            List<RosterRow> validRows,
            List<String> errors
    ) {}

    public record RosterConfirmRequest(
            @NotBlank(message = "Le nom du fichier est obligatoire.") String fileName,
            @NotEmpty(message = "La liste d'étudiants ne peut pas être vide.") @Valid List<RosterRow> rows
    ) {}

    public record RosterEntryResponse(
            UUID id,
            UUID academicClassId,
            String matricule,
            String lastName,
            String firstName,
            String email,
            UUID linkedUserId
    ) {}

    public record RosterImportHistoryResponse(
            UUID id,
            UUID academicClassId,
            UUID importedBy,
            String fileName,
            int importedCount,
            int updatedCount,
            Instant importedAt
    ) {}
}
