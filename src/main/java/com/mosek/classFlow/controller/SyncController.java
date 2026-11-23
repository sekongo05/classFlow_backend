package com.mosek.classFlow.controller;

import com.mosek.classFlow.business.SyncService;
import com.mosek.classFlow.dto.SyncDtos.SyncPullResponse;
import com.mosek.classFlow.dto.SyncDtos.SyncPushRequest;
import com.mosek.classFlow.dto.SyncDtos.SyncPushResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sync")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;

    @PostMapping("/push")
    public ResponseEntity<SyncPushResponse> push(@Valid @RequestBody SyncPushRequest body) {
        return ResponseEntity.ok(syncService.push(body));
    }

    @GetMapping("/pull")
    public ResponseEntity<SyncPullResponse> pull() {
        return ResponseEntity.ok(syncService.pull());
    }
}
