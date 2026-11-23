package com.mosek.classFlow.business;

import com.mosek.classFlow.dto.NotificationDtos;
import com.mosek.classFlow.entity.DeviceToken;
import com.mosek.classFlow.entity.Notification;
import com.mosek.classFlow.entity.User;
import com.mosek.classFlow.exception.ResourceNotFoundException;
import com.mosek.classFlow.repository.DeviceTokenRepository;
import com.mosek.classFlow.repository.NotificationRepository;
import com.mosek.classFlow.repository.UserRepository;
import com.mosek.classFlow.security.AuthenticatedUser;
import com.mosek.classFlow.security.SecurityUtils;
import com.mosek.classFlow.transformer.DomainTransformer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final DomainTransformer transformer;

    @Transactional
    public void notifyUser(UUID recipientId, String type, String title, String message) {
        if (recipientId == null) return;
        notificationRepository.save(Notification.builder()
                .recipientId(recipientId)
                .type(type)
                .title(title)
                .message(message)
                .read(false)
                .build());
    }

    @Transactional
    public void notifyClass(UUID academicClassId, String type, String title, String message) {
        List<User> members = userRepository.findByAcademicClassIdAndActiveTrue(academicClassId);
        List<Notification> batch = members.stream()
                .map(u -> Notification.builder()
                        .recipientId(u.getId())
                        .type(type)
                        .title(title)
                        .message(message)
                        .read(false)
                        .build())
                .toList();
        notificationRepository.saveAll(batch);
    }

    @Transactional(readOnly = true)
    public List<NotificationDtos.NotificationResponse> listMyNotifications() {
        AuthenticatedUser current = SecurityUtils.getCurrentUser();
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(current.id()).stream()
                .map(transformer::toNotificationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countMyUnread() {
        return notificationRepository.countByRecipientIdAndReadFalse(SecurityUtils.getCurrentUser().id());
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        AuthenticatedUser current = SecurityUtils.getCurrentUser();
        Notification n = notificationRepository.findById(notificationId)
                .filter(item -> item.getRecipientId().equals(current.id()))
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable."));
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void registerDeviceToken(NotificationDtos.RegisterDeviceTokenRequest req) {
        AuthenticatedUser current = SecurityUtils.getCurrentUser();
        deviceTokenRepository.findByToken(req.token().trim()).ifPresentOrElse(existing -> {
            existing.setUserId(current.id());
            existing.setPlatform(req.platform().trim().toUpperCase());
            deviceTokenRepository.save(existing);
        }, () -> deviceTokenRepository.save(DeviceToken.builder()
                .userId(current.id())
                .token(req.token().trim())
                .platform(req.platform().trim().toUpperCase())
                .build()));
    }

    @Transactional
    public void unregisterDeviceToken(String token) {
        deviceTokenRepository.deleteByTokenAndUserId(token.trim(), SecurityUtils.getCurrentUser().id());
    }
}
