package com.mosek.classFlow.business;

import com.mosek.classFlow.dto.AttendanceDtos;
import com.mosek.classFlow.entity.AttendanceCorrection;
import com.mosek.classFlow.entity.AttendanceMode;
import com.mosek.classFlow.entity.AttendanceRecord;
import com.mosek.classFlow.entity.AttendanceSheet;
import com.mosek.classFlow.entity.AttendanceStatus;
import com.mosek.classFlow.entity.ClassRosterEntry;
import com.mosek.classFlow.entity.CourseSession;
import com.mosek.classFlow.entity.Room;
import com.mosek.classFlow.entity.SessionStatus;
import com.mosek.classFlow.entity.User;
import com.mosek.classFlow.exception.BusinessRuleException;
import com.mosek.classFlow.exception.DuplicateResourceException;
import com.mosek.classFlow.exception.ResourceNotFoundException;
import com.mosek.classFlow.exception.UnauthorizedActionException;
import com.mosek.classFlow.repository.AttendanceCorrectionRepository;
import com.mosek.classFlow.repository.AttendanceRecordRepository;
import com.mosek.classFlow.repository.AttendanceSheetRepository;
import com.mosek.classFlow.repository.ClassRosterEntryRepository;
import com.mosek.classFlow.repository.CourseSessionRepository;
import com.mosek.classFlow.repository.UserRepository;
import com.mosek.classFlow.security.AuthenticatedUser;
import com.mosek.classFlow.security.SecurityUtils;
import com.mosek.classFlow.transformer.DomainTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final TimetableAndSessionService sessionService;
    private final AcademicService academicService;
    private final RoomService roomService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CourseSessionRepository sessionRepository;
    private final ClassRosterEntryRepository rosterRepository;
    private final UserRepository userRepository;
    private final AttendanceSheetRepository sheetRepository;
    private final AttendanceRecordRepository recordRepository;
    private final AttendanceCorrectionRepository correctionRepository;
    private final DomainTransformer transformer;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${classflow.attendance.late-tolerance-minutes:15}")
    private int lateToleranceMinutes;

    @Value("${classflow.attendance.max-late-minutes:60}")
    private int maxLateMinutes;

    @Transactional
    public AttendanceDtos.RecordResponse checkIn(AttendanceDtos.CheckInRequest req) {
        AuthenticatedUser current = SecurityUtils.getCurrentUser();
        CourseSession session = sessionService.findSessionOrThrow(req.courseSessionId());

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BusinessRuleException("SESSION_NOT_OPEN", "Le pointage n'est pas ouvert pour cette séance.");
        }
        if (current.academicClassId() == null || !current.academicClassId().equals(session.getAcademicClassId())) {
            throw new UnauthorizedActionException("Vous n'appartenez pas à la classe concernée par cette séance.");
        }
        if (recordRepository.existsByCourseSessionIdAndStudentId(session.getId(), current.id())) {
            throw new DuplicateResourceException("Votre présence a déjà été enregistrée pour cette séance.");
        }

        // RM06 : vérification réseau local de la salle (sauf si mode secours activé par le délégué)
        AttendanceMode mode = session.isEmergencyMode() ? AttendanceMode.EMERGENCY : AttendanceMode.NORMAL;
        if (!session.isEmergencyMode()) {
            Room room = roomService.findRoomOrThrow(session.getRoomId());
            roomService.verifyRoomNetwork(room, req.qrCodeToken(), req.wifiSsid(), req.networkProof());
        }

        User student = userRepository.findById(current.id())
                .orElseThrow(() -> new ResourceNotFoundException("Compte étudiant introuvable."));

        // Section 7.2 : tolérance paramétrable + limite d'1h
        Instant now = Instant.now();
        Instant sessionStart = LocalDateTime.of(session.getSessionDate(), session.getStartTime()).toInstant(ZoneOffset.UTC);
        long minutesLate = Duration.between(sessionStart, now).toMinutes();
        if (minutesLate > maxLateMinutes) {
            throw new BusinessRuleException("CHECKIN_WINDOW_EXPIRED",
                    "Le délai de pointage (1 heure après le début du cours) est dépassé. Contactez votre délégué.");
        }
        AttendanceStatus status = minutesLate > lateToleranceMinutes ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;

        AttendanceRecord saved = recordRepository.save(AttendanceRecord.builder()
                .courseSessionId(session.getId())
                .studentId(student.getId())
                .matricule(student.getMatricule() != null ? student.getMatricule() : "N/A")
                .studentName(student.getLastName() + " " + student.getFirstName())
                .status(status)
                .mode(mode)
                .checkedInAt(now)
                .regularized(false)
                .build());

        AttendanceSheet sheet = refreshSheetCounters(session.getId(), mode == AttendanceMode.EMERGENCY);
        broadcastLive("CHECK_IN", sheet, saved);
        return transformer.toRecordResponse(saved);
    }

    @Transactional
    public AttendanceDtos.RecordResponse correctAttendance(UUID recordId, AttendanceDtos.CorrectAttendanceRequest req) {
        AuthenticatedUser actor = SecurityUtils.getCurrentUser();
        AttendanceRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Enregistrement de présence introuvable."));
        CourseSession session = sessionService.findSessionOrThrow(record.getCourseSessionId());
        academicService.assertCanManageClass(session.getAcademicClassId(), actor);

        AttendanceStatus previous = record.getStatus();
        record.setStatus(req.newStatus());
        record.setRegularized(true);
        AttendanceRecord saved = recordRepository.save(record);

        correctionRepository.save(AttendanceCorrection.builder()
                .attendanceRecordId(saved.getId())
                .courseSessionId(session.getId())
                .previousStatus(previous)
                .newStatus(req.newStatus())
                .modifiedBy(actor.id())
                .reason(req.reason().trim())
                .build());

        AttendanceSheet sheet = refreshSheetCounters(session.getId(), false);
        // Section 9.3 : toute modification après validation incrémente la version et impose une nouvelle validation
        if (session.getStatus() == SessionStatus.VALIDATED || session.getStatus() == SessionStatus.DISPUTED) {
            sheet.setSheetVersion(sheet.getSheetVersion() + 1);
            sheet.setValidatedAt(null);
            sheet.setValidatedBy(null);
            sheetRepository.save(sheet);
            session.setStatus(SessionStatus.PENDING_VALIDATION);
            sessionRepository.save(session);
            notificationService.notifyUser(session.getTeacherId(), "SHEET_CORRECTED",
                    "Feuille corrigée à revalider", "La feuille du " + session.getSessionDate() + " a été corrigée (v" + sheet.getSheetVersion() + ").");
        }

        auditService.record("ATTENDANCE_CORRECTED", "AttendanceRecord", saved.getId().toString(),
                actor.id(), actor.email(), actor.role(), previous + " -> " + req.newStatus() + " : " + req.reason());
        broadcastLive("CORRECTION", sheet, saved);
        return transformer.toRecordResponse(saved);
    }

    @Transactional
    public AttendanceDtos.SheetResponse closeSession(UUID sessionId) {
        CourseSession session = sessionService.findSessionOrThrow(sessionId);
        if (session.getStatus() != SessionStatus.IN_PROGRESS && session.getStatus() != SessionStatus.SCHEDULED) {
            throw new BusinessRuleException("SESSION_ALREADY_CLOSED", "Cette séance est déjà clôturée ou inactive.");
        }

        // Marque absents tous les étudiants liés de la liste de classe qui n'ont pas pointé
        List<ClassRosterEntry> roster = rosterRepository.findByAcademicClassIdOrderByLastNameAscFirstNameAsc(session.getAcademicClassId());
        for (ClassRosterEntry entry : roster) {
            UUID studentId = entry.getLinkedUserId();
            if (studentId != null && !recordRepository.existsByCourseSessionIdAndStudentId(sessionId, studentId)) {
                recordRepository.save(AttendanceRecord.builder()
                        .courseSessionId(sessionId)
                        .studentId(studentId)
                        .matricule(entry.getMatricule())
                        .studentName(entry.getLastName() + " " + entry.getFirstName())
                        .status(AttendanceStatus.ABSENT)
                        .mode(session.isEmergencyMode() ? AttendanceMode.EMERGENCY : AttendanceMode.NORMAL)
                        .build());
            }
        }

        session.setStatus(SessionStatus.PENDING_VALIDATION);
        sessionRepository.save(session);

        AttendanceSheet sheet = refreshSheetCounters(sessionId, session.isEmergencyMode());
        sheet.setClosedAt(Instant.now());
        sheetRepository.save(sheet);

        notificationService.notifyUser(session.getTeacherId(), "SHEET_READY_FOR_VALIDATION",
                "Feuille de présence à valider", "La feuille du cours du " + session.getSessionDate() + " est prête pour validation.");
        return getSessionSheet(sessionId);
    }

    @Transactional
    public void toggleEmergencyMode(UUID sessionId, boolean enabled) {
        CourseSession session = sessionService.findSessionOrThrow(sessionId);
        AuthenticatedUser actor = SecurityUtils.getCurrentUser();
        academicService.assertCanManageClass(session.getAcademicClassId(), actor);
        session.setEmergencyMode(enabled);
        sessionRepository.save(session);
        auditService.record(enabled ? "EMERGENCY_MODE_ACTIVATED" : "EMERGENCY_MODE_DEACTIVATED",
                "CourseSession", sessionId.toString(), actor.id(), actor.email(), actor.role(), "emergency=" + enabled);
    }

    @Transactional(readOnly = true)
    public AttendanceDtos.SheetResponse getSessionSheet(UUID sessionId) {
        CourseSession session = sessionService.findSessionOrThrow(sessionId);
        AttendanceSheet sheet = sheetRepository.findByCourseSessionId(sessionId)
                .orElseGet(() -> AttendanceSheet.builder().courseSessionId(sessionId).sheetVersion(1).build());
        List<AttendanceRecord> records = recordRepository.findByCourseSessionIdOrderByStudentNameAsc(sessionId);
        return transformer.toSheetResponse(sheet, session.getStatus(), records);
    }

    @Transactional(readOnly = true)
    public List<AttendanceDtos.RecordResponse> getMyHistory() {
        return recordRepository.findByStudentIdOrderByCreatedAtDesc(SecurityUtils.getCurrentUser().id()).stream()
                .map(transformer::toRecordResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceDtos.CorrectionResponse> getSessionCorrections(UUID sessionId) {
        return correctionRepository.findByCourseSessionIdOrderByCreatedAtDesc(sessionId).stream()
                .map(transformer::toCorrectionResponse)
                .toList();
    }

    @Scheduled(fixedDelayString = "${classflow.attendance.scheduler-interval-ms:60000}")
    @Transactional
    public void autoTransitionSessions() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalTime now = LocalTime.now(ZoneOffset.UTC);

        for (CourseSession s : sessionRepository.findByStatusAndSessionDate(SessionStatus.SCHEDULED, today)) {
            if (!now.isBefore(s.getStartTime()) && now.isBefore(s.getEndTime())) {
                s.setStatus(SessionStatus.IN_PROGRESS);
                sessionRepository.save(s);
                notificationService.notifyClass(s.getAcademicClassId(), "CHECKIN_OPENED",
                        "Début du cours", "Le pointage est ouvert pour votre cours de " + s.getStartTime() + ".");
            }
        }
        for (CourseSession s : sessionRepository.findByStatusAndSessionDate(SessionStatus.IN_PROGRESS, today)) {
            if (!now.isBefore(s.getEndTime())) {
                closeSession(s.getId());
            }
        }
    }

    private AttendanceSheet refreshSheetCounters(UUID sessionId, boolean hasEmergency) {
        AttendanceSheet sheet = sheetRepository.findByCourseSessionId(sessionId)
                .orElseGet(() -> AttendanceSheet.builder().courseSessionId(sessionId).sheetVersion(1).build());
        List<AttendanceRecord> records = recordRepository.findByCourseSessionIdOrderByStudentNameAsc(sessionId);
        int present = 0, late = 0, absent = 0;
        boolean emergencyFound = hasEmergency || sheet.isContainsEmergencyRecords();
        for (AttendanceRecord r : records) {
            if (r.getMode() == AttendanceMode.EMERGENCY) emergencyFound = true;
            switch (r.getStatus()) {
                case PRESENT -> present++;
                case LATE -> late++;
                case ABSENT -> absent++;
            }
        }
        sheet.setPresentCount(present);
        sheet.setLateCount(late);
        sheet.setAbsentCount(absent);
        sheet.setContainsEmergencyRecords(emergencyFound);
        return sheetRepository.save(sheet);
    }

    private void broadcastLive(String eventType, AttendanceSheet sheet, AttendanceRecord record) {
        try {
            var event = new AttendanceDtos.LiveAttendanceEvent(
                    eventType, sheet.getCourseSessionId(),
                    sheet.getPresentCount(), sheet.getLateCount(), sheet.getAbsentCount(),
                    transformer.toRecordResponse(record)
            );
            messagingTemplate.convertAndSend("/topic/sessions/" + sheet.getCourseSessionId() + "/attendance", event);
        } catch (Exception ex) {
            log.warn("WebSocket broadcast skipped: {}", ex.getMessage());
        }
    }
}
