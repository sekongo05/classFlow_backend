# Fiche d'Intégration Frontend — Backend ClassFlow v1.0

Cette fiche constitue le contrat d'intégration officiel entre l'application **Frontend (Flutter Mobile / React Web)** et le backend **ClassFlow v1.0**.

---

## 1. Configuration générale & Accès API

- **Base URL REST** : `http://<host>:8080/api/v1`
- **Documentation OpenAPI 3 / Swagger UI** :
  - Interface interactive : `/swagger-ui.html`
  - Spécification JSON (génération automatique des modèles Dart / TypeScript) : `/v3/api-docs`
- **Healthcheck (public)** : `GET /api/v1/health` → `{"status":"UP","service":"classFlow","timestamp":"..."}`
- **Authentification** : Header HTTP `Authorization: Bearer <accessToken>` sur tous les endpoints protégés.
- **Rôles RBAC (5 rôles)** :
  - `ADMIN` : Administration globale (utilisateurs, classes, matières, délégués, salles)
  - `DELEGATE` : Délégué principal de classe
  - `DEPUTY_DELEGATE` : Délégué adjoint (mêmes permissions opérationnelles que le délégué, actions tracées)
  - `TEACHER` : Enseignant (consultation séances, validation/contestation des feuilles hors Wi-Fi de salle)
  - `STUDENT` : Étudiant (pointage QR Code, emploi du temps, notes personnelles, historique)

---

## 2. Format uniforme des erreurs (`ApiErrorResponse`)

Toutes les erreurs (métier, validation `@Valid`, JSON malformé, contrainte SQL, sécurité `401`/`403`, erreur serveur `500`) renvoient **toujours** la même structure JSON, sans jamais exposer de message technique ou stack trace :

```json
{
  "timestamp": "2026-11-19T18:00:00Z",
  "status": 422,
  "errorCode": "MATRICULE_NOT_IN_ROSTER",
  "message": "Votre matricule ne figure dans aucune liste de classe importée. Contactez votre délégué.",
  "path": "/api/v1/auth/register",
  "traceId": "443edd4d-0be2-4a98-b531-1ff2d700e031",
  "fieldErrors": {}
}
```

### Règles de gestion côté Frontend
1. **Affichage direct** : afficher `error.message` directement à l'utilisateur (déjà rédigé en français clair).
2. **Erreurs de formulaire (`400`, `errorCode == "VALIDATION_ERROR"`)** : parcourir `fieldErrors` (`{ "email": "Le format de l'adresse e-mail est invalide." }`) pour afficher le message sous chaque champ.
3. **Expiration de session (`401`, `errorCode == "AUTHENTICATION_FAILED"`)** : appeler `POST /api/v1/auth/refresh` avec le `refreshToken` ; si le refresh échoue, rediriger vers l'écran de connexion.
4. **Support (`500`, `errorCode == "INTERNAL_ERROR"`)** : afficher `message` + `traceId`.

### Principaux codes d'erreur métier (`errorCode`)
| `errorCode` | HTTP | Signification |
| :--- | :---: | :--- |
| `AUTHENTICATION_FAILED` | `401` | Identifiants invalides ou token expiré |
| `FORBIDDEN` | `403` | Droits insuffisants pour le rôle ou la classe |
| `NETWORK_VERIFICATION_FAILED` | `403` | QR Code invalide, mauvais Wi-Fi (`wifiSsid`) ou preuve réseau (`networkProof`) incorrecte |
| `RESOURCE_NOT_FOUND` | `404` | Ressource introuvable |
| `DUPLICATE_RESOURCE` | `409` | Doublon (ex. double pointage sur la même séance, e-mail/matricule déjà inscrit) |
| `MATRICULE_NOT_IN_ROSTER` | `422` | Matricule absent de la liste officielle importée par le délégué |
| `SESSION_NOT_OPEN` | `422` | Tentative de pointage sur une séance qui n'est pas `IN_PROGRESS` |
| `CHECKIN_WINDOW_EXPIRED` | `422` | Délai de pointage (> 1h après le début du cours) dépassé |
| `COMMENT_REQUIRED` | `422` | Commentaire manquant lors du signalement d'une anomalie par l'enseignant |

