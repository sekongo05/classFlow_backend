---
name: senior-api-contract
description: >-
  Use this skill when creating or modifying REST controllers, request/response DTOs, or OpenAPI
  contracts in ClassFlow to ensure French validation messages, proper HTTP status codes, and
  synchronization with the Frontend Integration Guide (MD + PDF).
---

# Senior API Contract & Frontend Sync — ClassFlow Backend

Active ce skill dès qu'un endpoint REST (`controller/`) ou un DTO (`dto/`) est ajouté ou modifié.

## 1. Standards de Contrat API REST

1. **Validation Bean Validation 100 % en français** :
   - Toute contrainte `@NotBlank`, `@NotNull`, `@NotEmpty`, `@Email`, `@Size`, `@Min`, `@Positive`, `@PositiveOrZero` dans `dto/` doit obligatoirement définir un `message = "..."` clair en français (pour que `GlobalExceptionHandler` alimente `fieldErrors` sans message technique par défaut).
   - Tout paramètre `@RequestBody` dans `controller/` correspondant à une requête validée doit porter `@Valid`.
2. **Codes HTTP sémantiques** :
   - Création de ressource (`POST`) : `201 CREATED` (ou `200 OK` pour une action de calcul/transition comme `login`, `refresh`, `open`, `close`, `push`).
   - Suppression ou action sans contenu (`DELETE`, `logout`, `markRead`) : `204 NO_CONTENT`.
3. **Synchronisation obligatoire avec la Fiche d'Intégration Frontend** :
   - Toute modification d'URL, de méthode HTTP, de rôle `@PreAuthorize` ou de structure JSON dans `dto/` doit être répercutée dans `docs/Fiche_Integration_Frontend_ClassFlow.md` (et `docs/Fiche_Integration_Frontend_ClassFlow.pdf`).

## 2. Audit du Contrat API

Exécute le script de vérification des contrats DTO / Contrôleurs / Documentation Frontend ([audit_api_contract.py](./scripts/audit_api_contract.py)) :

```bash
python3 .agents/skills/senior-api-contract/scripts/audit_api_contract.py
```

Le script doit afficher `API CONTRACT AUDIT: PASSED (0 issue)`.
