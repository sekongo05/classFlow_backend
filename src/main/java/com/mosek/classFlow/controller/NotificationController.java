package com.mosek.classFlow.controller;

import com.mosek.classFlow.business.NotificationService;
import com.mosek.classFlow.dto.NotificationDtos.NotificationResponse;
import com.mosek.classFlow.dto.NotificationDtos.RegisterDeviceTokenRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list() {
        return ResponseEntity.ok(notificationService.listMyNotifications());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.countMyUnread()));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/device-tokens")
    public ResponseEntity<Void> registerToken(@Valid @RequestBody RegisterDeviceTokenRequest body) {
        notificationService.registerDeviceToken(body);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/device-tokens")
    public ResponseEntity<Void> removeToken(@RequestParam String token) {
        notificationService.unregisterDeviceToken(token);
        return ResponseEntity.noContent().build();
    }
}
