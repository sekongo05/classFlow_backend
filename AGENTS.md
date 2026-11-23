# AGENTS.md — Règles de conduite et Architecture Backend ClassFlow

Ce fichier définit les règles strictes d'architecture, de qualité de code, de sécurité et de gestion des erreurs applicables à tout développement sur le backend **ClassFlow** (Java 21, Spring Boot 3, PostgreSQL 16, Flyway, OpenAPI).

---

## 1. Architecture en 9 couches obligatoire

Tout le code source applicatif sous `src/main/java/com/mosek/classFlow/` doit respecter strictement le découpage en couches suivant :

```text
com.mosek.classFlow
├── entity/       # Entités JPA (@Entity), Embeddables, Enums métier, BaseEntity auditée
├── repository/   # Interfaces Spring Data JPA uniquement (aucune logique métier)
├── dto/          # Objets de transfert (request / response / events), immutables (Java records recommandés)
├── transformer/  # Mappers dédiés Entity <-> DTO (aucune logique métier ni appel repository)
├── business/     # Services métier (@Service, @Transactional) portant les règles RM01-RM20
├── controller/   # Contrôleurs REST (@RestController) et WebSocket (STOMP) uniquement
├── config/       # Configurations Spring (Security, OpenAPI, WebSocket, CORS, JPA Auditing)
├── security/     # Filtres JWT (JwtAuthenticationFilter), AuthenticatedUser, SecurityUtils
└── exception/    # Exceptions métier (ClassFlowException) et GlobalExceptionHandler (@RestControllerAdvice)
```

### Règles strictes d'isolation entre couches
1. **Jamais d'entité JPA exposée dans un `controller/`** : toute entrée/sortie passe obligatoirement par un `dto/` converti via un `transformer/`.
2. **Jamais de logique métier dans un `controller/`** : un contrôleur valide l'entrée (`@Valid`), délègue à `business/`, et retourne la réponse HTTP adaptée (`ResponseEntity`).
3. **Jamais d'accès direct à un `repository/` depuis un `controller/`** : le passage par `business/` est obligatoire.
4. **Jamais d'accès `repository/` ou `business/` dans `transformer/`** : un transformer est un mapper pur.
5. **Transactions** : les annotations `@Transactional` (et `@Transactional(readOnly = true)` pour les lectures) vivent exclusivement dans `business/`.
6. **Migrations & Contrats** : le schéma initial consolidé est dans `src/main/resources/db/migration/V1__init_schema.sql` ; toute nouvelle évolution après mise en production passe par un script Flyway versionné (`V2__...`) ; tout endpoint REST est documenté via **OpenAPI**.

---

## 2. Gestion centralisée & interprétation des erreurs (RÈGLE CRITIQUE)

> **Principe absolu : Ne JAMAIS renvoyer de messages techniques, de stack traces, de noms de classes Java, ni de messages SQL/Hibernate bruts aux clients.**

### 2.1. Centralisation dans `exception/`
- Toutes les erreurs métier héritent de `ClassFlowException` (portant `HttpStatus`, un `errorCode` applicatif stable, et un message fonctionnel clair en français).
- Exceptions métier standards à utiliser dans `business/` :
  - `ResourceNotFoundException` (`404 NOT_FOUND`)
  - `DuplicateResourceException` (`409 CONFLICT`)
  - `BusinessRuleException` (`422 UNPROCESSABLE_ENTITY` ou `400 BAD_REQUEST`)
  - `UnauthorizedActionException` (`403 FORBIDDEN`)
  - `AuthenticationFailedException` (`401 UNAUTHORIZED`)
  - `NetworkVerificationException` (`403 FORBIDDEN` — QR Code, Wi-Fi de la salle ou preuve réseau invalide)