---

## 3. Authentification & Utilisateurs (`AuthController`, `UserController`)

### 3.1. Endpoints d'authentification (`/api/v1/auth`)
| Méthode | Endpoint | Accès | Payload Requête | Réponse |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/login` | Public | `{ "email", "password" }` | `AuthResponse` (`200`) |
| `POST` | `/api/v1/auth/register` | Public | `{ "matricule", "email", "password" (min 8), "firstName", "lastName" }` | `AuthResponse` (`201`) |
| `POST` | `/api/v1/auth/refresh` | Public | `{ "refreshToken" }` | `AuthResponse` (`200`) |
| `POST` | `/api/v1/auth/logout` | Authentifié | *(vide)* | `204 No Content` |
| `GET` | `/api/v1/auth/me` | Authentifié | — | `UserResponse` (`200`) |

**Exemple `AuthResponse` :**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "f47ac10b-58cc-4372-a567-0e02b2c3d479-...",
  "expiresInSeconds": 900,
  "user": {
    "id": "b3b8c7d2-...",
    "email": "jean@etu.edu",
    "firstName": "Jean",
    "lastName": "Kouassi",
    "matricule": "ESATIC001",
    "role": "STUDENT",
    "academicClassId": "a1b2c3d4-...",
    "active": true
  }
}
```

### 3.2. Gestion des comptes par l'Administrateur (`/api/v1/admin/users`)
- `POST /api/v1/admin/users` (`ADMIN`) :
  ```json
  {
    "email": "prof@classflow.edu",
    "password": "Password123!",
    "firstName": "Jean",
    "lastName": "Dupont",
    "matricule": null,
    "role": "TEACHER",
    "academicClassId": null
  }
  ```
- `GET /api/v1/admin/users` (`ADMIN`) → `List<UserResponse>`

---

## 4. Classes, Matières, Listes d'étudiants & Salles

