package com.mosek.classFlow.repository;

import com.mosek.classFlow.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    Optional<DeviceToken> findByToken(String token);
    void deleteByTokenAndUserId(String token, UUID userId);
}
