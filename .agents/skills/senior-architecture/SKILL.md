---
name: senior-architecture
description: >-
  Use this skill when designing, adding, or auditing features and packages in ClassFlow to enforce
  the strict 9-layer architecture, acyclic dependency graph, Flyway schema alignment, and transaction boundaries.
---

# Senior Software Architecture — ClassFlow Backend

Active ce skill avant d'ajouter un nouveau domaine, une nouvelle entité, un nouveau service ou un nouvel endpoint dans `com.mosek.classFlow`.

## 1. Topologie des 9 couches & Matrice de dépendances

Tout fichier Java sous `src/main/java/com/mosek/classFlow/` appartient obligatoirement à l'une des **9 couches** suivantes (hors `ClassFlowApplication.java`). Les dépendances suivent un graphe orienté acyclique (DAG) strict :

```text
controller  ──►  business  ──►  repository  ──►  entity
   │               │  │                            ▲
   │               │  └──────►  transformer ───────┘
   ▼               ▼                │
  dto  ◄───────────┴────────────────┘
```

### Matrice des dépendances internes autorisées (`com.mosek.classFlow.*`)
| Couche | Peut dépendre uniquement de | Interdictions strictes |
| :--- | :--- | :--- |
| **`entity/`** | `entity` | Aucune dépendance vers `dto`, `repository`, `business`, `controller`, `transformer`, `security`, `config` |
| **`repository/`** | `entity`, `repository` | Aucune logique métier, aucun import `dto`, `business`, `controller`, `transformer` |
| **`dto/`** | `dto`, `entity` *(enums uniquement)*, `exception` | Aucun import `repository`, `business`, `controller`, `transformer`, ni `@Entity` JPA |
| **`transformer/`** | `entity`, `dto`, `transformer` | Mapper pur : **jamais** d'injection `repository` ou `business`, jamais de `@Transactional` |
| **`business/`** | `business`, `repository`, `entity`, `dto`, `transformer`, `security`, `exception` | **Jamais** d'import `controller` ou `config`. Seule couche autorisée à porter `@Transactional` |
| **`controller/`** | `business`, `dto`, `security`, `exception` | **Jamais** d'import `entity` ou `repository`, **jamais** de `@Transactional` |
| **`security/`** | `security`, `exception` | Indépendant des repositories et entités |
| **`config/`** | `config`, `security`, `exception` | Configuration Spring pure |
| **`exception/`** | `exception` | Indépendant du métier : utilisable par toutes les couches |

## 2. Procédure standard d'ajout d'une fonctionnalité

1. **Schéma SQL (`db/migration/`)** : ajouter ou vérifier la table et ses index/contraintes dans `V1__init_schema.sql` (ou `V2__...` après mise en production).
2. **Modèle (`entity/` & `repository/`)** : créer l'`@Entity` héritant de `BaseEntity` et son interface `JpaRepository<Entity, UUID>`.
3. **Contrat (`dto/` & `transformer/`)** : ajouter les `record` Request/Response dans le conteneur `*Dtos.java` du domaine concerné et les méthodes de conversion dans `CoreTransformer` ou `DomainTransformer`.
4. **Métier (`business/`)** : implémenter la règle avec `@Transactional`, validation du périmètre utilisateur (`SecurityUtils.getCurrentUser()`), levée de `ClassFlowException` explicites et trace `AuditService` si opération sensible.
5. **Exposition (`controller/`)** : créer l'endpoint REST avec `@Valid`, `@PreAuthorize(...)` et retour `ResponseEntity<...Dto>`.

## 3. Vérification architecturale automatisée

Exécute systématiquement le script de vérification de la matrice d'architecture et d'alignement JPA ↔ Flyway ([audit_architecture.py](./scripts/audit_architecture.py)) :

```bash
python3 .agents/skills/senior-architecture/scripts/audit_architecture.py
```

Le script doit retourner `ARCHITECTURE AUDIT: PASSED (0 violation)`.
