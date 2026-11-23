package com.mosek.classFlow.business;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mosek.classFlow.dto.AttendanceDtos;
import com.mosek.classFlow.dto.GradeDtos;
import com.mosek.classFlow.dto.ScheduleDtos;
import com.mosek.classFlow.dto.SyncDtos;
import com.mosek.classFlow.entity.SyncOperationLog;
import com.mosek.classFlow.repository.PersonalEvaluationRepository;
import com.mosek.classFlow.repository.SyncOperationLogRepository;
import com.mosek.classFlow.security.AuthenticatedUser;
import com.mosek.classFlow.security.SecurityUtils;
import com.mosek.classFlow.transformer.DomainTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final SyncOperationLogRepository syncLogRepository;
    private final TimetableAndSessionService sessionService;
    private final PersonalGradeService personalGradeService;
    private final SheetValidationService sheetValidationService;
    private final NotificationService notificationService;
    private final PersonalEvaluationRepository evaluationRepository;
    private final DomainTransformer transformer;
    private final ObjectMapper objectMapper;

    @Transactional
    public SyncDtos.SyncPushResponse push(SyncDtos.SyncPushRequest req) {
        AuthenticatedUser current = SecurityUtils.getCurrentUser();
        List<SyncDtos.SyncOperationResult> results = new ArrayList<>();

        for (SyncDtos.SyncOperationRequest op : req.operations()) {
            // Section 13.3 : idempotence stricte par clientOperationId (zéro doublon)
            if (syncLogRepository.findByClientOperationId(op.clientOperationId()).isPresent()) {
                results.add(new SyncDtos.SyncOperationResult(op.clientOperationId(), "DUPLICATE_IGNORED", "Opération déjà synchronisée."));
                continue;
            }
            String status = "APPLIED";
            String message = "Opération enregistrée.";
            try {
                dispatchOfflineOperation(op);
            } catch (Exception ex) {
                log.warn("Offline sync conflict for {}: {}", op.clientOperationId(), ex.getMessage());
                status = "CONFLICT";
                message = ex.getMessage();
            }

            syncLogRepository.save(SyncOperationLog.builder()
                    .clientOperationId(op.clientOperationId())
                    .userId(current.id())
                    .operationType(op.operationType())
                    .status(status)
                    .message(message)
                    .build());
            results.add(new SyncDtos.SyncOperationResult(op.clientOperationId(), status, message));
        }
        return new SyncDtos.SyncPushResponse(Instant.now(), results);
    }

    @Transactional(readOnly = true)
    public SyncDtos.SyncPullResponse pull() {
        AuthenticatedUser current = SecurityUtils.getCurrentUser();
        var sessions = sessionService.listMySessions();
        var notifications = notificationService.listMyNotifications();
        var evaluations = evaluationRepository.findByStudentIdOrderByEvaluationDateDesc(current.id()).stream()
                .map(transformer::toEvaluationResponse)
                .toList();
        return new SyncDtos.SyncPullResponse(Instant.now(), sessions, notifications, evaluations);
    }

    private void dispatchOfflineOperation(SyncDtos.SyncOperationRequest op) throws Exception {
        if (op.payload() == null || op.payload().isBlank() || "{}".equals(op.payload().trim())) {
            return;
        }
        switch (op.operationType()) {
            case "OFFLINE_EVALUATION" -> {
                GradeDtos.EvaluationRequest req = objectMapper.readValue(op.payload(), GradeDtos.EvaluationRequest.class);
                personalGradeService.addEvaluation(req);
            }
            case "OFFLINE_VALIDATION" -> {
                if (op.courseSessionId() != null) {
                    AttendanceDtos.ValidateSheetRequest req = objectMapper.readValue(op.payload(), AttendanceDtos.ValidateSheetRequest.class);
                    sheetValidationService.validateOrDispute(op.courseSessionId(), req);
                }
            }
            case "OFFLINE_ABSENCE_DECLARATION" -> {
                if (op.courseSessionId() != null) {
                    ScheduleDtos.TeacherAbsenceRequest req = objectMapper.readValue(op.payload(), ScheduleDtos.TeacherAbsenceRequest.class);
                    sessionService.declareTeacherAbsence(op.courseSessionId(), req);
                }
            }
            default -> {
            }
        }
    }
}
