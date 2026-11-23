# ClassFlow — Plateforme de gestion des présences et suivi académique

Ce dépôt est organisé avec une **séparation stricte** entre le backend et le frontend pour permettre de déplacer ou déployer chacun indépendamment.

## Structure du dépôt

```text
classFlow/
├── pom.xml & src/        # Backend Java 21 / Spring Boot 3 (9 couches strictes)
├── AGENTS.md             # Règles d'architecture et qualité Backend
├── .agents/              # Skills (8) et workflow qualité dédiés au Backend
├── docs/                 # Cahier des charges & Fiche d'intégration API (MD + PDF)
└── frontend/             # Projet Web React + Vite + Tailwind CSS v4 (100 % autonome)
    ├── AGENTS.md         # Règles d'architecture et UI/UX Frontend
    ├── .agents/          # Skills (UI/UX) et workflow qualité dédiés au Frontend
    ├── docs/             # Contrat d'intégration API embarqué
    └── src/              # Code source React
```

## Démarrage rapide

### 1. Backend (port `8080`)
```bash
./mvnw spring-boot:run
```
> En profil `dev` par défaut, 5 comptes de démonstration sont créés automatiquement (ex. `admin@classflow.edu` / `Admin123!`).

### 2. Frontend (port `5173`)
```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

## Déplacer le frontend dans un autre dossier ou dépôt Git
Le dossier `frontend/` ne contient **aucune dépendance fichier** vers la racine. Pour l'extraire :
```bash
mv frontend /chemin/vers/classflow-web
```
Il restera immédiatement compilable et testable (`npm install && npm run audit && npm run dev`).
