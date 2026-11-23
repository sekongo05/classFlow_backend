package com.mosek.classFlow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class SyncDtos {

    private SyncDtos() {}

    public record SyncOperationRequest(
            @NotNull(message = "L'identifiant d'opération (clientOperationId) est obligatoire.") UUID clientOperationId,
            @NotBlank(message = "Le type d'opération est obligatoire.") String operationType,
            UUID courseSessionId,
            String payload
    ) {}

    public record SyncPushRequest(
            @NotEmpty(message = "La liste d'opérations ne peut pas être vide.") @Valid List<SyncOperationRequest> operations
    ) {}

    public record SyncOperationResult(
            UUID clientOperationId,
            String status,
            String message
    ) {}

    public record SyncPushResponse(
            Instant syncedAt,
            List<SyncOperationResult> results
    ) {}

    public record SyncPullResponse(
            Instant serverTime,
            List<ScheduleDtos.SessionResponse> sessions,
            List<NotificationDtos.NotificationResponse> notifications,
            List<GradeDtos.EvaluationResponse> evaluations
    ) {}
}
