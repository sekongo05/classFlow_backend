package com.mosek.classFlow.dto;

import com.mosek.classFlow.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank(message = "L'adresse e-mail est obligatoire.")
            @Email(message = "Le format de l'adresse e-mail est invalide.")
            String email,
            @NotBlank(message = "Le mot de passe est obligatoire.")
            String password
    ) {}

    public record RegisterStudentRequest(
            @NotBlank(message = "Le matricule est obligatoire.")
            String matricule,
            @NotBlank(message = "L'adresse e-mail est obligatoire.")
            @Email(message = "Le format de l'adresse e-mail est invalide.")
            String email,
            @NotBlank(message = "Le mot de passe est obligatoire.")
            @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères.")
            String password,
            @NotBlank(message = "Le prénom est obligatoire.")
            String firstName,
            @NotBlank(message = "Le nom est obligatoire.")
            String lastName
    ) {}

    public record CreateUserRequest(
            @NotBlank(message = "L'adresse e-mail est obligatoire.")
            @Email(message = "Le format de l'adresse e-mail est invalide.")
            String email,
            @NotBlank(message = "Le mot de passe est obligatoire.")
            @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères.")
            String password,
            @NotBlank(message = "Le prénom est obligatoire.")
            String firstName,
            @NotBlank(message = "Le nom est obligatoire.")
            String lastName,
            String matricule,
            @NotNull(message = "Le rôle est obligatoire.")
            Role role,
            UUID academicClassId
    ) {}

    public record RefreshTokenRequest(
            @NotBlank(message = "Le refresh token est obligatoire.")
            String refreshToken
    ) {}

    public record UserResponse(
            UUID id,
            String email,
            String firstName,
            String lastName,
            String matricule,
            Role role,
            UUID academicClassId,
            boolean active
    ) {}

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            long expiresInSeconds,
            UserResponse user
    ) {}
}
