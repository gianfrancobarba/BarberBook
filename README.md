# BarberBook 🪒

[![CI Backend](https://github.com/gianfrancobarba/BarberBook/actions/workflows/ci-backend.yml/badge.svg)](https://github.com/gianfrancobarba/BarberBook/actions/workflows/ci-backend.yml)

Sistema gestionale per prenotazioni barber shop — **Hair Man Tony**.

BarberBook digitalizza l'agenda del salone eliminando la dipendenza da fogli Excel condivisi,
offrendo prenotazioni in tempo reale con notifiche push e gestione completa del ciclo di vita degli appuntamenti.

---

## Stack Tecnologico

| Layer | Tecnologia |
|-------|-----------|
| Backend | Java 21 LTS · Spring Boot 3.3.x · Maven |
| Frontend | React 18 · Vite 6 · TypeScript 5 · Tailwind CSS v4 |
| Database | PostgreSQL 16 (GiST Exclusion Constraints) |
| Real-time | Spring SSE (Server-Sent Events) |
| Auth | JWT (15 min) + Refresh Token Rotation (HttpOnly cookie) |
| ORM | Spring Data JPA · Flyway (migrazioni versionabili) |
| DevOps | Docker · GitHub Actions · SonarCloud · Snyk |

---

## Struttura del Repository

```
barberbook/
├── backend/          ← Spring Boot application (Maven)
├── frontend/         ← React + Vite application (TypeScript)
├── docker/           ← Dockerfile e nginx.conf
├── docs/             ← Documentazione di progetto
│   ├── rad.md            ← Requirements & Analysis Document
│   ├── tech_design.md    ← Technology Design
│   ├── design_patterns.md
│   ├── sprint_plan.md    ← Piano Agile complessivo
│   └── sprint/           ← Dettaglio sprint-by-sprint
├── .github/
│   └── workflows/    ← GitHub Actions CI pipeline
├── docker-compose.yml
├── .env.example      ← Template variabili d'ambiente
└── README.md
```

---

## Quick Start (Docker)

### Prerequisiti
- Docker Desktop 24+

### Avvio
```bash
# 1. Clona il repository
git clone https://github.com/gianfrancobarba/BarberBook.git
cd BarberBook

# 2. Crea il file .env dal template
cp .env.example .env
# ⚠️  Modifica .env con le tue credenziali prima di procedere

# 3. Avvia lo stack completo
docker-compose up --build
```

### Endpoint
| Servizio | URL |
|---------|-----|
| Frontend | http://localhost |
| Backend API | http://localhost:8080/api |
| Health Check | http://localhost:8080/api/health |

### Credenziali iniziali
| Attore | Email | Password |
|--------|-------|---------|
| Barbiere (BAR) | `tony@hairmanbarber.it` | vedere `.env.example` |

> ⚠️ **Cambiare la password BAR prima del deploy in produzione.**

---

## Sviluppo Locale

### Backend
```bash
cd backend
cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml
mvn spring-boot:run -Pdev
```

### Frontend
```bash
cd frontend
npm install
npm run dev
# Disponibile su http://localhost:3000
```

### Database (solo)
```bash
docker-compose up postgres
```

---

## Qualità

| Metrica | Target |
|---------|--------|
| JaCoCo Line Coverage | ≥ 80% |
| JaCoCo Branch Coverage | ≥ 75% |
| PiTest Mutation Score | ≥ 70% |
| SonarCloud Security Rating | A |
| Vulnerabilità Snyk (HIGH+) | 0 |

---

## Branching Strategy

```
main          ← produzione stabile (protetto — solo merge da develop)
develop       ← integrazione (protetto — solo PR con CI verde)
feature/S{n}-{descrizione}   ← branch per ogni user story
```

---

## Documentazione

- [`docs/rad.md`](docs/rad.md) — Requisiti funzionali e non funzionali
- [`docs/tech_design.md`](docs/tech_design.md) — Stack tecnologico e architettura
- [`docs/sprint_plan.md`](docs/sprint_plan.md) — Piano Agile con tutti gli sprint
- [`docs/sprint/`](docs/sprint/) — Dettaglio fase-per-fase di ogni sprint

---

## Licenza

Progetto di sviluppo privato — tutti i diritti riservati.
