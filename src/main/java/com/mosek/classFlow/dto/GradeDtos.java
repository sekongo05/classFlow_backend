package com.mosek.classFlow.dto;

import com.mosek.classFlow.entity.EvaluationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GradeDtos {

    private GradeDtos() {}

    public record EvaluationRequest(
            @NotBlank(message = "La matière est obligatoire.") String subjectName,
            @NotNull(message = "Le type d'évaluation est obligatoire.") EvaluationType evaluationType,
            @NotBlank(message = "Le nom de l'évaluation est obligatoire.") String title,
            @PositiveOrZero(message = "La note doit être positive ou nulle.") double score,
            @Positive(message = "Le barème doit être positif.") Double maxScore,
            @Positive(message = "Le coefficient doit être positif.") Double coefficient,
            String semester,
            String academicYear,
            @NotNull(message = "La date de l'évaluation est obligatoire.") LocalDate evaluationDate
    ) {}

    public record EvaluationResponse(
            UUID id,
            String subjectName,
            EvaluationType evaluationType,
            String title,
            double score,
            double maxScore,
            double coefficient,
            String semester,
            String academicYear,
            LocalDate evaluationDate
    ) {}

    public record GradesSummaryResponse(
            double overallAverage,
            int totalEvaluations,
            String bestSubject,
            double bestSubjectAverage,
            Map<String, Double> averageBySubject,
            Map<String, Double> averageBySemester,
            Map<String, Double> averageByYear,
            List<EvaluationResponse> evaluations
    ) {}
}
