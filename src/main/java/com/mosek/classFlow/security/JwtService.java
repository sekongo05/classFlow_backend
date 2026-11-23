package com.mosek.classFlow.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtService(
            @Value("${classflow.jwt.secret}") String secret,
            @Value("${classflow.jwt.access-token-expiration-ms}") long accessExpirationMs,
            @Value("${classflow.jwt.refresh-token-expiration-ms}") long refreshExpirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(user.id().toString())
                .claim("email", user.email())
                .claim("role", user.role())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessExpirationMs)));
        if (user.academicClassId() != null) {
            builder.claim("classId", user.academicClassId().toString());
        }
        return builder.signWith(signingKey).compact();
    }

    public AuthenticatedUser parseAndValidate(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String classIdStr = claims.get("classId", String.class);
        return new AuthenticatedUser(
                UUID.fromString(claims.getSubject()),
                claims.get("email", String.class),
                claims.get("role", String.class),
                classIdStr != null ? UUID.fromString(classIdStr) : null
        );
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationMs / 1000L;
    }

    public Instant computeRefreshExpiry() {
        return Instant.now().plusMillis(refreshExpirationMs);
    }
}
