package com.mosek.classFlow.controller;

import com.mosek.classFlow.business.PersonalGradeService;
import com.mosek.classFlow.dto.GradeDtos.EvaluationRequest;
import com.mosek.classFlow.dto.GradeDtos.EvaluationResponse;
import com.mosek.classFlow.dto.GradeDtos.GradesSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/grades")
@PreAuthorize("hasAnyRole('STUDENT', 'DELEGATE', 'DEPUTY_DELEGATE')")
@RequiredArgsConstructor
public class GradeController {

    private final PersonalGradeService gradeService;

    @PostMapping("/evaluations")
    public ResponseEntity<EvaluationResponse> create(@Valid @RequestBody EvaluationRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gradeService.addEvaluation(body));
    }

    @PutMapping("/evaluations/{evaluationId}")
    public ResponseEntity<EvaluationResponse> update(@PathVariable UUID evaluationId, @Valid @RequestBody EvaluationRequest body) {
        return ResponseEntity.ok(gradeService.updateEvaluation(evaluationId, body));
    }

    @DeleteMapping("/evaluations/{evaluationId}")
    public ResponseEntity<Void> delete(@PathVariable UUID evaluationId) {
        gradeService.deleteEvaluation(evaluationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<GradesSummaryResponse> summary() {
        return ResponseEntity.ok(gradeService.getMySummary());
    }
}
