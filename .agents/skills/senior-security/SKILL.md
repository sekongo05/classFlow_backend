---
name: senior-security
description: >-
  Use this skill when auditing or implementing authentication, authorization (RBAC),
  multi-tenant data isolation, anti-fraud rules, or exception handling in ClassFlow.
---

# Senior Security & Error Shielding — ClassFlow Backend

Active ce skill lors de toute modification touchant `security/`, `config/SecurityConfig.java`, `exception/GlobalExceptionHandler.java`, ou un endpoint `controller/`.

## 1. Checklist Sécurité Senior

1. **Protection systématique des endpoints (`@PreAuthorize`)** :
   - Tout endpoint de `controller/` (hors `/api/v1/health`, `/api/v1/auth/login`, `/api/v1/auth/register`, `/api/v1/auth/refresh`) doit être protégé par `@PreAuthorize` au niveau de la classe ou de la méthode.
   - Aucun endpoint de test (`/api/v1/test/**`) ne doit figurer dans `SecurityConfig.java`.
2. **Contrôle de périmètre métier (`IDOR Prevention`)** :
   - Un `@PreAuthorize("hasAnyRole('DELEGATE', ...)")` ne suffit pas : dans `business/`, toujours vérifier que l'utilisateur connecté appartient bien à la classe (`academicService.assertCanManageClass(classId, actor)`) ou est bien l'enseignant assigné à la séance.
   - Pour les notes personnelles (`PersonalGradeService`) et notifications (`NotificationService`), toujours filtrer les requêtes JPA par `SecurityUtils.getCurrentUser().id()`.
3. **Bouclier d'erreurs (`Zero Technical Leak`)** :
   - `GlobalExceptionHandler` ne doit **jamais** renvoyer `ex.getMessage()` au client pour les exceptions techniques (`DataIntegrityViolationException`, `HttpMessageNotReadableException`, `AuthenticationException`, `Exception.class`).
   - `application.yml` doit maintenir `include-stacktrace: never`, `include-message: never`, `include-exception: false`.

## 2. Audit Sécurité automatisé

Exécute le script d'audit sécurité ([audit_security.py](./scripts/audit_security.py)) :

```bash
python3 .agents/skills/senior-security/scripts/audit_security.py
```

Le script doit afficher `SECURITY AUDIT: PASSED (0 vulnerability)` avant validation.