### 4.1. Classes & Matières (`/api/v1/classes`)
| Méthode | Endpoint | Rôles | Payload / Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/classes` | `ADMIN` | `{ "name", "track", "level", "academicYear", "expectedSize", "delegateId", "deputyDelegateId" }` |
| `GET` | `/api/v1/classes` | Authentifié | Liste toutes les classes (`List<ClassResponse>`) |
| `GET` | `/api/v1/classes/{classId}` | Authentifié | Détail d'une classe (`ClassResponse`) |
| `PUT` | `/api/v1/classes/{classId}/delegates` | `ADMIN` | `{ "delegateId", "deputyDelegateId" }` — met aussi à jour le rôle des utilisateurs désignés |
| `POST` | `/api/v1/classes/subjects` | `ADMIN` | `{ "academicClassId", "code", "name", "defaultCoefficient", "teacherId" }` |
| `GET` | `/api/v1/classes/{classId}/subjects` | Authentifié | Liste les matières de la classe (`List<SubjectResponse>`) |

### 4.2. Importation des listes de classe en 2 étapes (`/api/v1/classes/{classId}/roster`)
1. **Étape 1 — Aperçu & détection des doublons** :
   - `POST /api/v1/classes/{classId}/roster/preview` (`ADMIN`, `DELEGATE`, `DEPUTY_DELEGATE`)
   - `Content-Type: multipart/form-data`, champ `file` (`.csv` ou `.xlsx`, colonnes : `Matricule | Nom | Prénoms | Email`).
   - Retourne `RosterPreviewResponse` : `{ "fileName", "totalRows", "validCount", "validRows": [...], "errors": [...] }`.
2. **Étape 2 — Confirmation de l'import** :
   - `POST /api/v1/classes/{classId}/roster/confirm` (`ADMIN`, `DELEGATE`, `DEPUTY_DELEGATE`)
   - Body JSON : `{ "fileName": "liste.csv", "rows": [ { "matricule": "ESATIC001", "lastName": "KOUASSI", "firstName": "Jean", "email": "jean@etu.edu" } ] }`
3. **Consultation & Historique** :
   - `GET /api/v1/classes/{classId}/roster` (`ADMIN`, `DELEGATE`, `DEPUTY_DELEGATE`, `TEACHER`) → `List<RosterEntryResponse>`
   - `GET /api/v1/classes/{classId}/roster/history` (`ADMIN`, `DELEGATE`, `DEPUTY_DELEGATE`) → `List<RosterImportHistoryResponse>`

### 4.3. Salles & QR Codes permanents (`/api/v1/rooms`)
- `POST /api/v1/rooms` (`ADMIN`) :
  ```json
  {
    "code": "A204",
    "name": "Salle A204",
    "wifiSsid": "WIFI-A204",
    "networkSecret": "SECRET-A204",
    "operational": true
  }
  ```
  Retourne `RoomResponse` incluant le `qrCodeToken` permanent généré (`QR-A204-...`).
- `GET /api/v1/rooms` & `GET /api/v1/rooms/{roomId}` (Authentifié).

---

## 5. Emplois du temps, Séances & Absences Enseignants (`ScheduleController`)

### 5.1. Créneaux d'emploi du temps
- `POST /api/v1/classes/{classId}/timetable/slots` (`ADMIN`, `DELEGATE`, `DEPUTY_DELEGATE`) :
  ```json
  {
    "subjectId": "uuid",
    "teacherId": "uuid",
    "roomId": "uuid",
    "dayOfWeek": "MONDAY",
    "specificDate": null,
    "startTime": "07:30",
    "endTime": "10:00"
  }
  ```
- `GET /api/v1/classes/{classId}/timetable/slots` (Authentifié)
- `DELETE /api/v1/classes/{classId}/timetable/slots/{slotId}` (`ADMIN`, `DELEGATE`, `DEPUTY_DELEGATE`)

### 5.2. Génération & Cycle de vie des séances
- **États d'une séance (`SessionStatus`)** :
  `SCHEDULED` → `IN_PROGRESS` → `PENDING_VALIDATION` → `VALIDATED` / `DISPUTED` (ou `CANCELLED` / `POSTPONED`).
- `POST /api/v1/classes/{classId}/sessions/generate` (`ADMIN`, `DELEGATE`, `DEPUTY_DELEGATE`) :
  `{ "fromDate": "2026-11-16", "toDate": "2026-11-21" }` → génère automatiquement les séances à partir de l'emploi du temps.
- `POST /api/v1/classes/{classId}/sessions` (`ADMIN`, `DELEGATE`, `DEPUTY_DELEGATE`) : ajout manuel d'une séance exceptionnelle.
- `GET /api/v1/classes/{classId}/sessions` (Authentifié) : séances d'une classe.
- `GET /api/v1/sessions/my` (Authentifié) : séances de l'étudiant/délégué (sa classe) ou de l'enseignant connecté.
- `PATCH /api/v1/sessions/{sessionId}` (`ADMIN`, `DELEGATE`, `DEPUTY_DELEGATE`) : modifier salle, enseignant, date, horaire ou notes (notifie automatiquement la classe).
- `POST /api/v1/sessions/{sessionId}/open` (`ADMIN`, `DELEGATE`, `DEPUTY_DELEGATE`) : ouvre manuellement le pointage (`IN_PROGRESS`). *(Un scheduler ouvre et clôture aussi automatiquement les séances selon leurs horaires.)*

### 5.3. Déclaration anticipée d'absence enseignant
- `POST /api/v1/sessions/{sessionId}/teacher-absences` (`ADMIN`, `DELEGATE`, `DEPUTY_DELEGATE`) :
  ```json
  {
    "declarationType": "TEACHER_ABSENCE",
    "newDate": null,
    "newStartTime": null,
    "newEndTime": null,
    "replacementTeacherId": null,
    "comment": "L'enseignant a prévenu de son indisponibilité."
  }
  ```
  Valeurs de `declarationType` : `TEACHER_ABSENCE`, `CANCELLATION`, `POSTPONEMENT`, `TEACHER_CHANGE`.
- `POST /api/v1/sessions/{sessionId}/teacher-absences/{declarationId}/cancel` : annule la déclaration et réactive la séance (`SCHEDULED`).

---

## 6. Pointage QR Code, Présences Temps Réel, PDF & Statistiques (`AttendanceController`)

### 6.1. Pointage étudiant (`POST /api/v1/attendance/check-in`)
- **Rôles** : `STUDENT`, `DELEGATE`, `DEPUTY_DELEGATE`.
- **Requête (`CheckInRequest`)** :
  ```json
  {
    "courseSessionId": "uuid",
    "qrCodeToken": "QR-A204-8F3C1A2B",
    "wifiSsid": "WIFI-A204",
    "networkProof": "SECRET-A204"
  }
  ```
- **Règles appliquées par le serveur** :
  - Un seul pointage par étudiant et par séance (`409 DUPLICATE_RESOURCE` sinon).
  - Statut calculé automatiquement : `PRESENT` (dans le délai de tolérance, 15 min par défaut), `LATE` (entre 15 min et 1h), refusé au-delà d'1h (`422 CHECKIN_WINDOW_EXPIRED`).
  - Si le mode secours (`emergencyMode`) est actif sur la séance, l'enregistrement est marqué `mode = "EMERGENCY"`.

### 6.2. Suivi Temps Réel (WebSocket STOMP)
- **Endpoint WebSocket** : `ws://<host>:8080/ws` (compatible STOMP natif et SockJS).
- **Topic d'abonnement côté Délégué** : `/topic/sessions/{sessionId}/attendance`
- **Payload reçu à chaque pointage ou correction (`LiveAttendanceEvent`)** :
  ```json
  {
    "eventType": "CHECK_IN",
    "courseSessionId": "uuid",
    "presentCount": 18,
    "lateCount": 2,
    "absentCount": 0,
    "record": {
      "id": "uuid",
      "courseSessionId": "uuid",
      "studentId": "uuid",
      "matricule": "ESATIC001",
      "studentName": "Kouassi Jean",
      "status": "PRESENT",
      "mode": "NORMAL",
      "checkedInAt": "2026-11-19T08:05:00Z",
      "regularized": false
    }
  }
  ```

