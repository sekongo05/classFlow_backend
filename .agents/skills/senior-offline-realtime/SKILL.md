---
name: senior-offline-realtime
description: >-
  Use this skill when modifying attendance check-in, sheet corrections, teacher validations,
  WebSocket live broadcasts, emergency power-outage mode, or offline synchronization in ClassFlow.
---

# Senior Offline Sync & Real-Time Attendance — ClassFlow Backend

Active ce skill pour toute modification touchant `AttendanceService`, `SheetValidationService`, `SyncService` ou `WebSocketConfig`.

## 1. Invariants Métier Critiques (Sections 7 à 14 du Cahier des charges)

1. **Anti-fraude & Unicité du pointage (RM06–RM07)** :
   - Le pointage (`checkIn`) exige une séance à l'état `IN_PROGRESS`, l'appartenance de l'étudiant à la classe, et l'unicité `(courseSessionId, studentId)`.
   - Hors mode secours (`emergencyMode == false`), `RoomService.verifyRoomNetwork(...)` doit valider le `qrCodeToken`, le `wifiSsid` et la preuve réseau de la salle.
   - En mode secours (`emergencyMode == true`), le pointage est enregistré avec `AttendanceMode.EMERGENCY` et le booléen `containsEmergencyRecords` de la feuille est activé.
2. **Diffusion Temps Réel WebSocket STOMP (Section 8.2)** :
   - Chaque pointage (`CHECK_IN`) et chaque correction (`CORRECTION`) doit recalculer les compteurs (`presentCount`, `lateCount`, `absentCount`) et diffuser un `LiveAttendanceEvent` sur `/topic/sessions/{sessionId}/attendance` sans faire échouer la transaction HTTP si aucun client WebSocket n'écoute.
3. **Versioning des feuilles de présence (Section 9.3)** :
   - Toute correction effectuée par un délégué sur une séance déjà `VALIDATED` ou `DISPUTED` doit incrémenter `sheetVersion` (`sheetVersion + 1`), réinitialiser `validatedAt`/`validatedBy`, repasser la séance à `PENDING_VALIDATION` et notifier l'enseignant.
4. **Idempotence Hors Ligne (Section 13.3)** :
   - `SyncService.push` doit vérifier `syncLogRepository.findByClientOperationId(op.clientOperationId())` avant toute exécution et retourner `DUPLICATE_IGNORED` si l'UUID a déjà été traité.

## 2. Audit Offline & Temps Réel

Exécute le script de vérification des invariants ([audit_offline_realtime.py](./scripts/audit_offline_realtime.py)) :

```bash
python3 .agents/skills/senior-offline-realtime/scripts/audit_offline_realtime.py
```

Le script doit afficher `OFFLINE & REALTIME AUDIT: PASSED (0 issue)`.