- Un unique `@RestControllerAdvice` (`GlobalExceptionHandler`) intercepte **100 % des exceptions** :
  1. Toutes les `ClassFlowException` → renvoie le statut et le message métier interprété.
  2. `MethodArgumentNotValidException` / `ConstraintViolationException` → interprète chaque champ invalide en message lisible (`fieldErrors`).
  3. `HttpMessageNotReadableException` / `MethodArgumentTypeMismatchException` → *"Le format des données envoyées est invalide."*
  4. `DataIntegrityViolationException` → interprète les contraintes (doublon d'unicité, dépendance existante) sans jamais exposer le SQL.
  5. `AuthenticationException` / `BadCredentialsException` / `JwtException` → *"Identifiants invalides ou session expirée."*
  6. `AccessDeniedException` → *"Vous n'avez pas les droits nécessaires pour effectuer cette action."*
  7. `MaxUploadSizeExceededException` → *"Le fichier dépasse la taille maximale autorisée."*
  8. `NoResourceFoundException` / `HttpRequestMethodNotSupportedException` → message HTTP clair en français.
  9. **Fallback `Exception.class`** → log complet côté serveur avec `traceId` (UUID) pour le débogage, et renvoie au client uniquement : *"Une erreur inattendue est survenue. Veuillez réessayer plus tard."* (avec le `traceId`, sans aucun détail technique).

### 2.2. Format de réponse d'erreur uniforme (`ApiErrorResponse`)
```json
{
  "timestamp": "2026-11-16T12:00:00Z",
  "status": 400,
  "errorCode": "ATTENDANCE_ALREADY_RECORDED",
  "message": "Votre présence a déjà été enregistrée pour cette séance.",
  "path": "/api/v1/attendance/check-in",
  "traceId": "a1b2c3d4-...",
  "fieldErrors": {}
}
```
- `server.error.include-stacktrace=never`, `server.error.include-message=never`, `server.error.include-exception=false`.

---

## 3. Sécurité, Gouvernance & Règles métier ClassFlow v1.0 (RM01–RM20)

- **Authentification** : JWT stateless (Access Token 15 min + Refresh Token rotatif en base), mots de passe hashés via `BCryptPasswordEncoder`.
- **RBAC (5 rôles)** : `ADMIN`, `DELEGATE`, `DEPUTY_DELEGATE`, `TEACHER`, `STUDENT`. Vérifier systématiquement les permissions via `@PreAuthorize` et le périmètre d'appartenance dans `business/`.
- **Rapprochement & Unicité (RM01–RM07)** : rapprochement obligatoire du matricule étudiant avec la liste officielle importée par le délégué, QR Code permanent par salle, vérification du réseau local Wi-Fi de la salle (RM06), un seul pointage par étudiant et par séance (RM07).
- **Cycle de vie & Validation (RM08–RM15, RM20)** : ouverture/clôture automatique des séances, notifications ciblées par classe, déclaration anticipée d'absence enseignant, historisation obligatoire des corrections (motif + auteur + date), versioning des feuilles de présence imposant une nouvelle validation après correction, validation enseignant hors Wi-Fi de salle.
- **Confidentialité & Hors ligne (RM16–RM19)** : notes personnelles privées filtrées strictement par `currentUser.id()` avec calcul automatique des moyennes pondérées ; synchronisation hors ligne idempotente via `clientOperationId` (UUID) et mode secours `EMERGENCY` en cas de coupure d'électricité.

---

## 4. Qualité de code & Vérification continue

- **Java 21** : privilégier les `record` pour les DTOs, les `enum` pour les statuts et `Optional` pour les recherches.
- **Single Responsibility Principle (SRP)** : éviter les God classes ; extraire les parseurs, builders de documents et processeurs dédiés.
- **Aucun endpoint de test en production** : `SecurityConfig` ne doit jamais autoriser `/api/v1/test/**`.
- **Validation continue** : après chaque modification importante, exécuter `bash .agents/skills/senior-testing/scripts/run_tests.sh` ou lancer le workflow de qualité `.agents/workflows/quality_check_backend.py`.
