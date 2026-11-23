package com.mosek.classFlow.business;

import com.mosek.classFlow.dto.GradeDtos;
import com.mosek.classFlow.entity.PersonalEvaluation;
import com.mosek.classFlow.exception.BusinessRuleException;
import com.mosek.classFlow.exception.ResourceNotFoundException;
import com.mosek.classFlow.repository.PersonalEvaluationRepository;
import com.mosek.classFlow.security.SecurityUtils;
import com.mosek.classFlow.transformer.DomainTransformer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonalGradeService {

    private final PersonalEvaluationRepository evaluationRepository;
    private final DomainTransformer transformer;

    @Transactional
    public GradeDtos.EvaluationResponse addEvaluation(GradeDtos.EvaluationRequest req) {
        UUID studentId = SecurityUtils.getCurrentUser().id();
        double maxScore = req.maxScore() != null ? req.maxScore() : 20.0;
        if (req.score() > maxScore) {
            throw new BusinessRuleException("INVALID_SCORE", "La note obtenue ne peut pas dépasser le barème.");
        }
        PersonalEvaluation saved = evaluationRepository.save(PersonalEvaluation.builder()
                .studentId(studentId)
                .subjectName(req.subjectName().trim())
                .evaluationType(req.evaluationType())
                .title(req.title().trim())
                .score(req.score())
                .maxScore(maxScore)
                .coefficient(req.coefficient() != null ? req.coefficient() : 1.0)
                .semester(req.semester())
                .academicYear(req.academicYear())
                .evaluationDate(req.evaluationDate())
                .build());
        return transformer.toEvaluationResponse(saved);
    }

    @Transactional
    public GradeDtos.EvaluationResponse updateEvaluation(UUID evaluationId, GradeDtos.EvaluationRequest req) {
        UUID studentId = SecurityUtils.getCurrentUser().id();
        PersonalEvaluation eval = evaluationRepository.findByIdAndStudentId(evaluationId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Évaluation personnelle introuvable."));
        double maxScore = req.maxScore() != null ? req.maxScore() : 20.0;
        if (req.score() > maxScore) {
            throw new BusinessRuleException("INVALID_SCORE", "La note obtenue ne peut pas dépasser le barème.");
        }
        eval.setSubjectName(req.subjectName().trim());
        eval.setEvaluationType(req.evaluationType());
        eval.setTitle(req.title().trim());
        eval.setScore(req.score());
        eval.setMaxScore(maxScore);
        eval.setCoefficient(req.coefficient() != null ? req.coefficient() : 1.0);
        eval.setSemester(req.semester());
        eval.setAcademicYear(req.academicYear());
        eval.setEvaluationDate(req.evaluationDate());
        return transformer.toEvaluationResponse(evaluationRepository.save(eval));
    }

    @Transactional
    public void deleteEvaluation(UUID evaluationId) {
        UUID studentId = SecurityUtils.getCurrentUser().id();
        PersonalEvaluation eval = evaluationRepository.findByIdAndStudentId(evaluationId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Évaluation personnelle introuvable."));
        evaluationRepository.delete(eval);
    }

    @Transactional(readOnly = true)
    public GradeDtos.GradesSummaryResponse getMySummary() {
        UUID studentId = SecurityUtils.getCurrentUser().id();
        List<PersonalEvaluation> list = evaluationRepository.findByStudentIdOrderByEvaluationDateDesc(studentId);

        Map<String, Double> bySubject = computeGroupedAverage(list.stream().collect(Collectors.groupingBy(PersonalEvaluation::getSubjectName)));
        Map<String, Double> bySemester = computeGroupedAverage(list.stream()
                .filter(e -> e.getSemester() != null && !e.getSemester().isBlank())
                .collect(Collectors.groupingBy(PersonalEvaluation::getSemester)));
        Map<String, Double> byYear = computeGroupedAverage(list.stream()
                .filter(e -> e.getAcademicYear() != null && !e.getAcademicYear().isBlank())
                .collect(Collectors.groupingBy(PersonalEvaluation::getAcademicYear)));

        String bestSubject = null;
        double bestAvg = 0.0;
        for (var entry : bySubject.entrySet()) {
            if (bestSubject == null || entry.getValue() > bestAvg) {
                bestSubject = entry.getKey();
                bestAvg = entry.getValue();
            }
        }

        return new GradeDtos.GradesSummaryResponse(
                weightedAverageOn20(list),
                list.size(),
                bestSubject,
                bestAvg,
                bySubject,
                bySemester,
                byYear,
                list.stream().map(transformer::toEvaluationResponse).toList()
        );
    }

    private Map<String, Double> computeGroupedAverage(Map<String, List<PersonalEvaluation>> grouped) {
        Map<String, Double> out = new LinkedHashMap<>();
        grouped.forEach((k, v) -> out.put(k, weightedAverageOn20(v)));
        return out;
    }

    private double weightedAverageOn20(List<PersonalEvaluation> evals) {
        if (evals.isEmpty()) return 0.0;
        double weightedSum = 0.0;
        double coeffSum = 0.0;
        for (PersonalEvaluation e : evals) {
            double normalizedOn20 = (e.getScore() / e.getMaxScore()) * 20.0;
            weightedSum += normalizedOn20 * e.getCoefficient();
            coeffSum += e.getCoefficient();
        }
        return coeffSum == 0.0 ? 0.0 : Math.round((weightedSum / coeffSum) * 100.0) / 100.0;
    }
}