### 6.3. Gestion de la feuille, Corrections, Mode Secours & Export PDF
| Méthode | Endpoint | Rôles | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/attendance/sessions/{sessionId}/sheet` | `ADMIN`, `DELEGATE`, `DEPUTY_DELEGATE`, `TEACHER` | Feuille complète + compteurs + liste des pointages (`SheetResponse`) |
| `PATCH` | `/api/v1/attendance/records/{recordId}` | `ADMIN`, `DELEGATE`, `DEPUTY_DELEGATE` | Correction justifiée : `{ "newStatus": "LATE", "reason": "Motif obligatoire" }`. Si la feuille était déjà validée/contestée, incrémente `sheetVersion` et repasse la séance à `PENDING_VALIDATION`. |
| `GET` | `/api/v1/attendance/sessions/{sessionId}/corrections` | `ADMIN`, `DELEGATE`, `DEPUTY_DELEGATE`, `TEACHER` | Historique des corrections de la séance (`List<CorrectionResponse>`) |
| `POST` | `/api/v1/attendance/sessions/{sessionId}/close` | `ADMIN`, `DELEGATE`, `DEPUTY_DELEGATE` | Clôture la séance, marque `ABSENT` les étudiants inscrits non pointés, et notifie l'enseignant |
| `POST` | `/api/v1/attendance/sessions/{sessionId}/emergency-mode?enabled=true` | `ADMIN`, `DELEGATE`, `DEPUTY_DELEGATE` | Active/désactive la procédure de secours (coupure électrique) |
| `GET` | `/api/v1/attendance/sessions/{sessionId}/sheet/pdf` | `ADMIN`, `DELEGATE`, `DEPUTY_DELEGATE`, `TEACHER` | Télécharge la feuille de présence officielle en PDF (`application/pdf`) |
| `GET` | `/api/v1/attendance/me` | `STUDENT`, `DELEGATE`, `DEPUTY_DELEGATE` | Historique personnel de présence de l'étudiant connecté |
| `GET` | `/api/v1/attendance/stats/me` | `STUDENT`, `DELEGATE`, `DEPUTY_DELEGATE` | Statistiques individuelles (`StudentStatsResponse`) |
| `GET` | `/api/v1/attendance/stats/classes/{classId}` | `ADMIN`, `DELEGATE`, `DEPUTY_DELEGATE`, `TEACHER` | Statistiques globales de la classe (`ClassStatsResponse`) |

---

## 7. Validation des Feuilles par les Enseignants (`SheetValidationController`)

Accessible depuis n'importe quel réseau (aucune obligation d'être sur le Wi-Fi de la salle, `RM20`).

- `POST /api/v1/validations/sessions/{sessionId}` (`TEACHER`, `ADMIN`) :
  ```json
  {
    "decision": "VALIDATED",
    "comment": "RAS"
  }
  ```
  - `decision` : `VALIDATED` (confirme la feuille) ou `DISPUTED` (signale une anomalie — `comment` obligatoire).
- `GET /api/v1/validations/sessions/{sessionId}/history` (`TEACHER`, `ADMIN`, `DELEGATE`, `DEPUTY_DELEGATE`) → historique des validations/contestations de la séance.
- `GET /api/v1/validations/me` (`TEACHER`, `ADMIN`) → historique des validations de l'enseignant connecté.

---

## 8. Notes Personnelles Privées (`GradeController` — `/api/v1/grades`)

Espace strictement privé de chaque étudiant (`STUDENT`, `DELEGATE`, `DEPUTY_DELEGATE`), filtré par `currentUser.id()`.

- `POST /api/v1/grades/evaluations` :
  ```json
  {
    "subjectName": "Programmation Java",
    "evaluationType": "EXAM",
    "title": " Examen Partiel",
    "score": 16.0,
    "maxScore": 20.0,
    "coefficient": 2.0,
    "semester": "S1",
    "academicYear": "2026-2027",
    "evaluationDate": "2026-11-15"
  }
  ```
  Types acceptés (`EvaluationType`) : `HOMEWORK`, `EXAM`, `LAB`, `PROJECT`, `OTHER`.
- `PUT /api/v1/grades/evaluations/{evaluationId}` : modifier une note.
- `DELETE /api/v1/grades/evaluations/{evaluationId}` : supprimer une note (`204`).
- `GET /api/v1/grades/summary` : retourne `GradesSummaryResponse` avec le calcul automatique des moyennes pondérées :
  - `overallAverage`, `totalEvaluations`, `bestSubject`, `bestSubjectAverage`,
  - `averageBySubject`, `averageBySemester`, `averageByYear`, `evaluations`.

---

## 9. Notifications (`NotificationController` — `/api/v1/notifications`)

- `GET /api/v1/notifications` : liste des notifications de l'utilisateur connecté.
- `GET /api/v1/notifications/unread-count` → `{ "unreadCount": 2 }`.
- `PATCH /api/v1/notifications/{notificationId}/read` : marquer comme lue (`204`).
- `POST /api/v1/notifications/device-tokens` : `{ "token": "fcm-token...", "platform": "ANDROID" }`.
- `DELETE /api/v1/notifications/device-tokens?token=fcm-token...`

---

## 10. Synchronisation Hors Ligne (`SyncController` — `/api/v1/sync`)

- **`POST /api/v1/sync/push`** : envoie les opérations enregistrées hors ligne dans SQLite. L'idempotence est garantie par `clientOperationId` (UUID généré côté mobile).
  ```json
  {
    "operations": [
      {
        "clientOperationId": "c9a646d3-9c61-4cb7-bfcd-32522c8f633a",
        "operationType": "OFFLINE_EVALUATION",
        "courseSessionId": null,
        "payload": "{\"subjectName\":\"Java\",\"evaluationType\":\"HOMEWORK\",\"title\":\"TP2\",\"score\":15,\"maxScore\":20,\"coefficient\":1,\"evaluationDate\":\"2026-11-18\"}"
      }
    ]
  }
  ```
  - Types `operationType` supportés : `OFFLINE_EVALUATION`, `OFFLINE_VALIDATION`, `OFFLINE_ABSENCE_DECLARATION`.
  - Statuts retournés par opération : `APPLIED`, `DUPLICATE_IGNORED` (si déjà reçu), `CONFLICT`.
- **`GET /api/v1/sync/pull`** : récupère `{ "serverTime", "sessions", "notifications", "evaluations" }` pour mettre à jour le cache SQLite local.
