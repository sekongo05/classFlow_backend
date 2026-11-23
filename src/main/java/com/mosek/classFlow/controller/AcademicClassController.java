package com.mosek.classFlow.controller;

import com.mosek.classFlow.business.AcademicService;
import com.mosek.classFlow.business.RosterImportService;
import com.mosek.classFlow.dto.AcademicDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
public class AcademicClassController {

    private final AcademicService academicService;
    private final RosterImportService rosterImportService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AcademicDtos.ClassResponse> createClass(@Valid @RequestBody AcademicDtos.CreateClassRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(academicService.createClass(request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AcademicDtos.ClassResponse>> listClasses() {
        return ResponseEntity.ok(academicService.listClasses());
    }

    @GetMapping("/{classId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AcademicDtos.ClassResponse> getClass(@PathVariable UUID classId) {
        return ResponseEntity.ok(academicService.getClassById(classId));
    }

    @PutMapping("/{classId}/delegates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AcademicDtos.ClassResponse> assignDelegates(
            @PathVariable UUID classId,
            @RequestBody AcademicDtos.AssignDelegatesRequest request
    ) {
        return ResponseEntity.ok(academicService.assignDelegates(classId, request));
    }

    @PostMapping("/subjects")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AcademicDtos.SubjectResponse> createSubject(@Valid @RequestBody AcademicDtos.CreateSubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(academicService.createSubject(request));
    }

    @GetMapping("/{classId}/subjects")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AcademicDtos.SubjectResponse>> listSubjects(@PathVariable UUID classId) {
        return ResponseEntity.ok(academicService.listSubjects(classId));
    }

    @PostMapping(value = "/{classId}/roster/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'DELEGATE', 'DEPUTY_DELEGATE')")
    public ResponseEntity<AcademicDtos.RosterPreviewResponse> previewRoster(
            @PathVariable UUID classId,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(rosterImportService.previewRoster(classId, file));
    }

    @PostMapping("/{classId}/roster/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELEGATE', 'DEPUTY_DELEGATE')")
    public ResponseEntity<List<AcademicDtos.RosterEntryResponse>> confirmRoster(
            @PathVariable UUID classId,
            @Valid @RequestBody AcademicDtos.RosterConfirmRequest request
    ) {
        return ResponseEntity.ok(rosterImportService.confirmRoster(classId, request));
    }

    @GetMapping("/{classId}/roster")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELEGATE', 'DEPUTY_DELEGATE', 'TEACHER')")
    public ResponseEntity<List<AcademicDtos.RosterEntryResponse>> listRoster(@PathVariable UUID classId) {
        return ResponseEntity.ok(rosterImportService.listRoster(classId));
    }

    @GetMapping("/{classId}/roster/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELEGATE', 'DEPUTY_DELEGATE')")
    public ResponseEntity<List<AcademicDtos.RosterImportHistoryResponse>> rosterHistory(@PathVariable UUID classId) {
        return ResponseEntity.ok(rosterImportService.getImportHistory(classId));
    }
}
