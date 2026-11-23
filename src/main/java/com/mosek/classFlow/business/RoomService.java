package com.mosek.classFlow.business;

import com.mosek.classFlow.dto.RoomDtos;
import com.mosek.classFlow.entity.Room;
import com.mosek.classFlow.exception.DuplicateResourceException;
import com.mosek.classFlow.exception.NetworkVerificationException;
import com.mosek.classFlow.exception.ResourceNotFoundException;
import com.mosek.classFlow.repository.RoomRepository;
import com.mosek.classFlow.transformer.CoreTransformer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final CoreTransformer transformer;

    @Transactional
    public RoomDtos.RoomResponse createRoom(RoomDtos.CreateRoomRequest req) {
        String code = req.code().trim().toUpperCase();
        if (roomRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Une salle avec ce code existe déjà.");
        }
        String permanentQr = "QR-" + code + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Room saved = roomRepository.save(Room.builder()
                .code(code)
                .name(req.name().trim())
                .wifiSsid(req.wifiSsid().trim())
                .networkSecret(req.networkSecret().trim())
                .qrCodeToken(permanentQr)
                .operational(req.operational() == null || req.operational())
                .build());
        return transformer.toRoomResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RoomDtos.RoomResponse> listRooms() {
        return roomRepository.findAll().stream().map(transformer::toRoomResponse).toList();
    }

    @Transactional(readOnly = true)
    public RoomDtos.RoomResponse getRoom(UUID roomId) {
        return transformer.toRoomResponse(findRoomOrThrow(roomId));
    }

    public Room findRoomOrThrow(UUID roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable."));
    }

    public void verifyRoomNetwork(Room room, String qrCodeToken, String wifiSsid, String networkProof) {
        if (!room.isOperational()) {
            throw new NetworkVerificationException("Cette salle est actuellement marquée hors service.");
        }
        if (qrCodeToken == null || !room.getQrCodeToken().equals(qrCodeToken.trim())) {
            throw new NetworkVerificationException("Le QR Code scanné ne correspond pas à la salle de la séance.");
        }
        if (wifiSsid == null || !room.getWifiSsid().equalsIgnoreCase(wifiSsid.trim())) {
            throw new NetworkVerificationException("Vous devez être connecté au réseau Wi-Fi de la salle (" + room.getWifiSsid() + ").");
        }
        if (networkProof == null || !room.getNetworkSecret().equals(networkProof.trim())) {
            throw new NetworkVerificationException("Vérification du réseau local de la salle échouée.");
        }
    }
}
