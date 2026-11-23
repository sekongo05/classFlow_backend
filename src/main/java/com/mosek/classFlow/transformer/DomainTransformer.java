package com.mosek.classFlow.transformer;

import com.mosek.classFlow.dto.AttendanceDtos;
import com.mosek.classFlow.dto.GradeDtos;
import com.mosek.classFlow.dto.NotificationDtos;
import com.mosek.classFlow.dto.ScheduleDtos;
import com.mosek.classFlow.entity.AttendanceCorrection;
import com.mosek.classFlow.entity.AttendanceRecord;
import com.mosek.classFlow.entity.AttendanceSheet;
import com.mosek.classFlow.entity.CourseSession;
import com.mosek.classFlow.entity.Notification;
import com.mosek.classFlow.entity.PersonalEvaluation;
import com.mosek.classFlow.entity.SessionStatus;
import com.mosek.classFlow.entity.SheetValidation;
import com.mosek.classFlow.entity.TeacherAbsenceDeclaration;
import com.mosek.classFlow.entity.TimetableSlot;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DomainTransformer {

    public ScheduleDtos.TimetableSlotResponse toSlotResponse(TimetableSlot s) {
        return new ScheduleDtos.TimetableSlotResponse(
                s.getId(), s.getAcademicClassId(), s.getSubjectId(), s.getTeacherId(),
                s.getRoomId(), s.getDayOfWeek(), s.getSpecificDate(), s.getStartTime(), s.getEndTime()
        );
    }

    public ScheduleDtos.SessionResponse toSessionResponse(CourseSession s) {
        return new ScheduleDtos.SessionResponse(
                s.getId(), s.getAcademicClassId(), s.getSubjectId(), s.getTeacherId(), s.getRoomId(),
                s.getSessionDate(), s.getStartTime(), s.getEndTime(), s.getStatus(), s.isEmergencyMode(), s.getNotes()
        );
    }

    public ScheduleDtos.TeacherAbsenceResponse toAbsenceResponse(TeacherAbsenceDeclaration d) {
        return new ScheduleDtos.TeacherAbsenceResponse(
                d.getId(), d.getCourseSessionId(), d.getDeclaredBy(),
                d.getDeclarationType(), d.getComment(), d.isActive(), d.getCreatedAt()
        );
    }

    public AttendanceDtos.RecordResponse toRecordResponse(AttendanceRecord r) {
        return new AttendanceDtos.RecordResponse(
                r.getId(), r.getCourseSessionId(), r.getStudentId(), r.getMatricule(), r.getStudentName(),
                r.getStatus(), r.getMode(), r.getCheckedInAt(), r.isRegularized()
        );
    }

    public AttendanceDtos.CorrectionResponse toCorrectionResponse(AttendanceCorrection c) {
        return new AttendanceDtos.CorrectionResponse(
                c.getId(), c.getAttendanceRecordId(), c.getPreviousStatus(),
                c.getNewStatus(), c.getModifiedBy(), c.getReason(), c.getCreatedAt()
        );
    }

    public AttendanceDtos.SheetResponse toSheetResponse(AttendanceSheet sheet, SessionStatus status, List<AttendanceRecord> records) {
        return new AttendanceDtos.SheetResponse(
                sheet.getId(), sheet.getCourseSessionId(), status, sheet.getSheetVersion(),
                sheet.getPresentCount(), sheet.getLateCount(), sheet.getAbsentCount(),
                sheet.isContainsEmergencyRecords(), sheet.getClosedAt(), sheet.getValidatedBy(), sheet.getValidatedAt(),
                records.stream().map(this::toRecordResponse).toList()
        );
    }

    public AttendanceDtos.ValidationResponse toValidationResponse(SheetValidation v) {
        return new AttendanceDtos.ValidationResponse(
                v.getId(), v.getCourseSessionId(), v.getSheetId(), v.getSheetVersion(),
                v.getTeacherId(), v.getDecision(), v.getComment(), v.getCreatedAt()
        );
    }

    public GradeDtos.EvaluationResponse toEvaluationResponse(PersonalEvaluation e) {
        return new GradeDtos.EvaluationResponse(
                e.getId(), e.getSubjectName(), e.getEvaluationType(), e.getTitle(),
                e.getScore(), e.getMaxScore(), e.getCoefficient(), e.getSemester(), e.getAcademicYear(), e.getEvaluationDate()
        );
    }

    public NotificationDtos.NotificationResponse toNotificationResponse(Notification n) {
        return new NotificationDtos.NotificationResponse(
                n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.isRead(), n.getCreatedAt()
        );
    }
}
