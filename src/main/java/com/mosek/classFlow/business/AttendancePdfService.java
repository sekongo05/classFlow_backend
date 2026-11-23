package com.mosek.classFlow.business;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.mosek.classFlow.dto.AttendanceDtos;
import com.mosek.classFlow.entity.AcademicClass;
import com.mosek.classFlow.entity.CourseSession;
import com.mosek.classFlow.entity.Room;
import com.mosek.classFlow.entity.Subject;
import com.mosek.classFlow.exception.BusinessRuleException;
import com.mosek.classFlow.repository.SubjectRepository;
import com.mosek.classFlow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendancePdfService {

    private final TimetableAndSessionService sessionService;
    private final AcademicService academicService;
    private final RoomService roomService;
    private final AttendanceService attendanceService;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    @Value("${classflow.establishment-name:ClassFlow Institut Supérieur}")
    private String establishmentName;

    @Transactional(readOnly = true)
    public byte[] exportSessionSheetPdf(UUID sessionId) {
        CourseSession session = sessionService.findSessionOrThrow(sessionId);
        AcademicClass academicClass = academicService.findClassOrThrow(session.getAcademicClassId());
        Room room = roomService.findRoomOrThrow(session.getRoomId());
        String subjectName = subjectRepository.findById(session.getSubjectId()).map(Subject::getName).orElse("N/A");
        String teacherName = session.getTeacherId() != null
                ? userRepository.findById(session.getTeacherId()).map(u -> u.getFirstName() + " " + u.getLastName()).orElse("Non assigné")
                : "Non assigné";
        AttendanceDtos.SheetResponse sheet = attendanceService.getSessionSheet(sessionId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document();
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            doc.add(new Paragraph(establishmentName + " — Feuille de présence", titleFont));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Classe : " + academicClass.getName() + " (" + academicClass.getTrack() + " - " + academicClass.getLevel() + ")", bodyFont));
            doc.add(new Paragraph("Matière : " + subjectName + " | Enseignant : " + teacherName, bodyFont));
            doc.add(new Paragraph("Salle : " + room.getCode() + " (" + room.getName() + ") | Date : " + session.getSessionDate() + " (" + session.getStartTime() + " - " + session.getEndTime() + ")", bodyFont));
            doc.add(new Paragraph("Statut validation : " + sheet.sessionStatus() + " (v" + sheet.sheetVersion() + ")", boldFont));
            doc.add(new Paragraph("Totaux — Présents : " + sheet.presentCount() + " | Retards : " + sheet.lateCount() + " | Absents : " + sheet.absentCount(), boldFont));
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.addCell(new Paragraph("Matricule", boldFont));
            table.addCell(new Paragraph("Étudiant", boldFont));
            table.addCell(new Paragraph("Statut", boldFont));
            table.addCell(new Paragraph("Mode", boldFont));

            for (AttendanceDtos.RecordResponse rec : sheet.records()) {
                table.addCell(new Paragraph(rec.matricule(), bodyFont));
                table.addCell(new Paragraph(rec.studentName(), bodyFont));
                table.addCell(new Paragraph(rec.status().name(), bodyFont));
                table.addCell(new Paragraph(rec.mode().name(), bodyFont));
            }
            doc.add(table);
            doc.close();
            return baos.toByteArray();
        } catch (Exception ex) {
            log.error("PDF generation failed for session {}: {}", sessionId, ex.getMessage(), ex);
            throw new BusinessRuleException("PDF_GENERATION_ERROR", "Impossible de générer la feuille de présence en PDF.");
        }
    }
}
