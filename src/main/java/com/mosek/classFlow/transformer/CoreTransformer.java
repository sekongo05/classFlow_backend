package com.mosek.classFlow.transformer;

import com.mosek.classFlow.dto.AcademicDtos;
import com.mosek.classFlow.dto.AuthDtos;
import com.mosek.classFlow.dto.RoomDtos;
import com.mosek.classFlow.entity.AcademicClass;
import com.mosek.classFlow.entity.ClassRosterEntry;
import com.mosek.classFlow.entity.Room;
import com.mosek.classFlow.entity.RosterImportHistory;
import com.mosek.classFlow.entity.Subject;
import com.mosek.classFlow.entity.User;
import org.springframework.stereotype.Component;

@Component
public class CoreTransformer {

    public AuthDtos.UserResponse toUserResponse(User u) {
        return new AuthDtos.UserResponse(
                u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(),
                u.getMatricule(), u.getRole(), u.getAcademicClassId(), u.isActive()
        );
    }

    public AcademicDtos.ClassResponse toClassResponse(AcademicClass c) {
        return new AcademicDtos.ClassResponse(
                c.getId(), c.getName(), c.getTrack(), c.getLevel(), c.getAcademicYear(),
                c.getExpectedSize(), c.getDelegateId(), c.getDeputyDelegateId()
        );
    }

    public AcademicDtos.SubjectResponse toSubjectResponse(Subject s) {
        return new AcademicDtos.SubjectResponse(
                s.getId(), s.getAcademicClassId(), s.getCode(), s.getName(), s.getDefaultCoefficient(), s.getTeacherId()
        );
    }

    public AcademicDtos.RosterEntryResponse toRosterEntryResponse(ClassRosterEntry e) {
        return new AcademicDtos.RosterEntryResponse(
                e.getId(), e.getAcademicClassId(), e.getMatricule(),
                e.getLastName(), e.getFirstName(), e.getEmail(), e.getLinkedUserId()
        );
    }

    public AcademicDtos.RosterImportHistoryResponse toRosterImportHistoryResponse(RosterImportHistory h) {
        return new AcademicDtos.RosterImportHistoryResponse(
                h.getId(), h.getAcademicClassId(), h.getImportedBy(),
                h.getFileName(), h.getImportedCount(), h.getUpdatedCount(), h.getCreatedAt()
        );
    }

    public RoomDtos.RoomResponse toRoomResponse(Room r) {
        return new RoomDtos.RoomResponse(
                r.getId(), r.getCode(), r.getName(), r.getWifiSsid(), r.getQrCodeToken(), r.isOperational()
        );
    }
}
