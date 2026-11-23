package com.mosek.classFlow.controller;

import com.mosek.classFlow.business.AttendancePdfService;
import com.mosek.classFlow.business.AttendanceService;
import com.mosek.classFlow.business.StatisticsService;
import com.mosek.classFlow.dto.AttendanceDtos.CheckInRequest;
import com.mosek.classFlow.dto.AttendanceDtos.ClassStatsResponse;
import com.mosek.classFlow.dto.AttendanceDtos.CorrectAttendanceRequest;
import com.mosek.classFlow.dto.AttendanceDtos.CorrectionResponse;
import com.mosek.classFlow.dto.AttendanceDtos.RecordResponse;
import com.mosek.classFlow.dto.AttendanceDtos.SheetResponse;
import com.mosek.classFlow.dto.AttendanceDtos.StudentStatsResponse;
import com.mosek.classFlow.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AttendancePdfService pdfService;
    private final StatisticsService statisticsService;

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('STUDENT', 'DELEGATE', 'DEPUTY_DELEGATE')")
    public ResponseEntity<RecordResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.checkIn(request));
    }

    @PatchMapping("/records/{recordId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELEGATE', 'DEPUTY_DELEGATE')")
    public ResponseEntity<RecordResponse> correct(
            @PathVariable UUID recordId,
            @Valid @RequestBody CorrectAttendanceRequest request
    ) {
        return ResponseEntity.ok(attendanceService.correctAttendance(recordId, request));
    }

    @PostMapping("/sessions/{sessionId}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELEGATE', 'DEPUTY_DELEGATE')")
    public ResponseEntity<SheetResponse> closeSession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(attendanceService.closeSession(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/emergency-mode")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELEGATE', 'DEPUTY_DELEGATE')")
    public ResponseEntity<Void> toggleEmergency(@PathVariable UUID sessionId, @RequestParam boolean enabled) {
        attendanceService.toggleEmergencyMode(sessionId, enabled);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions/{sessionId}/sheet")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELEGATE', 'DEPUTY_DELEGATE', 'TEACHER')")
    public ResponseEntity<SheetResponse> getSheet(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(attendanceService.getSessionSheet(sessionId));
    }

    @GetMapping(value = "/sessions/{sessionId}/sheet/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'DELEGATE', 'DEPUTY_DELEGATE', 'TEACHER')")
    public ResponseEntity<byte[]> exportSheetPdf(@PathVariable UUID sessionId) {
        byte[] pdf = pdfService.exportSessionSheetPdf(sessionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=feuille-presence-" + sessionId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/sessions/{sessionId}/corrections")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELEGATE', 'DEPUTY_DELEGATE', 'TEACHER')")
    public ResponseEntity<List<CorrectionResponse>> getCorrections(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(attendanceService.getSessionCorrections(sessionId));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('STUDENT', 'DELEGATE', 'DEPUTY_DELEGATE')")
    public ResponseEntity<List<RecordResponse>> myHistory() {
        return ResponseEntity.ok(attendanceService.getMyHistory());
    }

    @GetMapping("/stats/me")
    @PreAuthorize("hasAnyRole('STUDENT', 'DELEGATE', 'DEPUTY_DELEGATE')")
    public ResponseEntity<StudentStatsResponse> myStats() {
        return ResponseEntity.ok(statisticsService.getStudentStats(SecurityUtils.getCurrentUser().id()));
    }

    @GetMapping("/stats/classes/{classId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELEGATE', 'DEPUTY_DELEGATE', 'TEACHER')")
    public ResponseEntity<ClassStatsResponse> classStats(@PathVariable UUID classId) {
        return ResponseEntity.ok(statisticsService.getClassStats(classId));
    }
}
