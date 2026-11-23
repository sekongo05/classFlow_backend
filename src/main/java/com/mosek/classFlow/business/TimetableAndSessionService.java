package com.mosek.classFlow.business;

import com.mosek.classFlow.dto.ScheduleDtos;
import com.mosek.classFlow.entity.AbsenceDeclarationType;
import com.mosek.classFlow.entity.CourseSession;
import com.mosek.classFlow.entity.Role;
import com.mosek.classFlow.entity.SessionStatus;
import com.mosek.classFlow.entity.TeacherAbsenceDeclaration;
import com.mosek.classFlow.entity.TimetableSlot;
import com.mosek.classFlow.exception.BusinessRuleException;
import com.mosek.classFlow.exception.ResourceNotFoundException;
import com.mosek.classFlow.repository.CourseSessionRepository;
import com.mosek.classFlow.repository.TeacherAbsenceDeclarationRepository;
import com.mosek.classFlow.repository.TimetableSlotRepository;
import com.mosek.classFlow.security.AuthenticatedUser;
import com.mosek.classFlow.security.SecurityUtils;
import com.mosek.classFlow.transformer.DomainTransformer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimetableAndSessionService {

    private final AcademicService academicService;
    private final RoomService roomService;
    private final TimetableSlotRepository slotRepository;
    private final CourseSessionRepository sessionRepository;
    private final TeacherAbsenceDeclarationRepository absenceRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final DomainTransformer transformer;

    @Transactional
    public ScheduleDtos.TimetableSlotResponse addSlot(UUID classId, ScheduleDtos.TimetableSlotRequest req) {
        academicService.assertCanManageClass(classId, SecurityUtils.getCurrentUser());
        roomService.findRoomOrThrow(req.roomId());
        if (!req.endTime().isAfter(req.startTime())) {
            throw new BusinessRuleException("INVALID_TIME_RANGE", "L'heure de fin doit être postérieure à l'heure de début.");
        }
        TimetableSlot saved = slotRepository.save(TimetableSlot.builder()
                .academicClassId(classId)
                .subjectId(req.subjectId())
                .teacherId(req.teacherId())
                .roomId(req.roomId())
                .dayOfWeek(req.dayOfWeek())
                .specificDate(req.specificDate())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .build());
        return transformer.toSlotResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ScheduleDtos.TimetableSlotResponse> listSlots(UUID classId) {
        return slotRepository.findByAcademicClassIdOrderByDayOfWeekAscStartTimeAsc(classId).stream()
                .map(transformer::toSlotResponse)
                .toList();
    }

    @Transactional
    public void deleteSlot(UUID classId, UUID slotId) {
        academicService.assertCanManageClass(classId, SecurityUtils.getCurrentUser());
        TimetableSlot slot = slotRepository.findById(slotId)
                .filter(s -> s.getAcademicClassId().equals(classId))
                .orElseThrow(() -> new ResourceNotFoundException("Créneau d'emploi du temps introuvable."));
        slotRepository.delete(slot);
    }

    @Transactional
    public List<ScheduleDtos.SessionResponse> generateSessions(UUID classId, ScheduleDtos.GenerateSessionsRequest req) {
        academicService.assertCanManageClass(classId, SecurityUtils.getCurrentUser());
        if (req.toDate().isBefore(req.fromDate())) {
            throw new BusinessRuleException("INVALID_DATE_RANGE", "La date de fin doit être postérieure ou égale à la date de début.");
        }
        List<TimetableSlot> slots = slotRepository.findByAcademicClassIdOrderByDayOfWeekAscStartTimeAsc(classId);
        List<CourseSession> created = new ArrayList<>();

        for (LocalDate d = req.fromDate(); !d.isAfter(req.toDate()); d = d.plusDays(1)) {
            for (TimetableSlot slot : slots) {
                boolean matchesDate = slot.getSpecificDate() != null
                        ? slot.getSpecificDate().equals(d)
                        : slot.getDayOfWeek() == d.getDayOfWeek();
                if (!matchesDate) continue;

                boolean exists = sessionRepository.existsByAcademicClassIdAndSubjectIdAndSessionDateAndStartTime(
                        classId, slot.getSubjectId(), d, slot.getStartTime());
                if (!exists) {
                    created.add(sessionRepository.save(CourseSession.builder()
                            .academicClassId(classId)
                            .subjectId(slot.getSubjectId())
                            .teacherId(slot.getTeacherId())
                            .roomId(slot.getRoomId())
                            .sessionDate(d)
                            .startTime(slot.getStartTime())
                            .endTime(slot.getEndTime())
                            .status(SessionStatus.SCHEDULED)
                            .build()));
                }
            }
        }
        return created.stream().map(transformer::toSessionResponse).toList();
    }

    @Transactional
    public ScheduleDtos.SessionResponse createManualSession(UUID classId, ScheduleDtos.CreateSessionRequest req) {
        academicService.assertCanManageClass(classId, SecurityUtils.getCurrentUser());
        roomService.findRoomOrThrow(req.roomId());
        if (!req.endTime().isAfter(req.startTime())) {
            throw new BusinessRuleException("INVALID_TIME_RANGE", "L'heure de fin doit être postérieure à l'heure de début.");
        }
        CourseSession saved = sessionRepository.save(CourseSession.builder()
                .academicClassId(classId)
                .subjectId(req.subjectId())
                .teacherId(req.teacherId())
                .roomId(req.roomId())
                .sessionDate(req.sessionDate())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .status(SessionStatus.SCHEDULED)
                .notes(req.notes())
                .build());
        notificationService.notifyClass(classId, "SESSION_ADDED", "Nouvelle séance programmée",
                "Une séance a été ajoutée le " + saved.getSessionDate() + " à " + saved.getStartTime() + ".");
        return transformer.toSessionResponse(saved);
    }

    @Transactional
    public ScheduleDtos.SessionResponse updateSession(UUID sessionId, ScheduleDtos.UpdateSessionRequest req) {
        CourseSession session = findSessionOrThrow(sessionId);
        AuthenticatedUser actor = SecurityUtils.getCurrentUser();
        academicService.assertCanManageClass(session.getAcademicClassId(), actor);

        if (req.roomId() != null) {
            roomService.findRoomOrThrow(req.roomId());
            session.setRoomId(req.roomId());
        }
        if (req.teacherId() != null) session.setTeacherId(req.teacherId());
        if (req.sessionDate() != null) session.setSessionDate(req.sessionDate());
        if (req.startTime() != null) session.setStartTime(req.startTime());
        if (req.endTime() != null) session.setEndTime(req.endTime());
        if (req.notes() != null) session.setNotes(req.notes());

        CourseSession saved = sessionRepository.save(session);
        notificationService.notifyClass(saved.getAcademicClassId(), "SESSION_UPDATED", "Séance modifiée",
                "La séance du " + saved.getSessionDate() + " à " + saved.getStartTime() + " a été mise à jour.");
        auditService.record("SESSION_UPDATED", "CourseSession", saved.getId().toString(),
                actor.id(), actor.email(), actor.role(), "date=" + saved.getSessionDate());
        return transformer.toSessionResponse(saved);
    }

    @Transactional
    public ScheduleDtos.SessionResponse openSession(UUID sessionId) {
        CourseSession session = findSessionOrThrow(sessionId);
        academicService.assertCanManageClass(session.getAcademicClassId(), SecurityUtils.getCurrentUser());
        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new BusinessRuleException("INVALID_SESSION_STATE", "Seule une séance programmée peut être ouverte.");
        }
        session.setStatus(SessionStatus.IN_PROGRESS);
        CourseSession saved = sessionRepository.save(session);
        notificationService.notifyClass(saved.getAcademicClassId(), "CHECKIN_OPENED", "Pointage ouvert",
                "Le pointage pour votre cours est désormais ouvert.");
        return transformer.toSessionResponse(saved);
    }

    @Transactional
    public ScheduleDtos.TeacherAbsenceResponse declareTeacherAbsence(UUID sessionId, ScheduleDtos.TeacherAbsenceRequest req) {
        CourseSession session = findSessionOrThrow(sessionId);
        AuthenticatedUser actor = SecurityUtils.getCurrentUser();
        academicService.assertCanManageClass(session.getAcademicClassId(), actor);

        TeacherAbsenceDeclaration declaration = absenceRepository.save(TeacherAbsenceDeclaration.builder()
                .courseSessionId(sessionId)
                .declaredBy(actor.id())
                .declarationType(req.declarationType())
                .comment(req.comment())
                .active(true)
                .build());

        if (req.declarationType() == AbsenceDeclarationType.CANCELLATION || req.declarationType() == AbsenceDeclarationType.TEACHER_ABSENCE) {
            session.setStatus(SessionStatus.CANCELLED);
        } else if (req.declarationType() == AbsenceDeclarationType.POSTPONEMENT) {
            session.setStatus(SessionStatus.POSTPONED);
            if (req.newDate() != null) session.setSessionDate(req.newDate());
            if (req.newStartTime() != null) session.setStartTime(req.newStartTime());
            if (req.newEndTime() != null) session.setEndTime(req.newEndTime());
        } else if (req.declarationType() == AbsenceDeclarationType.TEACHER_CHANGE && req.replacementTeacherId() != null) {
            session.setTeacherId(req.replacementTeacherId());
        }
        sessionRepository.save(session);

        notificationService.notifyClass(session.getAcademicClassId(), "TEACHER_ABSENCE",
                "Mise à jour de cours", "Déclaration enregistrée (" + req.declarationType() + ") pour le cours du " + session.getSessionDate() + ".");
        auditService.record("TEACHER_ABSENCE_DECLARED", "CourseSession", sessionId.toString(),
                actor.id(), actor.email(), actor.role(), req.declarationType().name());

        return transformer.toAbsenceResponse(declaration);
    }

    @Transactional
    public ScheduleDtos.TeacherAbsenceResponse cancelDeclaration(UUID sessionId, UUID declarationId) {
        CourseSession session = findSessionOrThrow(sessionId);
        AuthenticatedUser actor = SecurityUtils.getCurrentUser();
        academicService.assertCanManageClass(session.getAcademicClassId(), actor);

        TeacherAbsenceDeclaration declaration = absenceRepository.findById(declarationId)
                .filter(d -> d.getCourseSessionId().equals(sessionId))
                .orElseThrow(() -> new ResourceNotFoundException("Déclaration introuvable."));
        declaration.setActive(false);
        absenceRepository.save(declaration);

        if (session.getStatus() == SessionStatus.CANCELLED || session.getStatus() == SessionStatus.POSTPONED) {
            session.setStatus(SessionStatus.SCHEDULED);
            sessionRepository.save(session);
        }
        notificationService.notifyClass(session.getAcademicClassId(), "SESSION_REACTIVATED",
                "Cours rétabli", "Le cours du " + session.getSessionDate() + " est finalement maintenu.");
        return transformer.toAbsenceResponse(declaration);
    }

    @Transactional(readOnly = true)
    public List<ScheduleDtos.SessionResponse> listClassSessions(UUID classId) {
        return sessionRepository.findByAcademicClassIdOrderBySessionDateAscStartTimeAsc(classId).stream()
                .map(transformer::toSessionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduleDtos.SessionResponse> listMySessions() {
        AuthenticatedUser current = SecurityUtils.getCurrentUser();
        List<CourseSession> sessions = Role.TEACHER.name().equals(current.role())
                ? sessionRepository.findByTeacherIdOrderBySessionDateAscStartTimeAsc(current.id())
                : (current.academicClassId() != null
                        ? sessionRepository.findByAcademicClassIdOrderBySessionDateAscStartTimeAsc(current.academicClassId())
                        : List.of());
        return sessions.stream().map(transformer::toSessionResponse).toList();
    }

    public CourseSession findSessionOrThrow(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Séance de cours introuvable."));
    }
}
