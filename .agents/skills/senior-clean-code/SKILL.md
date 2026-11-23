---
name: senior-clean-code
description: >-
  Use this skill when writing, refactoring, or auditing Java 21 / Spring Boot code in ClassFlow
  to enforce Senior Clean Code standards: Single Responsibility Principle (max 300 lines per class),
  domain-grouped record DTOs, N+1 query prevention, hygiene, and zero dead code.
---

# Senior Clean Code — ClassFlow Backend

Applique cette procédure avant et après toute création ou modification de code dans `src/main/java/com/mosek/classFlow/`. *(L'isolation stricte des 9 couches est vérifiée en complément par `senior-architecture`.)*

## 1. Règles Clean Code à respecter

1. **Anti-prolifération de fichiers & SRP** :
   - Regrouper les DTOs (`record` Java 21) par domaine dans les conteneurs existants de `dto/` (`AuthDtos`, `AcademicDtos`, `RoomDtos`, `ScheduleDtos`, `AttendanceDtos`, `GradeDtos`, `NotificationDtos`, `SyncDtos`) au lieu de créer des dizaines de petits fichiers isolés.
   - Aucune classe ne doit dépasser **300 lignes** : extraire les parseurs/lecteurs de fichiers ou générateurs de documents dans des classes utilitaires package-private dédiées (ex. `TabularFileReader`).
2. **Performance JPA (Zéro N+1)** :
   - Ne jamais exécuter de requête `repository.findBy...` à l'intérieur d'une boucle `for`. Utiliser `findBy...In(Collection<UUID>)` ou une requête d'agrégation.
3. **Hygiène du code** :
   - Zéro `import ...*;` (wildcard import), zéro import inutilisé, zéro DTO mort.
   - Toujours utiliser `Role.ADMIN.name()`, `Role.TEACHER.name()`, etc. au lieu de chaînes littérales `"ADMIN"`, `"TEACHER"`.

## 2. Vérification automatisée

Exécute systématiquement le script d'audit [audit_clean_code.py](./scripts/audit_clean_code.py) après tes modifications :

```bash
python3 .agents/skills/senior-clean-code/scripts/audit_clean_code.py
```

Le script doit retourner `CLEAN CODE AUDIT: PASSED (0 issue)` avant de clore la tâche.
