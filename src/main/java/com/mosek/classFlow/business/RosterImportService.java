package com.mosek.classFlow.business;

import com.mosek.classFlow.dto.AcademicDtos;
import com.mosek.classFlow.entity.ClassRosterEntry;
import com.mosek.classFlow.entity.RosterImportHistory;
import com.mosek.classFlow.exception.BusinessRuleException;
import com.mosek.classFlow.repository.ClassRosterEntryRepository;
import com.mosek.classFlow.repository.RosterImportHistoryRepository;
import com.mosek.classFlow.security.AuthenticatedUser;
import com.mosek.classFlow.security.SecurityUtils;
import com.mosek.classFlow.transformer.CoreTransformer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RosterImportService {

    private final AcademicService academicService;
    private final ClassRosterEntryRepository rosterRepository;
    private final RosterImportHistoryRepository historyRepository;
    private final CoreTransformer transformer;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public AcademicDtos.RosterPreviewResponse previewRoster(UUID classId, MultipartFile file) {
        AuthenticatedUser actor = SecurityUtils.getCurrentUser();
        academicService.assertCanManageClass(classId, actor);

        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("EMPTY_FILE", "Le fichier importé est vide.");
        }
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "roster.csv";
        List<List<String>> rows = TabularFileReader.readRows(file, fileName);
        if (rows.size() < 2) {
            throw new BusinessRuleException("INVALID_ROSTER_FILE", "Le fichier doit contenir un en-tête et au moins un étudiant.");
        }

        List<AcademicDtos.RosterRow> validRows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> seenMatricules = new HashSet<>();

        for (int i = 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            if (r.stream().allMatch(String::isBlank)) continue;

            String matricule = TabularFileReader.cell(r, 0).toUpperCase(Locale.ROOT);
            String lastName = TabularFileReader.cell(r, 1);
            String firstName = TabularFileReader.cell(r, 2);
            String email = TabularFileReader.cell(r, 3);

            if (matricule.isBlank() || lastName.isBlank() || firstName.isBlank()) {
                errors.add("Ligne " + (i + 1) + " : matricule, nom et prénom sont obligatoires.");
                continue;
            }
            if (!seenMatricules.add(matricule)) {
                errors.add("Ligne " + (i + 1) + " : doublon détecté pour le matricule " + matricule + ".");
                continue;
            }
            validRows.add(new AcademicDtos.RosterRow(matricule, lastName, firstName, email));
        }

        return new AcademicDtos.RosterPreviewResponse(fileName, rows.size() - 1, validRows.size(), validRows, errors);
    }

    @Transactional
    public List<AcademicDtos.RosterEntryResponse> confirmRoster(UUID classId, AcademicDtos.RosterConfirmRequest req) {
        AuthenticatedUser actor = SecurityUtils.getCurrentUser();
        academicService.assertCanManageClass(classId, actor);

        int inserted = 0;
        int updated = 0;
        for (AcademicDtos.RosterRow row : req.rows()) {
            String mat = row.matricule().trim().toUpperCase(Locale.ROOT);
            var existingOpt = rosterRepository.findByAcademicClassIdAndMatriculeIgnoreCase(classId, mat);
            if (existingOpt.isPresent()) {
                ClassRosterEntry e = existingOpt.get();
                e.setLastName(row.lastName().trim());
                e.setFirstName(row.firstName().trim());
                e.setEmail(row.email());
                rosterRepository.save(e);
                updated++;
            } else {
                rosterRepository.save(ClassRosterEntry.builder()
                        .academicClassId(classId)
                        .matricule(mat)
                        .lastName(row.lastName().trim())
                        .firstName(row.firstName().trim())
                        .email(row.email())
                        .build());
                inserted++;
            }
        }

        historyRepository.save(RosterImportHistory.builder()
                .academicClassId(classId)
                .importedBy(actor.id())
                .fileName(req.fileName())
                .importedCount(inserted)
                .updatedCount(updated)
                .build());

        auditService.record("ROSTER_IMPORTED", "AcademicClass", classId.toString(),
                actor.id(), actor.email(), actor.role(), "inserted=" + inserted + ", updated=" + updated);

        return listRoster(classId);
    }

    @Transactional(readOnly = true)
    public List<AcademicDtos.RosterEntryResponse> listRoster(UUID classId) {
        return rosterRepository.findByAcademicClassIdOrderByLastNameAscFirstNameAsc(classId).stream()
                .map(transformer::toRosterEntryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AcademicDtos.RosterImportHistoryResponse> getImportHistory(UUID classId) {
        return historyRepository.findByAcademicClassIdOrderByCreatedAtDesc(classId).stream()
                .map(transformer::toRosterImportHistoryResponse)
                .toList();
    }
}
