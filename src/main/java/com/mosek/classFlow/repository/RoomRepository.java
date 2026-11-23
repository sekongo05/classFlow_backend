package com.mosek.classFlow.repository;

import com.mosek.classFlow.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    Optional<Room> findByCodeIgnoreCase(String code);
    Optional<Room> findByQrCodeToken(String qrCodeToken);
    boolean existsByCodeIgnoreCase(String code);
}
