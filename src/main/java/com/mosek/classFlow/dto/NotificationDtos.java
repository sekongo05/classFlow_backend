package com.mosek.classFlow.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class NotificationDtos {

    private NotificationDtos() {}

    public record NotificationResponse(
            UUID id,
            String type,
            String title,
            String message,
            boolean read,
            Instant createdAt
    ) {}

    public record RegisterDeviceTokenRequest(
            @NotBlank(message = "Le token appareil est obligatoire.") String token,
            @NotBlank(message = "La plateforme est obligatoire (ANDROID/IOS).") String platform
    ) {}
}
