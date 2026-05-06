# BarberBook — Hair Man Tony

> Sistema di prenotazione online per il salone **Hair Man Tony**.  
> Clienti e ospiti prenotano dal web; il barbiere gestisce l'agenda dal pannello dedicato.

---

## Stack Tecnologico

| Layer | Tecnologia |
|-------|-----------|
| Backend | Java 21 · Spring Boot 3.3 · Maven |
| Frontend | React 19 · Vite · TypeScript · Tailwind CSS v4 · shadcn/ui |
| Database | PostgreSQL 16 (GiST Exclusion Constraint anti-double-booking) |
| Real-time | Server-Sent Events (SSE) |
| Auth | JWT access token (15 min) + Refresh Token Rotation (HttpOnly cookie, 7 gg) |
| Infrastruttura | Docker Compose (Nginx + Spring Boot + Vite dev server + PostgreSQL) |

---

## Avvio rapido

### Prerequisiti
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installato e in esecuzione
- File `.env` nella root (vedi sotto)

### Configurazione `.env`

```env
DB_USER=barberbook_user
DB_PASSWORD=barberbook_pass
JWT_SECRET=una-chiave-segreta-di-almeno-32-caratteri-per-hmac
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost
```

### Avvio

```bash
# Clona il repository
git clone <repo-url>
cd BarberBook

# Avvia tutti i servizi
docker compose up --build
```

L'applicazione sarà disponibile su **http://localhost**

| Servizio | URL |
|----------|-----|
| Frontend (via nginx) | http://localhost |
| Backend API | http://localhost/api |
| Backend diretto | http://localhost:8080 |
| Database | localhost:5432 |

---

## Credenziali demo

### Barbiere (Tony)
| Campo | Valore |
|-------|--------|
| Email | `tony@hairmanbarber.it` |
| Password | `admin1234` |

### Cliente (registrazione libera)
Vai su http://localhost/register e crea un account. Oppure prenota direttamente come ospite su http://localhost/book.

---

## Funzionalità principali

### Per i clienti
- **Prenotazione senza registrazione** — scegli servizio, data, orario e lascia nome e telefono
- **Prenotazione con account** — storico completo, gestione appuntamenti, annullamento con motivazione
- **Notifiche real-time** — avviso immediato quando Tony accetta o rifiuta la prenotazione
- **Riprenotazione rapida** — riprenota un servizio già effettuato in un click

### Per il barbiere (Tony)
- **Agenda giornaliera** — vista per poltrona con tutte le prenotazioni del giorno
- **Vista settimanale** — panoramica a calendario degli slot liberi e occupati
- **Gestione prenotazioni** — accetta, rifiuta o annulla con motivazione
- **Prenotazione diretta** — inserisce manualmente un appuntamento a nome di un cliente
- **Gestione catalogo** — crea e modifica servizi (nome, durata, prezzo, attivo/disattivo)
- **Gestione poltrone** — configura le postazioni disponibili
- **Orari di lavoro** — definisce orari apertura e pause per ogni poltrona, giorno per giorno

---

## Architettura

```
docker-compose.yml
├── nginx          → reverse proxy (porta 80) — instrada /api a backend, / a frontend
├── frontend       → Vite dev server (porta 3000) in modalità hot-reload
├── backend        → Spring Boot (porta 8080)
└── postgres       → PostgreSQL 16 (porta 5432)
```

### Backend — struttura package

```
com.barberbook
├── config/          → SecurityConfig, CORS, JWT
├── controller/      → REST endpoints (Auth, Booking, Client, Dashboard…)
├── domain/
│   ├── model/       → User, ClienteRegistrato, Barbiere, Prenotazione, Servizio…
│   └── enums/       → BookingStatus, UserRole, DayOfWeek, ScheduleType
├── dto/             → Request/Response DTO (record Java)
├── mapper/          → MapStruct mapper (entity ↔ DTO)
├── repository/      → Spring Data JPA
├── security/        → JwtAuthFilter, RateLimitFilter, UserPrincipal
├── service/         → logica di business
└── scheduler/       → aggiornamento automatico stato PASSATA
```

### Frontend — struttura src

```
src/
├── api/             → client Axios + funzioni per ogni dominio (auth, bookings, availability…)
├── components/
│   ├── common/      → ErrorBoundary, Logo, Spinner, StatusBadge, EmptyState…
│   ├── layout/      → PublicLayout, ClientLayout, BarberLayout, ProtectedRoute
│   └── ui/          → componenti shadcn/ui
├── hooks/           → useAuth, useBookings, useAvailability, useSSE…
├── pages/           → una pagina per route
├── stores/          → Zustand (authStore)
├── types/           → TypeScript interfaces allineate al backend
└── router.tsx       → React Router v6 con errorElement su ogni route group
```

---

## Sicurezza

| Misura | Dettaglio |
|--------|-----------|
| Rate limiting login | 5 tentativi/minuto per IP (Bucket4j + Caffeine) |
| JWT stateless | Access token 15 min, mai persistito in localStorage |
| Refresh token rotation | Ogni refresh revoca il vecchio token; riuso rilevato invalida tutta la sessione |
| HttpOnly cookie | Il refresh token non è accessibile via JS |
| BCrypt 12 rounds | Password hashate con costo elevato |
| GiST exclusion constraint | Impedisce double-booking a livello DB anche con race condition |
| CORS configurabile | Origini consentite via variabile d'ambiente |

---

## Variabili d'ambiente complete

| Variabile | Descrizione | Default dev |
|-----------|-------------|-------------|
| `DB_USER` | Utente PostgreSQL | — |
| `DB_PASSWORD` | Password PostgreSQL | — |
| `JWT_SECRET` | HMAC secret (min 32 char) | — |
| `CORS_ALLOWED_ORIGINS` | Origini CORS consentite (comma-separated) | `http://localhost:3000,http://localhost` |
| `SPRING_PROFILES_ACTIVE` | Profilo Spring (`dev`/`prod`) | `dev` |
| `BARBER_PASSWORD_HASH` | BCrypt hash password Tony (prod) | dal profilo dev |

---

## Database — migrazioni Flyway

| Versione | Contenuto |
|----------|-----------|
| V1 | Schema base utenti |
| V2 | Tabelle `barbers`, `clients`, `refresh_tokens` |
| V3 | Seed account barbiere (Tony) |
| V4–V5 | Schema e seed servizi |
| V6–V7 | Schema e seed poltrone |
| V8–V9 | Schema e seed orari |
| V10 | Schema prenotazioni |
| V11 | GiST exclusion constraint anti-double-booking |
| V12 | Schema notifiche |
| V13 | Schema password reset token |

---

## Test

```bash
# Backend
cd backend && mvn test

# Frontend (type check)
cd frontend && npx tsc -b --noEmit
```

---

## Licenza

Progetto sviluppato per uso interno del salone Hair Man Tony. Tutti i diritti riservati.
