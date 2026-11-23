package com.mosek.classFlow.business;

import com.mosek.classFlow.dto.AuthDtos;
import com.mosek.classFlow.entity.ClassRosterEntry;
import com.mosek.classFlow.entity.RefreshToken;
import com.mosek.classFlow.entity.Role;
import com.mosek.classFlow.entity.User;
import com.mosek.classFlow.exception.AuthenticationFailedException;
import com.mosek.classFlow.exception.BusinessRuleException;
import com.mosek.classFlow.exception.DuplicateResourceException;
import com.mosek.classFlow.exception.ResourceNotFoundException;
import com.mosek.classFlow.repository.ClassRosterEntryRepository;
import com.mosek.classFlow.repository.RefreshTokenRepository;
import com.mosek.classFlow.repository.UserRepository;
import com.mosek.classFlow.security.AuthenticatedUser;
import com.mosek.classFlow.security.JwtService;
import com.mosek.classFlow.security.SecurityUtils;
import com.mosek.classFlow.transformer.CoreTransformer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ClassRosterEntryRepository rosterEntryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CoreTransformer transformer;
    private final AuditService auditService;

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(email)
                .filter(User::isActive)
                .orElseThrow(() -> new AuthenticationFailedException("Identifiants invalides."));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new AuthenticationFailedException("Identifiants invalides.");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthDtos.AuthResponse registerStudent(AuthDtos.RegisterStudentRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        String matricule = req.matricule().trim().toUpperCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Un compte utilise déjà cette adresse e-mail.");
        }
        if (userRepository.existsByMatriculeIgnoreCase(matricule)) {
            throw new DuplicateResourceException("Un compte existe déjà pour ce matricule.");
        }

        // RM02 & Section 3.3 : rapprochement obligatoire avec la liste officielle importée par le délégué
        ClassRosterEntry rosterEntry = rosterEntryRepository.findByMatriculeIgnoreCase(matricule)
                .orElseThrow(() -> new BusinessRuleException(
                        "MATRICULE_NOT_IN_ROSTER",
                        "Votre matricule ne figure dans aucune liste de classe importée. Contactez votre délégué."
                ));

        User student = userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(req.password()))
                .firstName(req.firstName().trim())
                .lastName(req.lastName().trim())
                .matricule(matricule)
                .role(Role.STUDENT)
                .academicClassId(rosterEntry.getAcademicClassId())
                .active(true)
                .build());

        rosterEntry.setLinkedUserId(student.getId());
        rosterEntryRepository.save(rosterEntry);

        auditService.record("STUDENT_REGISTERED", "User", student.getId().toString(),
                student.getId(), student.getEmail(), student.getRole().name(), "matricule=" + matricule);

        return issueTokens(student);
    }

    @Transactional
    public AuthDtos.AuthResponse refresh(AuthDtos.RefreshTokenRequest req) {
        RefreshToken stored = refreshTokenRepository.findByToken(req.refreshToken())
                .filter(t -> !t.isRevoked() && t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new AuthenticationFailedException("Session expirée. Veuillez vous reconnecter."));

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = userRepository.findById(stored.getUserId())
                .filter(User::isActive)
                .orElseThrow(() -> new AuthenticationFailedException("Compte introuvable ou désactivé."));

        return issueTokens(user);
    }

    @Transactional
    public void logout() {
        AuthenticatedUser current = SecurityUtils.getCurrentUser();
        List<RefreshToken> active = refreshTokenRepository.findByUserIdAndRevokedFalse(current.id());
        active.forEach(t -> t.setRevoked(true));
        refreshTokenRepository.saveAll(active);
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserResponse me() {
        AuthenticatedUser current = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(current.id())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
        return transformer.toUserResponse(user);
    }

    @Transactional
    public AuthDtos.UserResponse createUserByAdmin(AuthDtos.CreateUserRequest req) {
        AuthenticatedUser actor = SecurityUtils.getCurrentUser();
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Un utilisateur existe déjà avec cette adresse e-mail.");
        }
        String matricule = req.matricule() != null && !req.matricule().isBlank()
                ? req.matricule().trim().toUpperCase(Locale.ROOT) : null;
        if (matricule != null && userRepository.existsByMatriculeIgnoreCase(matricule)) {
            throw new DuplicateResourceException("Un utilisateur existe déjà avec ce matricule.");
        }

        User saved = userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(req.password()))
                .firstName(req.firstName().trim())
                .lastName(req.lastName().trim())
                .matricule(matricule)
                .role(req.role())
                .academicClassId(req.academicClassId())
                .active(true)
                .build());

        auditService.record("USER_CREATED", "User", saved.getId().toString(),
                actor.id(), actor.email(), actor.role(), "role=" + saved.getRole());

        return transformer.toUserResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AuthDtos.UserResponse> listUsers() {
        return userRepository.findAll().stream().map(transformer::toUserResponse).toList();
    }

    private AuthDtos.AuthResponse issueTokens(User user) {
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(), user.getEmail(), user.getRole().name(), user.getAcademicClassId()
        );
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshTokenValue = UUID.randomUUID() + "-" + UUID.randomUUID();
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .token(refreshTokenValue)
                .expiresAt(jwtService.computeRefreshExpiry())
                .revoked(false)
                .build());

        return new AuthDtos.AuthResponse(
                accessToken, refreshTokenValue, jwtService.getAccessExpirationSeconds(), transformer.toUserResponse(user)
        );
    }
}
