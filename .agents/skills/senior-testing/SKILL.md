---
name: senior-testing
description: >-
  Use this skill when writing or running unit and integration tests in ClassFlow to ensure
  end-to-end coverage of business rules (RM01-RM20), role permissions, edge cases, and
  error response contracts without technical leaks.
---

# Senior Testing Strategy — ClassFlow Backend

Utilise ce skill pour concevoir, enrichir et exécuter les tests du backend ClassFlow.

## 1. Standards de test Senior

1. **Double couverture par fonctionnalité** :
   - **Cas nominal** : vérifie le statut HTTP (`200`/`201`/`204`), le payload JSON retourné et l'effet de bord en base.
   - **Cas d'erreur métier & sécurité** : vérifie systématiquement le rejet (`400`, `401`, `403`, `404`, `409`, `422`), la présence de `errorCode` et `traceId` dans `ApiErrorResponse`, et l'absence de noms d'exceptions Java (`Exception`, `Jackson`, `Hibernate`, codes SQL) dans le corps de réponse.
2. **Règles métier critiques à toujours couvrir (RM01–RM20)** :
   - Rapprochement matricule obligatoire à l'inscription étudiante (`422 MATRICULE_NOT_IN_ROSTER` si absent de la liste).
   - Vérification du Wi-Fi et du QR Code permanent de la salle au pointage (`403 NETWORK_VERIFICATION_FAILED`).
   - Interdiction du double pointage sur une même séance (`409 DUPLICATE_RESOURCE`).
   - Incrémentation de `sheetVersion` et retour à `PENDING_VALIDATION` lorsqu'un délégué corrige une feuille déjà validée ou contestée.
   - Isolation stricte des notes personnelles par étudiant et exactitude de la moyenne pondérée.
   - Idempotence de `/api/v1/sync/push` (`APPLIED` au 1er envoi, `DUPLICATE_IGNORED` au 2e envoi avec le même `clientOperationId`).
3. **Configuration de test rapide et déterministe** :
   - Les tests tournent sous `@ActiveProfiles("test")` (H2 en mémoire en mode PostgreSQL, scheduler désactivé via `spring.task.scheduling.enabled=false`).

## 2. Exécution de la suite de tests

Lance le script d'exécution Maven ([run_tests.sh](./scripts/run_tests.sh)) qui sélectionne automatiquement un JDK 21+ si nécessaire :

```bash
bash .agents/skills/senior-testing/scripts/run_tests.sh
```
