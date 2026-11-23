---
name: senior-database-flyway
description: >-
  Use this skill when creating or modifying JPA entities, repositories, or Flyway SQL migrations
  in ClassFlow to enforce PostgreSQL best practices, EnumType.STRING, UTC timestamps, indexing,
  and schema validation.
---

# Senior Database & Flyway — ClassFlow Backend

Active ce skill lors de toute évolution du modèle de données (`entity/`, `repository/`, `src/main/resources/db/migration/`).

## 1. Standards Base de Données & JPA

1. **Clés primaires & Types** :
   - Toutes les entités métier héritent de `BaseEntity` (`UUID` généré, `createdAt` et `updatedAt` en `Instant` UTC → `TIMESTAMP WITH TIME ZONE` sous PostgreSQL).
   - **Jamais** de `@Enumerated` sans `(EnumType.STRING)` : le mode `ORDINAL` par défaut est strictement interdit.
2. **Configuration Hibernate / Flyway** :
   - `spring.jpa.open-in-view: false` obligatoire dans `application.yml` pour éviter les requêtes lazy hors transaction.
   - `spring.jpa.hibernate.ddl-auto: validate` obligatoire en `dev` et `prod` : seul Flyway gère le DDL.
3. **Migrations Flyway & Indexation** :
   - Tant que la v1.0 est en développement initial, le schéma consolidé vit dans `V1__init_schema.sql`. Après la première mise en production, toute modification passe par un nouveau fichier `V2__<description>.sql` (ne jamais modifier une migration déjà appliquée en production).
   - Toute colonne fréquemment filtrée (`academic_class_id`, `course_session_id`, `student_id`, `teacher_id`, `recipient_id`) doit posséder un index explicite (`CREATE INDEX idx_...`).

## 2. Audit Base de données automatisé

Exécute le script de vérification JPA / SQL ([audit_database.py](./scripts/audit_database.py)) :

```bash
python3 .agents/skills/senior-database-flyway/scripts/audit_database.py
```

Le script doit afficher `DATABASE & FLYWAY AUDIT: PASSED (0 issue)`.
