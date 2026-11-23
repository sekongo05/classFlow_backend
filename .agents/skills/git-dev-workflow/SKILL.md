---
name: git-dev-workflow
description: >-
  Use this skill whenever committing or pushing backend changes in ClassFlow to enforce
  working on the dedicated development branch (develop), running pre-commit security/test
  checks, writing commit messages in French, and pushing safely to origin/develop.
---

# Git Dev Workflow — ClassFlow Backend

Active ce skill pour tout commit ou push sur le dépôt backend.

## 1. Branche dédiée au développement (`develop`)
1. Vérifier la branche courante avec `git branch --show-current`.
2. **Ne jamais commiter ni pousser directement sur `main`** sans demande explicite de l'utilisateur.
3. Basculer sur `develop` (ou la créer si elle n'existe pas encore) :
   ```bash
   git checkout develop || git checkout -b develop
   ```

## 2. Contrôles obligatoires avant commit
1. Vérifier `git status --short` : aucun fichier sensible ou généré (`.env`, clés, `.DS_Store`, `target/`, `.idea/`) ne doit être stagé.
2. Exécuter les vérifications du backend :
   ```bash
   python3 .agents/skills/senior-security/scripts/audit_security.py
   python3 .agents/skills/senior-devops-release/scripts/audit_devops.py
   python3 .agents/skills/senior-database-flyway/scripts/audit_database.py
   bash .agents/skills/senior-testing/scripts/run_tests.sh
   ```

## 3. Format des commits (obligatoirement en français)
Rédiger des messages concis en **français** au format Conventional Commits :
- `feat: <nouvelle fonctionnalité en français>`
- `fix: <correction de bug en français>`
- `refactor: <refactorisation en français>`
- `test: <ajout ou mise à jour de tests en français>`
- `docs: <mise à jour documentaire en français>`
- `chore: <maintenance ou configuration en français>`

Exemple : `feat: ajouter la validation hors ligne des feuilles de présence`

## 4. Commit & Push SSH
```bash
git add -A
git commit -m "<type>: <message en français>"
git push -u origin develop
```
