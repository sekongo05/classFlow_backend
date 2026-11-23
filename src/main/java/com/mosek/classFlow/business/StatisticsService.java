package com.mosek.classFlow.business;

import com.mosek.classFlow.dto.AttendanceDtos;
import com.mosek.classFlow.entity.AttendanceRecord;
import com.mosek.classFlow.entity.AttendanceStatus;
import com.mosek.classFlow.entity.CourseSession;
import com.mosek.classFlow.repository.AttendanceRecordRepository;
import com.mosek.classFlow.repository.ClassRosterEntryRepository;
import com.mosek.classFlow.repository.CourseSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final AttendanceRecordRepository recordRepository;
    private final CourseSessionRepository sessionRepository;
    private final ClassRosterEntryRepository rosterRepository;

    @Transactional(readOnly = true)
    public AttendanceDtos.StudentStatsResponse getStudentStats(UUID studentId) {
        List<AttendanceRecord> records = recordRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        long present = records.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
        long late = records.stream().filter(r -> r.getStatus() == AttendanceStatus.LATE).count();
        long absent = records.stream().filter(r -> r.getStatus() == AttendanceStatus.ABSENT).count();
        long total = records.size();
        double rate = total == 0 ? 0.0 : Math.round(((present + late) * 10000.0) / total) / 100.0;
        return new AttendanceDtos.StudentStatsResponse(studentId, total, present, late, absent, rate);
    }

    @Transactional(readOnly = true)
    public AttendanceDtos.ClassStatsResponse getClassStats(UUID classId) {
        int rosterSize = rosterRepository.findByAcademicClassIdOrderByLastNameAscFirstNameAsc(classId).size();
        List<CourseSession> sessions = sessionRepository.findByAcademicClassIdOrderBySessionDateAscStartTimeAsc(classId);
        if (sessions.isEmpty()) {
            return new AttendanceDtos.ClassStatsResponse(classId, rosterSize, 0, 0.0, 0, 0, 0.0);
        }
        List<UUID> sessionIds = sessions.stream().map(CourseSession::getId).toList();
        List<AttendanceRecord> allRecords = recordRepository.findByCourseSessionIdIn(sessionIds);

        long present = 0, late = 0, absent = 0;
        for (AttendanceRecord r : allRecords) {
            switch (r.getStatus()) {
                case PRESENT -> present++;
                case LATE -> late++;
                case ABSENT -> absent++;
            }
        }
        long totalRecords = present + late + absent;
        double avgPresent = Math.round(((present + late) * 100.0) / sessions.size()) / 100.0;
        double globalRate = totalRecords == 0 ? 0.0 : Math.round(((present + late) * 10000.0) / totalRecords) / 100.0;
        return new AttendanceDtos.ClassStatsResponse(classId, rosterSize, sessions.size(), avgPresent, late, absent, globalRate);
    }
}
