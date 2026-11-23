package com.mosek.classFlow.business;

import com.mosek.classFlow.dto.AcademicDtos;
import com.mosek.classFlow.entity.AcademicClass;
import com.mosek.classFlow.entity.Role;
import com.mosek.classFlow.entity.Subject;
import com.mosek.classFlow.exception.DuplicateResourceException;
import com.mosek.classFlow.exception.ResourceNotFoundException;
import com.mosek.classFlow.exception.UnauthorizedActionException;
import com.mosek.classFlow.repository.AcademicClassRepository;
import com.mosek.classFlow.repository.SubjectRepository;
import com.mosek.classFlow.repository.UserRepository;
import com.mosek.classFlow.security.AuthenticatedUser;
import com.mosek.classFlow.security.SecurityUtils;
import com.mosek.classFlow.transformer.CoreTransformer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcademicService {

    private final AcademicClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final CoreTransformer transformer;
    private final AuditService auditService;

    @Transactional
    public AcademicDtos.ClassResponse createClass(AcademicDtos.CreateClassRequest req) {
        if (classRepository.existsByNameIgnoreCaseAndAcademicYearIgnoreCase(req.name().trim(), req.academicYear().trim())) {
            throw new DuplicateResourceException("Une classe portant ce nom existe déjà pour cette année académique.");
        }
        AcademicClass saved = classRepository.save(AcademicClass.builder()
                .name(req.name().trim())
                .track(req.track().trim())
                .level(req.level().trim())
                .academicYear(req.academicYear().trim())
                .expectedSize(req.expectedSize())
                .delegateId(req.delegateId())
                .deputyDelegateId(req.deputyDelegateId())
                .build());
        syncDelegateRoles(saved);
        return transformer.toClassResponse(saved);
    }

    @Transactional
    public AcademicDtos.ClassResponse assignDelegates(UUID classId, AcademicDtos.AssignDelegatesRequest req) {
        AcademicClass academicClass = findClassOrThrow(classId);
        academicClass.setDelegateId(req.delegateId());
        academicClass.setDeputyDelegateId(req.deputyDelegateId());
        AcademicClass saved = classRepository.save(academicClass);
        syncDelegateRoles(saved);

        AuthenticatedUser actor = SecurityUtils.getCurrentUser();
        auditService.record("DELEGATES_ASSIGNED", "AcademicClass", saved.getId().toString(),
                actor.id(), actor.email(), actor.role(),
                "delegate=" + req.delegateId() + ", deputy=" + req.deputyDelegateId());

        return transformer.toClassResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AcademicDtos.ClassResponse> listClasses() {
        return classRepository.findAll().stream().map(transformer::toClassResponse).toList();
    }

    @Transactional(readOnly = true)
    public AcademicDtos.ClassResponse getClassById(UUID classId) {
        return transformer.toClassResponse(findClassOrThrow(classId));
    }

    @Transactional
    public AcademicDtos.SubjectResponse createSubject(AcademicDtos.CreateSubjectRequest req) {
        findClassOrThrow(req.academicClassId());
        if (subjectRepository.findByAcademicClassIdAndCodeIgnoreCase(req.academicClassId(), req.code().trim()).isPresent()) {
            throw new DuplicateResourceException("Cette matière existe déjà dans cette classe.");
        }
        Subject saved = subjectRepository.save(Subject.builder()
                .academicClassId(req.academicClassId())
                .code(req.code().trim().toUpperCase())
                .name(req.name().trim())
                .defaultCoefficient(req.defaultCoefficient() != null ? req.defaultCoefficient() : 1.0)
                .teacherId(req.teacherId())
                .build());
        return transformer.toSubjectResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AcademicDtos.SubjectResponse> listSubjects(UUID classId) {
        return subjectRepository.findByAcademicClassIdOrderByNameAsc(classId).stream()
                .map(transformer::toSubjectResponse)
                .toList();
    }

    public AcademicClass findClassOrThrow(UUID classId) {
        return classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe introuvable."));
    }

    public void assertCanManageClass(UUID classId, AuthenticatedUser actor) {
        if (Role.ADMIN.name().equals(actor.role())) {
            return;
        }
        AcademicClass academicClass = findClassOrThrow(classId);
        boolean isDelegate = actor.id().equals(academicClass.getDelegateId())
                || actor.id().equals(academicClass.getDeputyDelegateId());
        if (!isDelegate) {
            throw new UnauthorizedActionException("Vous n'êtes pas autorisé à gérer cette classe.");
        }
    }

    private void syncDelegateRoles(AcademicClass academicClass) {
        if (academicClass.getDelegateId() != null) {
            userRepository.findById(academicClass.getDelegateId()).ifPresent(u -> {
                u.setRole(Role.DELEGATE);
                u.setAcademicClassId(academicClass.getId());
                userRepository.save(u);
            });
        }
        if (academicClass.getDeputyDelegateId() != null) {
            userRepository.findById(academicClass.getDeputyDelegateId()).ifPresent(u -> {
                u.setRole(Role.DEPUTY_DELEGATE);
                u.setAcademicClassId(academicClass.getId());
                userRepository.save(u);
            });
        }
    }
}
