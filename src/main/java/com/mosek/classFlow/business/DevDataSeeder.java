package com.mosek.classFlow.business;

import com.mosek.classFlow.entity.AcademicClass;
import com.mosek.classFlow.entity.ClassRosterEntry;
import com.mosek.classFlow.entity.CourseSession;
import com.mosek.classFlow.entity.Role;
import com.mosek.classFlow.entity.Room;
import com.mosek.classFlow.entity.SessionStatus;
import com.mosek.classFlow.entity.Subject;
import com.mosek.classFlow.entity.User;
import com.mosek.classFlow.repository.AcademicClassRepository;
import com.mosek.classFlow.repository.ClassRosterEntryRepository;
import com.mosek.classFlow.repository.CourseSessionRepository;
import com.mosek.classFlow.repository.RoomRepository;
import com.mosek.classFlow.repository.SubjectRepository;
import com.mosek.classFlow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Initialise des comptes et données de démonstration uniquement en profil `dev`.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final AcademicClassRepository classRepository;
    private final ClassRosterEntryRepository rosterRepository;
    private final SubjectRepository subjectRepository;
    private final RoomRepository roomRepository;
    private final CourseSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmailIgnoreCase("admin@classflow.edu")) {
            return;
        }

        userRepository.save(User.builder()
                .email("admin@classflow.edu")
                .passwordHash(passwordEncoder.encode("Admin123!"))
                .firstName("Super")
                .lastName("Admin")
                .role(Role.ADMIN)
                .active(true)
                .build());

        User teacher = userRepository.save(User.builder()
                .email("prof@classflow.edu")
                .passwordHash(passwordEncoder.encode("Prof123!"))
                .firstName("Jean")
                .lastName("Dupont")
                .role(Role.TEACHER)
                .active(true)
                .build());

        User delegate = userRepository.save(User.builder()
                .email("delegue@classflow.edu")
                .passwordHash(passwordEncoder.encode("Delegue123!"))
                .firstName("Awa")
                .lastName("Kone")
                .role(Role.DELEGATE)
                .active(true)
                .build());

        User deputy = userRepository.save(User.builder()
                .email("adjoint@classflow.edu")
                .passwordHash(passwordEncoder.encode("Adjoint123!"))
                .firstName("Marc")
                .lastName("Yao")
                .role(Role.DEPUTY_DELEGATE)
                .active(true)
                .build());

        AcademicClass demoClass = classRepository.save(AcademicClass.builder()
                .name("L3 INFO")
                .track("Informatique")
                .level("L3")
                .academicYear("2026-2027")
                .expectedSize(30)
                .delegateId(delegate.getId())
                .deputyDelegateId(deputy.getId())
                .build());

        delegate.setAcademicClassId(demoClass.getId());
        deputy.setAcademicClassId(demoClass.getId());
        userRepository.saveAll(List.of(delegate, deputy));

        User student = userRepository.save(User.builder()
                .email("etudiant@classflow.edu")
                .passwordHash(passwordEncoder.encode("Etudiant123!"))
                .firstName("Jean")
                .lastName("Kouassi")
                .matricule("ESATIC001")
                .role(Role.STUDENT)
                .academicClassId(demoClass.getId())
                .active(true)
                .build());

        rosterRepository.saveAll(List.of(
                ClassRosterEntry.builder()
                        .academicClassId(demoClass.getId())
                        .matricule("ESATIC001")
                        .lastName("KOUASSI")
                        .firstName("Jean")
                        .email("etudiant@classflow.edu")
                        .linkedUserId(student.getId())
                        .build(),
                ClassRosterEntry.builder()
                        .academicClassId(demoClass.getId())
                        .matricule("ESATIC002")
                        .lastName("TRAORE")
                        .firstName("Moussa")
                        .email("moussa@classflow.edu")
                        .build()
        ));

        Subject subject = subjectRepository.save(Subject.builder()
                .academicClassId(demoClass.getId())
                .code("JAVA")
                .name("Programmation Java")
                .defaultCoefficient(3.0)
                .teacherId(teacher.getId())
                .build());

        Room room = roomRepository.save(Room.builder()
                .code("A204")
                .name("Salle A204")
                .wifiSsid("WIFI-A204")
                .networkSecret("SECRET-A204")
                .qrCodeToken("QR-A204-DEMO")
                .operational(true)
                .build());

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalTime now = LocalTime.now(ZoneOffset.UTC).withSecond(0).withNano(0);
        LocalTime start = now.minusMinutes(5);
        LocalTime end = start.plusHours(2);
        if (end.isBefore(start) || start.isAfter(now)) {
            start = LocalTime.of(8, 0);
            end = LocalTime.of(10, 0);
        }

        sessionRepository.save(CourseSession.builder()
                .academicClassId(demoClass.getId())
                .subjectId(subject.getId())
                .teacherId(teacher.getId())
                .roomId(room.getId())
                .sessionDate(today)
                .startTime(start)
                .endTime(end)
                .status(SessionStatus.IN_PROGRESS)
                .notes("Séance de démonstration ouverte au pointage")
                .build());

        log.info("DevDataSeeder: 5 comptes et données de démonstration initialisés (classe L3 INFO, salle A204).");
    }
}
