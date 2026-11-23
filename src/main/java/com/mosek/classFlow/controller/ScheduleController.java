package com.mosek.classFlow.controller;

import com.mosek.classFlow.business.TimetableAndSessionService;
import com.mosek.classFlow.dto.ScheduleDtos.CreateSessionRequest;
import com.mosek.classFlow.dto.ScheduleDtos.GenerateSessionsRequest;
import com.mosek.classFlow.dto.ScheduleDtos.SessionResponse;
import com.mosek.classFlow.dto.ScheduleDtos.TeacherAbsenceRequest;
import com.mosek.classFlow.dto.ScheduleDtos.TeacherAbsenceResponse;
import com.mosek.classFlow.dto.ScheduleDtos.TimetableSlotRequest;
import com.mosek.classFlow.dto.ScheduleDtos.TimetableSlotResponse;
import com.mosek.classFlow.dto.ScheduleDtos.UpdateSessionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ScheduleController {

    private final TimetableAndSessionService scheduleService;

    @PostMapping("/classes/{classId}/timetable/slots")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELEGATE', 'DEPUTY_DELEGATE')")
    public ResponseEntity<TimetableSlotResponse> addSlot(
            @PathVariable UUID classId,
            @Valid @RequestBody TimetableSlotRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.addSlot(classId, request));
    }

    @GetMapping("/classes/{classId}/timetable/slots")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TimetableSlotResponse>> listSlots(@PathVariable UUID classId) {
        return ResponseEntity.ok(scheduleService.listSlots(classId));
    }

    @DeleteMapping("/classes/{classId}/timetable/slots/{slotId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELEGATE', 'DEPUTY_DELEGATE')")
    public ResponseEntity<Void> deleteSlot(@PathVariable UUID classId, @PathVariable UUID slotId) {
        scheduleService.deleteSlot(classId, slotId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/classes/{classId}/sessions/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELEGATE', 'DEPUTY_DELEGATE')")
    public ResponseEntity<List<SessionResponse>> generateSessions(
            @PathVariable UUID classId,
            @Valid @RequestBody GenerateSessionsRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.generateSessions(classId, request));
    }

    @PostMapping("/classes/{classId}/sessions")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELEGATE', 'DEPUTY_DELEGATE')")
    public ResponseEntity<SessionResponse> createSession(
            @PathVariable UUID classId,
            @Valid @RequestBody CreateSessionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.createManualSession(classId, request));
    }

    @GetMapping("/classes/{classId}/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SessionResponse>> listClassSessions(@PathVariable UUID classId) {
        return ResponseEntity.ok(scheduleService.listClassSessions(classId));
    }

    @GetMapping("/sessions/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SessionResponse>> listMySessions() {
        return ResponseEntity.ok(scheduleService.listMySessions());
    }

    @PatchMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELEGATE', 'DEPUTY_DELEGATE')")
    public ResponseEntity<SessionResponse> updateSession(
            @PathVariable UUID sessionId,
            @RequestBody UpdateSessionRequest request
    ) {
        return ResponseEntity.ok(scheduleService.updateSession(sessionId, request));
    }

    @PostMapping("/sessions/{sessionId}/open")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELEGATE', 'DEPUTY_DELEGATE')")
    public ResponseEntity<SessionResponse> openSession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(scheduleService.openSession(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/teacher-absences")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELEGATE', 'DEPUTY_DELEGATE')")
    public ResponseEntity<TeacherAbsenceResponse> declareAbsence(
            @PathVariable UUID sessionId,
            @Valid @RequestBody TeacherAbsenceRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.declareTeacherAbsence(sessionId, request));
    }

    @PostMapping("/sessions/{sessionId}/teacher-absences/{declarationId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELEGATE', 'DEPUTY_DELEGATE')")
    public ResponseEntity<TeacherAbsenceResponse> cancelAbsence(
            @PathVariable UUID sessionId,
            @PathVariable UUID declarationId
    ) {
        return ResponseEntity.ok(scheduleService.cancelDeclaration(sessionId, declarationId));
    }
}
