package com.mosek.classFlow.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public final class RoomDtos {

    private RoomDtos() {}

    public record CreateRoomRequest(
            @NotBlank(message = "Le code salle est obligatoire.") String code,
            @NotBlank(message = "Le nom de la salle est obligatoire.") String name,
            @NotBlank(message = "Le réseau Wi-Fi associé est obligatoire.") String wifiSsid,
            @NotBlank(message = "La clé réseau de la salle est obligatoire.") String networkSecret,
            Boolean operational
    ) {}

    public record RoomResponse(
            UUID id,
            String code,
            String name,
            String wifiSsid,
            String qrCodeToken,
            boolean operational
    ) {}
}
