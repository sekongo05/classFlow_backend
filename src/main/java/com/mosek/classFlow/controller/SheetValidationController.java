package com.mosek.classFlow.controller;

import com.mosek.classFlow.business.SheetValidationService;
import com.mosek.classFlow.dto.AttendanceDtos.ValidateSheetRequest;
import com.mosek.classFlow.dto.AttendanceDtos.ValidationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/validations")
@RequiredArgsConstructor
public class SheetValidationController {

    private final SheetValidationService validationService;

    @PostMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ValidationResponse> decide(@PathVariable UUID sessionId, @Valid @RequestBody ValidateSheetRequest body) {
        return ResponseEntity.ok(validationService.validateOrDispute(sessionId, body));
    }

    @GetMapping("/sessions/{sessionId}/history")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'DELEGATE', 'DEPUTY_DELEGATE')")
    public ResponseEntity<List<ValidationResponse>> history(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(validationService.getSessionValidationHistory(sessionId));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<List<ValidationResponse>> mine() {
        return ResponseEntity.ok(validationService.getMyValidations());
    }
}
