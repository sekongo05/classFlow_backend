package com.mosek.classFlow.security;

import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email,
        String role,
        UUID academicClassId
) {
}
