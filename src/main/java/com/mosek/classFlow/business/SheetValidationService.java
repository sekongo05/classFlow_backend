package com.mosek.classFlow.business;

import com.mosek.classFlow.dto.AttendanceDtos;
import com.mosek.classFlow.entity.AttendanceSheet;
import com.mosek.classFlow.entity.CourseSession;
import com.mosek.classFlow.entity.Role;
import com.mosek.classFlow.entity.SessionStatus;
import com.mosek.classFlow.entity.SheetValidation;
import com.mosek.classFlow.entity.ValidationDecision;
import com.mosek.classFlow.exception.BusinessRuleException;
import com.mosek.classFlow.exception.ResourceNotFoundException;
import com.mosek.classFlow.exception.UnauthorizedActionException;
import com.mosek.classFlow.repository.AttendanceSheetRepository;
import com.mosek.classFlow.repository.CourseSessionRepository;
import com.mosek.classFlow.repository.SheetValidationRepository;
import com.mosek.classFlow.security.AuthenticatedUser;
import com.mosek.classFlow.security.SecurityUtils;
import com.mosek.classFlow.transformer.DomainTransformer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SheetValidationService {

    private final TimetableAndSessionService sessionService;
    private final CourseSessionRepository sessionRepository;
    private final AttendanceSheetRepository sheetRepository;
    private final SheetValidationRepository validationRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final DomainTransformer transformer;

    @Transactional
    public AttendanceDtos.ValidationResponse validateOrDispute(UUID sessionId, AttendanceDtos.ValidateSheetRequest req) {
        AuthenticatedUser actor = SecurityUtils.getCurrentUser();
        CourseSession session = sessionService.findSessionOrThrow(sessionId);

        if (Role.TEACHER.name().equals(actor.role()) && session.getTeacherId() != null && !session.getTeacherId().equals(actor.id())) {
            throw new UnauthorizedActionException("Vous n'êtes pas l'enseignant assigné à cette séance.");
        }
        AttendanceSheet sheet = sheetRepository.findByCourseSessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Feuille de présence introuvable pour cette séance."));

        if (req.decision() == ValidationDecision.DISPUTED && (req.comment() == null || req.comment().isBlank())) {
            throw new BusinessRuleException("COMMENT_REQUIRED", "Un commentaire est obligatoire pour signaler une anomalie.");
        }

        SheetValidation saved = validationRepository.save(SheetValidation.builder()
                .courseSessionId(sessionId)
                .sheetId(sheet.getId())
                .sheetVersion(sheet.getSheetVersion())
                .teacherId(actor.id())
                .decision(req.decision())
                .comment(req.comment())
                .build());

        if (req.decision() == ValidationDecision.VALIDATED) {
            session.setStatus(SessionStatus.VALIDATED);
            sheet.setValidatedBy(actor.id());
            sheet.setValidatedAt(Instant.now());
            sheetRepository.save(sheet);
        } else {
            session.setStatus(SessionStatus.DISPUTED);
            notificationService.notifyClass(session.getAcademicClassId(), "SHEET_DISPUTED",
                    "Anomalie signalée sur une feuille", "L'enseignant a signalé une anomalie sur le cours du " + session.getSessionDate() + ".");
        }
        sessionRepository.save(session);

        auditService.record("SHEET_" + req.decision().name(), "AttendanceSheet", sheet.getId().toString(),
                actor.id(), actor.email(), actor.role(), "v" + sheet.getSheetVersion());

        return transformer.toValidationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AttendanceDtos.ValidationResponse> getSessionValidationHistory(UUID sessionId) {
        return validationRepository.findByCourseSessionIdOrderByCreatedAtDesc(sessionId).stream()
                .map(transformer::toValidationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceDtos.ValidationResponse> getMyValidations() {
        return validationRepository.findByTeacherIdOrderByCreatedAtDesc(SecurityUtils.getCurrentUser().id()).stream()
                .map(transformer::toValidationResponse)
                .toList();
    }
}
