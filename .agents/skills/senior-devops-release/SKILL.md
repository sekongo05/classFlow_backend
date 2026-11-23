---
name: senior-devops-release
description: >-
  Use this skill when configuring environments (dev, test, prod), preparing a production release,
  or checking deployment readiness for the ClassFlow backend.
---

# Senior DevOps & Release Readiness — ClassFlow Backend

Active ce skill avant une livraison, une mise en production ou une modification de `pom.xml` / `application*.yml`.

## 1. Checklist Prêt pour la Production (Production Readiness)

1. **Séparation stricte des profils (`dev`, `test`, `prod`)** :
   - `application-prod.yml` ne doit contenir **aucun mot de passe ni URL de base de données en dur** : utiliser exclusivement `${SPRING_DATASOURCE_URL}`, `${SPRING_DATASOURCE_USERNAME}`, `${SPRING_DATASOURCE_PASSWORD}` et `${CLASSFLOW_JWT_SECRET}`.
   - `application-test.yml` utilise H2 en mode PostgreSQL (`MODE=PostgreSQL`) avec `spring.task.scheduling.enabled: false`.
2. **Sonde de santé (`Healthcheck`)** :
   - `GET /api/v1/health` doit rester public dans `SecurityConfig` et répondre `200 OK` avec `"status": "UP"`.
3. **Gate de Release Complet** :
   - Exécuter l'ensemble des audits statiques des skills Senior + la suite de tests Maven avant tout tag ou déploiement.

## 2. Script de Release Gate Global

Exécute le script de vérification DevOps et de Release Gate ([audit_devops.py](./scripts/audit_devops.py)) :

```bash
python3 .agents/skills/senior-devops-release/scripts/audit_devops.py
```

Le script doit afficher `DEVOPS & RELEASE AUDIT: PASSED (0 issue)`.
