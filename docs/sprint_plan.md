# Sprint Plan — BarberBook
> Piano di sviluppo Agile/Scrum  
> Autore: Gianfranco Barba | Versione: 1.0 | Data: 22/04/2026  
> Stato: **IN ATTESA DI APPROVAZIONE**

---

## Premessa Metodologica

Il piano adotta una metodologia **Agile ispirata a Scrum** con sprint tematici a complessità crescente.
Ogni sprint rispetta il principio di **dipendenza verticale**: nessuna feature viene sviluppata senza che le sue dipendenze siano già implementate, testate e validate.

Il criterio di ordinamento degli sprint è:

> **"Prima le fondamenta, poi le mura, poi il tetto."**  
> Infrastruttura → Identità → Risorse di salone → Disponibilità → Prenotazioni → Notifiche → Viste → Feature avanzate → Frontend → Hardening

### Definition of Done (DoD) — Globale

Uno sprint è considerato **DONE** solo se:

- [ ] Tutti i requisiti funzionali dello sprint sono implementati
- [ ] **Unit test** coprono il Service layer con coverage ≥ 80% (JaCoCo)
- [ ] **Integration test** coprono il flusso API end-to-end (MockMvc + Testcontainers)
- [ ] La pipeline GitHub Actions è **verde** (build + test + JaCoCo + SonarCloud)
- [ ] Nessun Code Smell **Critical/Major** su SonarCloud
- [ ] Le Entity JPA non sono mai esposte direttamente via API (DTO rigorosi)
- [ ] Le migrazioni Flyway sono versionabili e applicate automaticamente
- [ ] Il branch `develop` è aggiornato e stabile

---

## Mappa delle Dipendenze (visuale)

```
Sprint 0 — Infrastruttura
    └── Sprint 1 — Auth & Utenti
            └── Sprint 2 — Catalogo Servizi
                    └── Sprint 3 — Gestione Poltrone ✅
                            └── Sprint 4 — Orari & Disponibilità
                                    └── Sprint 5 — Prenotazioni (Core)
                                            ├── Sprint 6 — Notifiche
                                            ├── Sprint 7 — Dashboard & Storico
                                            └── Sprint 8 — Feature Avanzate
                                                        └── Sprint 9 — Frontend React
                                                                └── Sprint 10 — Security Hardening
```

---

## Sprint 0 — Infrastruttura & Scaffolding

**Obiettivo**: Avere un progetto che compila, si avvia, passa la CI e ha un database raggiungibile.  
Nessuna feature di business in questo sprint — solo la fondamenta tecnica.

### User Stories
| ID | Descrizione | Note |
|----|-------------|------|
| INF-1 | Struttura monorepo con `backend/` e `frontend/` | Maven + Vite |
| INF-2 | Spring Boot 3.3.x configurato con Java 21 | `application.yml` dev/prod |
| INF-3 | Docker Compose con PostgreSQL 16 + backend + nginx | healthcheck + depends_on |
| INF-4 | Prima migrazione Flyway `V1__init_schema.sql` | schema base (solo tabella users per ora) |
| INF-5 | Endpoint `GET /api/health` che risponde `200 OK` | verifica che lo stack giri |
| INF-6 | GitHub Actions CI pipeline verde | build + test vuoto + JaCoCo |
| INF-7 | SonarCloud collegato al repo | quality gate configurato |
| INF-8 | `.env` e gestione secrets via variabili d'ambiente | nessuna credenziale hardcoded |
| INF-9 | Gitignore + struttura `docs/` consolidata | |

### Struttura Package Backend (da creare)
```
com.barberbook/
├── config/
├── domain/model/
├── domain/enums/
├── repository/
├── service/
├── controller/
├── dto/
├── security/
├── exception/
└── util/
```

### Migrazioni Flyway Sprint 0
- `V1__init_schema.sql` — tabella `users` (schema base)

### Test Richiesti
- Nessun test di business — solo verifica che `mvn verify` passi con 0 test failure
- Test infrastruttura: il container Docker si avvia e risponde all'health check

### Definition of Done Sprint 0
- [ ] `mvn verify` verde in locale e in CI
- [ ] `docker-compose up` avvia stack completo senza errori
- [ ] `GET /api/health` risponde `{"status": "UP"}`
- [ ] SonarCloud riceve il primo report (coverage 0% — accettabile)
- [ ] Branch `main` e `develop` protetti con regole di merge

---

## Sprint 1 — Autenticazione & Gestione Utenti

**Obiettivo**: Implementare il sistema di identità completo. Dopo questo sprint ogni attore del sistema ha la propria identità e può autenticarsi in sicurezza.

**Dipende da**: Sprint 0 ✅

### Requisiti Funzionali Coperti
| RF | Nome | Attore |
|----|------|--------|
| RF_GEN_1 | Registrazione Account | CLR |
| RF_GEN_2 | Login | BAR, CLR |
| RF_GEN_3 | Logout | BAR, CLR |

### User Stories
| ID | Descrizione | Pattern/Tecnica |
|----|-------------|-----------------|
| AUTH-1 | Gerarchia entità `User` → `Barbiere`, `ClienteRegistrato` (JPA Inheritance) | Entity, Builder |
| AUTH-2 | Registrazione CLR con validazione email/telefono | Bean Validation, DTO |
| AUTH-3 | Login BAR e CLR — verifica password BCrypt(12) | Spring Security |
| AUTH-4 | Generazione Access Token JWT (15 min, HMAC-SHA256) | jjwt |
| AUTH-5 | Generazione e salvataggio Refresh Token (7 giorni, hashed SHA-256) | RT Rotation |
| AUTH-6 | `POST /auth/refresh` — rinnovo coppia token con rotazione | Security by Design |
| AUTH-7 | `POST /auth/logout` — invalidazione Refresh Token dal DB | |
| AUTH-8 | `JwtFilter` — intercettazione e validazione JWT su ogni request | Spring Security Filter Chain |
| AUTH-9 | RBAC: `ROLE_BARBER`, `ROLE_CLIENT` configurati su Spring Security | `@PreAuthorize` |
| AUTH-10 | Account BAR pre-configurato via migrazione Flyway (non si registra autonomamente) | Flyway seed |

### Entità JPA
```
User (abstract)
  ├── id, nome, cognome, telefono, email, ruolo
  ├── Barbiere extends User
  └── ClienteRegistrato extends User
        └── password (BCrypt hash)

RefreshToken
  ├── id, tokenHash, userId, expiresAt, revoked
```

### Migrazioni Flyway Sprint 1
- `V2__auth_schema.sql` — tabella `users` completa, tabella `refresh_tokens`
- `V3__seed_barber.sql` — account BAR pre-inserito con password BCrypt

### API Endpoints
| Metodo | Path | Auth | RF |
|--------|------|------|----|
| POST | `/api/auth/register` | Pubblico | RF_GEN_1 |
| POST | `/api/auth/login` | Pubblico | RF_GEN_2 |
| POST | `/api/auth/refresh` | Pubblico (RT in cookie) | — |
| POST | `/api/auth/logout` | Autenticato | RF_GEN_3 |
| GET | `/api/users/me` | Autenticato | — |

### Test Richiesti
**Unit Test (AuthService)**
- `register_success` — registrazione valida crea CLR con password hashata
- `register_duplicateEmail_throwsException` — email già usata
- `login_validCredentials_returnsTokenPair` — login corretto
- `login_wrongPassword_throwsException` — password errata
- `login_unknownEmail_throwsException` — utente inesistente
- `refreshToken_valid_returnsNewPair` — rotazione corretta
- `refreshToken_expired_throwsException` — token scaduto
- `refreshToken_revoked_throwsException` — token già usato (segnale di furto)
- `logout_invalidatesRefreshToken` — RT rimosso dal DB

**Integration Test (MockMvc + Testcontainers)**
- Flusso completo: register → login → accesso endpoint protetto → refresh → logout
- Tentativo accesso endpoint BARBER con token CLIENT → `403 Forbidden`
- Tentativo accesso senza token → `401 Unauthorized`

### Definition of Done Sprint 1
- [ ] Registrazione CLR funzionante via API
- [ ] Login BAR e CLR restituiscono JWT validi
- [ ] Refresh Token rotation implementata e testata (incluso scenario furto token)
- [ ] Nessun endpoint BAR-only accessibile da CLR
- [ ] BCrypt strength 12 configurato
- [ ] Coverage Service layer ≥ 80%

---

## Sprint 2 — Catalogo Servizi

**Obiettivo**: Il BAR può gestire la propria offerta. I clienti possono consultare i servizi disponibili senza autenticarsi.

**Dipende da**: Sprint 1 ✅ (sistema di autenticazione + RBAC)

### Requisiti Funzionali Coperti
| RF | Nome | Attore |
|----|------|--------|
| RF_BAR_6 | Creazione Servizio | BAR |
| RF_BAR_7 | Modifica Servizio | BAR |
| RF_BAR_8 | Eliminazione Servizio | BAR |
| RF_CLI_1 | Vetrina Servizi (pubblica) | CLI |

### User Stories
| ID | Descrizione | Pattern/Tecnica |
|----|-------------|-----------------|
| SVC-1 | Entità `Servizio` (nome, durata minuti, prezzo, attivo) | Entity, Builder |
| SVC-2 | CRUD completo servizi riservato a BAR | `@PreAuthorize("hasRole('BARBER')")` |
| SVC-3 | Soft-delete: eliminazione → `attivo = false` (non cancellazione fisica) | Integrità referenziale |
| SVC-4 | Endpoint pubblico vetrina servizi (no auth) | Endpoint pubblico |
| SVC-5 | Validazione: durata > 0, prezzo ≥ 0, nome non vuoto | Bean Validation + DTO |

### Entità JPA
```
Servizio
  ├── id, nome, descrizione
  ├── durataMinuti (> 0)
  ├── prezzo (>= 0, DecimalMin)
  └── attivo (default: true)
```

### Migrazioni Flyway Sprint 2
- `V4__services_schema.sql` — tabella `services`
- `V5__seed_services.sql` — servizi iniziali (Capelli, Barba, Capelli+Barba, ecc.)

### API Endpoints
| Metodo | Path | Auth | RF |
|--------|------|------|----|
| GET | `/api/services` | Pubblico | RF_CLI_1 |
| GET | `/api/services/{id}` | Pubblico | RF_CLI_1 |
| POST | `/api/services` | BARBER | RF_BAR_6 |
| PUT | `/api/services/{id}` | BARBER | RF_BAR_7 |
| DELETE | `/api/services/{id}` | BARBER | RF_BAR_8 |

### Test Richiesti
**Unit Test (ServiceCatalogService)**
- `createService_validData_success`
- `createService_negativeDuration_throwsValidationException`
- `updateService_notFound_throwsException`
- `deleteService_setsActiveFalse` — verifica soft-delete
- `getPublicServices_returnsOnlyActive` — i servizi eliminati non appaiono in vetrina

**Integration Test**
- CLR non può creare/modificare/eliminare servizi → `403`
- Guest può vedere vetrina servizi → `200`

### Definition of Done Sprint 2
- [ ] CRUD servizi funzionante per BAR
- [ ] Soft-delete implementato (i servizi cancellati non appaiono in vetrina)
- [ ] Vetrina pubblica accessibile senza autenticazione
- [ ] Validazione input su tutti i DTO

---

## Sprint 3 — Gestione Poltrone

**Obiettivo**: Il BAR gestisce le risorse fisiche del salone. Le poltrone sono l'unità base su cui si organizza tutta l'agenda.

**Dipende da**: Sprint 1 ✅

### Requisiti Funzionali Coperti
| RF | Nome | Attore |
|----|------|--------|
| RF_BAR_3 | Aggiunta Poltrona | BAR |
| RF_BAR_4 | Rimozione Poltrona | BAR |
| RF_BAR_5 | Rinomina Poltrona | BAR |
| RF_CLI_2 | Visualizzazione Poltrone Disponibili | CLI |

### User Stories
| ID | Descrizione | Pattern/Tecnica |
|----|-------------|-----------------|
| CHR-1 | Entità `Poltrona` (nome, attiva) | Entity, Builder |
| CHR-2 | Aggiunta poltrona con nome univoco | Validazione unicità |
| CHR-3 | Soft-delete poltrona → `attiva = false` | Integrità referenziale (prenotazioni esistenti) |
| CHR-4 | Rinomina poltrona | PATCH parziale |
| CHR-5 | Lista poltrone attive pubblica | Endpoint pubblico |
| CHR-6 | Seed iniziale: 2 poltrone predefinite | Flyway |

### Entità JPA
```
Poltrona
  ├── id, nome (unique, not null)
  └── attiva (default: true)
```

### Migrazioni Flyway Sprint 3
- `V6__chairs_schema.sql` — tabella `chairs`
- `V7__seed_chairs.sql` — due poltrone iniziali ("Poltrona 1", "Poltrona 2")

### API Endpoints
| Metodo | Path | Auth | RF |
|--------|------|------|----|
| GET | `/api/chairs` | Pubblico | RF_CLI_2 |
| POST | `/api/chairs` | BARBER | RF_BAR_3 |
| PATCH | `/api/chairs/{id}` | BARBER | RF_BAR_5 |
| DELETE | `/api/chairs/{id}` | BARBER | RF_BAR_4 |

### Test Richiesti
**Unit Test (ChairService)**
- `addChair_uniqueName_success`
- `addChair_duplicateName_throwsException`
- `deleteChair_setsActiveFalse`
- `renameChair_updatesName`
- `getActiveChairs_returnsOnlyActiveChairs`

**Integration Test**
- Guest vede lista poltrone attive
- Poltrona soft-deleted non appare in lista pubblica

### Definition of Done Sprint 3
- [x] CRUD poltrone funzionante per BAR
- [x] Unicità del nome garantita a livello DB e Service
- [x] Soft-delete implementato e testato
- [x] Vetrina pubblica poltrone funzionante
- [x] Seed Flyway V7 applicato

---

## Sprint 4 — Orari & Calcolo Disponibilità

**Obiettivo**: Il motore di calcolo degli slot disponibili. Questo è il cuore algoritmico del sistema — lo sprint più complesso fino a questo punto.

**Dipende da**: Sprint 2 ✅ (servizi con durata) + Sprint 3 ✅ (poltrone)

### Requisiti Funzionali Coperti
| RF | Nome | Attore |
|----|------|--------|
| RF_BAR_9 | Definizione Orari di Apertura | BAR |
| RF_BAR_10 | Gestione Pause | BAR |
| RF_CLI_3 | Filtro per Giorno | CLI |
| RF_CLI_4 | Visualizzazione Slot Liberi | CLI |

### User Stories
| ID | Descrizione | Pattern/Tecnica |
|----|-------------|-----------------|
| SCH-1 | Entità `FasciaOraria` (giorno, oraInizio, oraFine, tipo: APERTURA/PAUSA, poltrona) | Entity |
| SCH-2 | BAR configura orari di apertura per giorno della settimana | CRUD |
| SCH-3 | BAR aggiunge/rimuove fasce di pausa per poltrona e giorno | CRUD |
| SCH-4 | `AvailabilityService` con `Strategy Pattern` — calcolo slot liberi | Strategy, Value Object TimeSlot |
| SCH-5 | `StandardAvailabilityStrategy` — granularità 15 min, filtra pause e prenotazioni esistenti | Strategy impl |
| SCH-6 | API pubblica: slot liberi per giorno + servizio (durata considerata) | |
| SCH-7 | Gestione giorni di chiusura (nessuna fascia di apertura = chiuso) | |

### Entità JPA
```
FasciaOraria
  ├── id, giornoSettimana (enum Mon-Sun)
  ├── oraInizio, oraFine
  ├── tipo (APERTURA | PAUSA)
  └── @ManyToOne Poltrona

TimeSlot (Value Object — non persistito)
  ├── start: LocalTime
  ├── end: LocalTime
  └── overlapsWith(other): boolean
```

### Migrazioni Flyway Sprint 4
- `V8__schedules_schema.sql` — tabella `schedules` + `breaks`
- `V9__seed_schedules.sql` — orari di default (Lun-Sab 9:00-19:00, pausa 13:00-15:00)

### API Endpoints
| Metodo | Path | Auth | RF |
|--------|------|------|----|
| GET | `/api/availability?date={date}&serviceId={id}` | Pubblico | RF_CLI_3, RF_CLI_4 |
| GET | `/api/schedules` | BARBER | RF_BAR_9 |
| POST | `/api/schedules` | BARBER | RF_BAR_9 |
| PUT | `/api/schedules/{id}` | BARBER | RF_BAR_9 |
| DELETE | `/api/schedules/{id}` | BARBER | RF_BAR_9 |
| POST | `/api/schedules/{id}/breaks` | BARBER | RF_BAR_10 |
| DELETE | `/api/schedules/breaks/{id}` | BARBER | RF_BAR_10 |

### Test Richiesti — Intensivi (cuore del sistema)
**Unit Test (StandardAvailabilityStrategy — PURI, nessun mock DB)**
- `noBreaks_noBookings_returnsAllPossibleSlots` — 9:00-13:00 con servizio 30min → 13 slot
- `withBreak_breakSlotsExcluded` — pausa 11:00-12:00 rimuove slot sovrapposti
- `existingBooking_overlappingSlotRemoved` — prenotazione 10:00-10:30 blocca slot 9:45 e 10:00
- `serviceDurationLongerThanRemainingDay_noSlotAvailable`
- `closedDay_returnsEmptyList`
- `slotAtExactClosingTime_notIncluded` — slot 12:45 con servizio 30min: finisce a 13:15 > 13:00 → escluso
- `multipleChairs_eachCalculatedIndependently`

**Unit Test (AvailabilityService)**
- `getAvailableSlots_callsStrategyWithCorrectContext`
- `getAvailableSlots_unknownService_throwsNotFoundException`

**Integration Test**
- Endpoint `/api/availability` restituisce slot corretti con DB reale
- Slot già prenotati (`IN_ATTESA` / `ACCETTATA`) non appaiono come disponibili

> **Nota**: `JMH` benchmark su `AvailabilityService` eseguito manualmente in questa fase.

### Definition of Done Sprint 4
- [ ] `AvailabilityService` testato con ≥ 10 scenari unitari puri
- [ ] Slot liberi calcolati correttamente tenendo conto di orari, pause e prenotazioni esistenti
- [ ] Slot con durata che sfora l'orario di chiusura esclusi correttamente
- [ ] Coverage Strategy layer ≥ 90% (logica critica)

---

## Sprint 5 — Prenotazioni (Core)

**Obiettivo**: Il cuore operativo di BarberBook. Tutto il ciclo di vita delle prenotazioni, dalla richiesta del cliente all'approvazione o rifiuto del BAR, fino all'annullamento.

**Dipende da**: Sprint 4 ✅ (disponibilità) + Sprint 1 ✅ (utenti)

### Requisiti Funzionali Coperti
| RF | Nome | Attore |
|----|------|--------|
| RF_CLI_5 | Selezione Servizio in Prenotazione | CLI |
| RF_CLI_6 | Invio Richiesta di Prenotazione | CLI |
| RF_CLG_1 | Form Dati Ospite | CLG |
| RF_BAR_11 | Creazione Prenotazione Diretta | BAR |
| RF_BAR_12 | Modifica Prenotazione | BAR |
| RF_BAR_13 | Cancellazione Prenotazione | BAR |
| RF_BAR_14 | Accettazione Richiesta | BAR |
| RF_BAR_15 | Rifiuto Richiesta | BAR |
| RF_CLR_4 | Annullamento Prenotazione con motivazione | CLR |

### User Stories
| ID | Descrizione | Pattern/Tecnica |
|----|-------------|-----------------|
| BKG-1 | Entità `Prenotazione` completa con `BookingStatus` enum + transizioni | State, Builder |
| BKG-2 | `BookingStatus` con `canTransitionTo()` + `transitionTo()` + `InvalidBookingTransitionException` | State Pattern |
| BKG-3 | `GuestData` embedded per CLG (nome, cognome, telefono) | @Embedded |
| BKG-4 | `ValidationChain` pre-creazione prenotazione (4 validators con @Order) | Chain of Responsibility |
| BKG-5 | `BookingService` come Facade: `createRequest`, `acceptRequest`, `rejectRequest`, `cancelByClient`, `cancelByBarber`, `createDirect`, `updateBooking` | Facade |
| BKG-6 | Flusso CLR: selezione servizio → slot → conferma → stato `IN_ATTESA` | UC_CLI_1 |
| BKG-7 | Flusso CLG: come CLR + form dati ospite obbligatorio | RF_CLG_1 |
| BKG-8 | BAR accetta richiesta → stato `ACCETTATA` | UC_BAR_2 |
| BKG-9 | BAR rifiuta richiesta → stato `RIFIUTATA` | UC_BAR_2 alt A |
| BKG-10 | CLR annulla prenotazione con motivazione → stato `ANNULLATA` | RF_CLR_4 |
| BKG-11 | BAR crea prenotazione diretta → stato `ACCETTATA` direttamente | RF_BAR_11, SD_2 |
| BKG-12 | BAR modifica prenotazione (orario/servizio/poltrona) con verifica disponibilità | RF_BAR_12 |
| BKG-13 | BAR cancella prenotazione → stato `ANNULLATA` | RF_BAR_13 |
| BKG-14 | Exclusion Constraint PostgreSQL GiST (no double-booking a livello DB) | RNF_R_1 |
| BKG-15 | Optimistic Locking con `@Version` su `Prenotazione` (race condition) | JPA Locking |
| BKG-16 | Transizione automatica a `PASSATA` per prenotazioni scadute (scheduled task) | `@Scheduled` |

### Entità JPA
```
Prenotazione
  ├── id, startTime, endTime
  ├── status: BookingStatus (IN_ATTESA | ACCETTATA | RIFIUTATA | ANNULLATA | PASSATA)
  ├── cancellationReason (nullable)
  ├── version (Long — @Version per optimistic locking)
  ├── createdAt, updatedAt
  ├── @ManyToOne Poltrona
  ├── @ManyToOne Servizio
  ├── @ManyToOne User client (nullable → CLG)
  └── @Embedded GuestData (nullable → CLR)

GuestData (@Embeddable)
  ├── guestNome, guestCognome, guestTelefono
```

### Migrazioni Flyway Sprint 5
- `V10__bookings_schema.sql` — tabella `bookings` con TUTTI i campi + FK
- `V11__bookings_gist_constraint.sql` — Exclusion Constraint GiST no-overlap

```sql
-- Esempio constraint in migrazione
ALTER TABLE bookings
ADD CONSTRAINT no_overlap_booking
EXCLUDE USING GIST (
  chair_id WITH =,
  tstzrange(start_time, end_time, '[)') WITH &&
)
WHERE (status IN ('IN_ATTESA', 'ACCETTATA'));
```

### API Endpoints
| Metodo | Path | Auth | RF |
|--------|------|------|----|
| POST | `/api/bookings` | Pubblico / CLIENT | RF_CLI_6, RF_CLG_1 |
| GET | `/api/bookings/pending` | BARBER | RF_BAR_16 (richieste in attesa) |
| PATCH | `/api/bookings/{id}/accept` | BARBER | RF_BAR_14 |
| PATCH | `/api/bookings/{id}/reject` | BARBER | RF_BAR_15 |
| POST | `/api/bookings/direct` | BARBER | RF_BAR_11 |
| PUT | `/api/bookings/{id}` | BARBER | RF_BAR_12 |
| DELETE | `/api/bookings/{id}` | BARBER | RF_BAR_13 |
| PATCH | `/api/bookings/{id}/cancel` | CLIENT | RF_CLR_4 |

### Test Richiesti — I Più Critici dell'Intero Progetto
**Unit Test (BookingStatus — State Machine)**
- `inAttesa_canTransitionTo_accettata` ✓
- `inAttesa_canTransitionTo_rifiutata` ✓
- `accettata_canTransitionTo_annullata` ✓
- `accettata_canTransitionTo_passata` ✓
- `rifiutata_isTerminal` — set transizioni vuoto
- `annullata_isTerminal`
- `passata_isTerminal`
- `rifiutata_transitionTo_accettata_throwsInvalidTransitionException`
- `passata_transitionTo_annullata_throwsInvalidTransitionException`

**Unit Test (BookingService — Facade)**
- `createRequest_slotAvailable_createsWithStatusInAttesa`
- `createRequest_slotNotAvailable_throwsSlotNotAvailableException`
- `acceptRequest_fromInAttesa_statusBecomesAccettata`
- `acceptRequest_fromRifiutata_throwsInvalidTransitionException`
- `rejectRequest_fromInAttesa_statusBecomesRifiutata`
- `cancelByClient_ownBooking_statusBecomesAnnullata`
- `cancelByClient_notOwner_throwsUnauthorizedException`
- `cancelByClient_missingReason_throwsValidationException`
- `createDirect_byBarBer_statusIsAccettataDirectly`
- `updateBooking_newSlotOccupied_throwsSlotNotAvailableException`

**Unit Test (ValidationChain — ogni Validator isolato)**
- `ChairActiveValidator_inactiveChair_throwsException`
- `SlotWithinScheduleValidator_outsideHours_throwsException`
- `SlotNotInBreakValidator_slotInBreak_throwsException`
- `NoOverlapValidator_overlappingBooking_throwsException`
- `NoOverlapValidator_noOverlap_passes`

**Integration Test — Testcontainers PostgreSQL Reale**
- `doubleBooking_sameSlotSameChair_onlyOneSucceeds` (100 tentativi concorrenti → esatto 1 accettato)
- Flusso completo: CLR invia richiesta → BAR accetta → stato ACCETTATA in DB
- Flusso alternativo: CLR invia richiesta → BAR rifiuta → stato RIFIUTATA
- CLR annulla con motivazione → stato ANNULLATA, motivazione salvata
- Exclusion Constraint GiST blocca secondo insert con stato `ACCETTATA` sullo stesso slot

**PiTest Mutation Testing**
- Eseguito al termine di Sprint 5 su `com.barberbook.service.*`
- Target: Mutation Score ≥ 70%

### Definition of Done Sprint 5
- [ ] Tutti i 9 stati-transizioni della state machine coperti dai test
- [ ] Test di no-double-booking con 100 tentativi concorrenti supera il gate
- [ ] Exclusion Constraint GiST attivo in DB
- [ ] Optimistic Locking (`@Version`) attivo sull'entità Prenotazione
- [ ] ValidationChain completa con tutti i 4 validator testati in isolamento
- [ ] PiTest mutation score ≥ 70% sul Service layer

---

## Sprint 6 — Notifiche In-App (SSE)

**Obiettivo**: Il sistema reagisce agli eventi di business e avvisa proattivamente gli attori in real-time.

**Dipende da**: Sprint 5 ✅ (eventi di prenotazione)

### Requisiti Funzionali Coperti
| RF | Nome | Attore |
|----|------|--------|
| RF_GEN_5 | Notifiche In-App | BAR, CLR |
| RF_BAR_16 | Area Notifiche BAR | BAR |
| RF_CLR_7 | Area Notifiche CLR | CLR |

### User Stories
| ID | Descrizione | Pattern/Tecnica |
|----|-------------|-----------------|
| NOT-1 | Entità `Notifica` (destinatario, tipo, titolo, testo, letta, timestamp) | Entity |
| NOT-2 | `NotificationType` enum (NUOVA_RICHIESTA, ACCETTATA, RIFIUTATA, ANNULLAMENTO_CLIENTE, ANNULLAMENTO_BARBIERE) | Enum |
| NOT-3 | `NotificationFactory` — creazione centralizzata messaggi tipizzati per ogni evento | Factory Method |
| NOT-4 | `NotificationService` con `@EventListener` sui domain events di Spring | Observer |
| NOT-5 | `SseEmitterRegistry` — registro delle connessioni SSE attive (ConcurrentHashMap) | Component |
| NOT-6 | `GET /api/notifications/stream` — endpoint SSE per push real-time | SSE |
| NOT-7 | `GET /api/notifications` — lista notifiche persistite (storico) | |
| NOT-8 | `PATCH /api/notifications/{id}/read` — segna come letta | |
| NOT-9 | `BookingService` pubblica Spring Events dopo ogni transizione di stato | Observer Publisher |
| NOT-10 | CLG non riceve notifiche in-app — gestito fuori dal sistema | Eccezione domain |

### Domain Events pubblicati da BookingService
```java
BookingRequestCreatedEvent  → NotificationService → pushToBarber()
BookingAcceptedEvent        → NotificationService → pushToUser(clientId)
BookingRejectedEvent        → NotificationService → pushToUser(clientId)
BookingCancelledByClientEvent → NotificationService → pushToBarber()
BookingCancelledByBarberEvent → NotificationService → pushToUser(clientId)
```

### Entità JPA
```
Notifica
  ├── id, tipo: NotificationType
  ├── titolo, messaggio
  ├── letta (default: false)
  ├── createdAt
  ├── bookingId (riferimento)
  └── @ManyToOne User destinatario
```

### Migrazioni Flyway Sprint 6
- `V12__notifications_schema.sql` — tabella `notifications`

### API Endpoints
| Metodo | Path | Auth | RF |
|--------|------|------|----|
| GET | `/api/notifications/stream` | Autenticato | RF_GEN_5 |
| GET | `/api/notifications` | Autenticato | RF_BAR_16, RF_CLR_7 |
| PATCH | `/api/notifications/{id}/read` | Autenticato | — |
| PATCH | `/api/notifications/read-all` | Autenticato | — |

### Test Richiesti
**Unit Test (NotificationService)**
- `onBookingAccepted_createsNotificationForClient`
- `onBookingAccepted_guestBooking_noNotificationCreated` (CLG → nessuna notifica in-app)
- `onBookingRequestCreated_createsNotificationForBarber`
- `onBookingCancelledByClient_createsNotificationWithReason`

**Unit Test (NotificationFactory)**
- `createNewRequestNotification_containsCorrectClientName`
- `createAcceptedNotification_containsServiceAndTime`
- `createClientCancellationNotification_containsReason`

**Integration Test**
- BookingService pubblica evento → NotificationService persiste Notifica in DB
- Endpoint SSE riceve push quando viene pubblicata nuova notifica

### Definition of Done Sprint 6
- [ ] Notifica in-app inviata a BAR per ogni nuova richiesta
- [ ] Notifica in-app inviata a CLR per accettazione, rifiuto, cancellazione da BAR
- [ ] Notifica in-app inviata a BAR per cancellazione da CLR (con motivazione nel testo)
- [ ] CLG non genera notifiche in-app
- [ ] SSE funzionante: push real-time senza polling

---

## Sprint 7 — Dashboard & Storico Prenotazioni

**Obiettivo**: Le viste operative del BAR e il portale personale del CLR. Il BAR ha il pieno controllo visivo sull'agenda; il CLR ha il proprio storico filtrato.

**Dipende da**: Sprint 5 ✅ + Sprint 6 ✅

### Requisiti Funzionali Coperti
| RF | Nome | Attore |
|----|------|--------|
| RF_BAR_1 | Dashboard Settimanale | BAR |
| RF_BAR_2 | Dashboard Giornaliera | BAR |
| RF_CLR_1 | Homepage Personale (prossimi appuntamenti) | CLR |
| RF_CLR_2 | Visualizzazione Storico Prenotazioni | CLR |
| RF_CLR_3 | Filtro Prenotazioni per Stato | CLR |

### User Stories
| ID | Descrizione | Pattern/Tecnica |
|----|-------------|-----------------|
| DSH-1 | `GET /api/dashboard/daily?date={date}` — prenotazioni del giorno per poltrona | Specification |
| DSH-2 | `GET /api/dashboard/weekly?from={date}` — prenotazioni settimana per poltrona e giorno | Specification |
| DSH-3 | `GET /api/bookings/my` con filtro `?status={status}` — storico CLR | Specification |
| DSH-4 | `BookingSpecifications` composabili: byClient, byStatus, byDate, byChair, upcoming | Specification Pattern |
| DSH-5 | `GET /api/bookings/my/upcoming` — prossimi appuntamenti confermati CLR | RF_CLR_1 |
| DSH-6 | Ordinamento cronologico decrescente sullo storico | |
| DSH-7 | Paginazione opzionale sulle query di storico (Spring Pageable) | |

### API Endpoints
| Metodo | Path | Auth | RF |
|--------|------|------|----|
| GET | `/api/dashboard/daily` | BARBER | RF_BAR_2 |
| GET | `/api/dashboard/weekly` | BARBER | RF_BAR_1 |
| GET | `/api/bookings/my` | CLIENT | RF_CLR_2 |
| GET | `/api/bookings/my?status={status}` | CLIENT | RF_CLR_3 |
| GET | `/api/bookings/my/upcoming` | CLIENT | RF_CLR_1 |

### Test Richiesti
**Unit Test (BookingSpecifications)**
- `byClient_returnsOnlyClientBookings`
- `byStatus_returnsOnlyMatchingStatus`
- `byDate_returnsOnlyTodayBookings`
- `composedSpec_byClientAndStatus_filtersCorrectly`
- `upcoming_returnsOnlyFutureBookings`

**Integration Test**
- Dashboard giornaliera con 3 prenotazioni su 2 poltrone → struttura corretta nel JSON response
- Filtro storico per stato `ANNULLATA` → solo prenotazioni annullate
- CLR non vede le prenotazioni di altri clienti → isolamento dati

### Definition of Done Sprint 7
- [ ] Dashboard giornaliera e settimanale restituisce struttura corretta (raggruppata per poltrona)
- [ ] CLR vede solo le proprie prenotazioni
- [ ] Filtro per stato funzionante
- [ ] Nessuna prenotazione di altri CLR accessibile via API (test di isolamento)

---

## Sprint 8 — Feature Avanzate

**Obiettivo**: Completare le funzionalità secondarie che completano l'esperienza utente.

**Dipende da**: Sprint 5 ✅ + Sprint 7 ✅

### Requisiti Funzionali Coperti
| RF | Nome | Attore | Priorità |
|----|------|--------|----------|
| RF_GEN_4 | Recupero Password | CLR | Media |
| RF_CLR_6 | Modifica Profilo Personale | CLR | Alta |
| RF_CLR_5 | Riprenotazione Rapida | CLR | Bassa |
| RF_CLG_2 | Registrazione Veloce Post-Prenotazione | CLG | Bassa |

### User Stories
| ID | Descrizione | Pattern/Tecnica |
|----|-------------|-----------------|
| ADV-1 | `POST /auth/forgot-password` — invio link reset via email (JavaMailSender / Mailhog in dev) | Token temporaneo |
| ADV-2 | `POST /auth/reset-password` — reset password con token valido (scadenza 1h) | Token monouso |
| ADV-3 | `GET /api/users/me` — visualizzazione profilo CLR | |
| ADV-4 | `PUT /api/users/me` — modifica nome, cognome, email, telefono con validazione formato | Bean Validation |
| ADV-5 | `POST /api/bookings/{id}/rebook` — copia servizio e poltrona, richiede solo data/ora | RF_CLR_5 |
| ADV-6 | `POST /auth/guest-register` — conversione CLG in CLR post-prenotazione | RF_CLG_2 |

### Migrazioni Flyway Sprint 8
- `V13__password_reset_tokens.sql` — tabella `password_reset_tokens`

### Test Richiesti
**Unit Test**
- `forgotPassword_knownEmail_generatesToken`
- `forgotPassword_unknownEmail_silentNoOp` (non rivelare se email esiste)
- `resetPassword_validToken_updatesPassword`
- `resetPassword_expiredToken_throwsException`
- `resetPassword_usedToken_throwsException` (token monouso)
- `updateProfile_invalidEmail_throwsValidationException`
- `rebook_pastBooking_prefillsServiceAndChair`

### Definition of Done Sprint 8
- [ ] Password recovery funzionante (anche solo con log in dev se email non configurata)
- [ ] Token reset monouso e con scadenza
- [ ] Modifica profilo con validazione formato email/telefono
- [ ] `forgotPassword` non rivela se l'email è registrata (security best practice)

---

## Sprint 9 — Frontend React

**Obiettivo**: Realizzare l'interfaccia utente completa in React + TypeScript che consuma tutte le API backend già validate.

**Dipende da**: Sprint 1-8 ✅ (tutte le API sono stabili)  
**Sviluppo parallelo**: Il frontend può partire dalla Fase 2 usando **MSW (Mock Service Worker)** per simulare le API non ancora disponibili.

### Tecnologie
- React 18 + Vite 6 + TypeScript 5
- React Router v7 (routing SPA)
- TanStack Query v5 (server state + cache)
- Zustand (client state: auth, notifiche)
- Axios (HTTP client con interceptors JWT)
- shadcn/ui + Tailwind CSS v4 (componenti + styling)
- React Hook Form + Zod (form + validazione type-safe)
- date-fns (gestione date/ore)
- native EventSource (SSE per notifiche)
- Lucide React (icons)

### Schermate da Implementare
| Schermata | Pattern FE | RF Coperti |
|-----------|------------|------------|
| LoginUI | Container/Presenter | RF_GEN_2 |
| RegistrazioneUI | Container/Presenter | RF_GEN_1 |
| DashboardGiornalieraUI (BAR) | Container + Custom Hook | RF_BAR_2 |
| DashboardSettimanaleUI (BAR) | Container + Custom Hook | RF_BAR_1 |
| GestioneServiziUI (BAR) | Container/Presenter | RF_BAR_6/7/8 |
| GestionePoltronaUI (BAR) | Container/Presenter | RF_BAR_3/4/5 |
| GestioneOrariUI (BAR) | Container/Presenter | RF_BAR_9/10 |
| AreaNotificheUI (BAR + CLR) | Custom Hook SSE | RF_GEN_5 |
| VetrinaServiziUI (pubblica) | Presenter | RF_CLI_1/2 |
| PrenotazioneUI (flusso guidato) | Container multi-step | RF_CLI_3/4/5/6 |
| FormDatiOspiteUI | Presenter + Zod | RF_CLG_1 |
| HomepagePersonaleUI (CLR) | Container | RF_CLR_1 |
| StoricoPrenotazioniUI (CLR) | Container + filtri | RF_CLR_2/3 |
| ProfiloPersonaleUI (CLR) | Container/Presenter | RF_CLR_6 |

### Custom Hooks Principali
```typescript
useAuth()            // login, logout, refresh, stato sessione
useAvailableSlots()  // calcolo disponibilità
useCreateBooking()   // invio richiesta prenotazione
useMyBookings()      // storico con filtri
useDailyDashboard()  // agenda BAR giornaliera
useWeeklyDashboard() // agenda BAR settimanale
useNotifications()   // SSE + storico notifiche
useServices()        // catalogo servizi
useChairs()          // poltrone attive
```

### Test Frontend (Vitest + RTL + MSW)
- `BookingListPresenter` — render corretto con prenotazioni / stato vuoto / loading
- `useCreateBooking` — mock MSW, verifica invalidazione cache post-success
- `LoginPage` — submit form con dati validi → redirect dashboard
- `FormDatiOspiteUI` — campi obbligatori: submit bloccato se mancanti
- `BookingStatusBadge` — colori corretti per ogni stato

### Definition of Done Sprint 9
- [ ] Tutte le schermate implementate e funzionanti
- [ ] Responsive su 375px / 768px / 1280px (RNF_U_2)
- [ ] Tutte le label e testi in italiano (RNF_X_1)
- [ ] JWT gestito con Access Token in memoria (Zustand) e Refresh Token in cookie HttpOnly
- [ ] SSE notifiche funzionanti in tempo reale senza refresh
- [ ] Gestione token scaduto: interceptor Axios chiama `/auth/refresh` automaticamente

---

## Sprint 10 — Security Hardening & Quality Final

**Obiettivo**: Revisione finale di sicurezza, performance e qualità globale prima della chiusura del progetto.

**Dipende da**: Sprint 9 ✅

### Attività
| ID | Attività | Riferimento |
|----|----------|-------------|
| SEC-1 | Rate limiting su endpoint login/register (es. Bucket4j) | OWASP Brute Force |
| SEC-2 | Content Security Policy via header Nginx | RNF_X_2 |
| SEC-3 | Audit finale Snyk — zero vulnerabilità HIGH/CRITICAL | CI Quality Gate |
| SEC-4 | GitGuardian: nessun segreto nel codebase | CI Quality Gate |
| SEC-5 | Revisione CORS: whitelist esplicita, no wildcard | tech_design.md |
| SEC-6 | Verifica che nessuna Entity JPA esca dal Service layer | DTO rigorosi |
| SEC-7 | Mutation Score finale PiTest ≥ 70% su tutto il Service layer | Qualità Test |
| SEC-8 | SonarCloud: zero Bug Critical/Major, zero vulnerabilità | Quality Gate CI |
| SEC-9 | Test E2E manuale dei 5 scenari del RAD (Scenario 1-5) | RAD §3.4 |
| SEC-10 | Revisione Dockerfiles multi-stage e docker-compose finale | |

### Test Scenari RAD (Verifica Manuale)
- **Scenario 1**: Tony consulta agenda giornaliera e crea prenotazione manuale → appare immediatamente
- **Scenario 2**: Marco invia richiesta → Tony riceve notifica → accetta → Marco riceve conferma
- **Scenario 3**: Luigi (CLG) prenota senza account → inserisce dati ospite → sistema conferma ricezione
- **Scenario 4**: Marco annulla prenotazione confermata con motivazione → slot liberato, Tony notificato
- **Scenario 5**: Tony configura salone da zero (servizi, poltrone, orari) → dashboard popolata

---

## Riepilogo Sprint — Tabella Complessiva

| Sprint | Tema | RF Coperti | Dipende da | Priorità |
|--------|------|------------|------------|----------|
| **S0** | Infrastruttura | — | — | 🔴 Critico |
| **S1** | Auth & Utenti | RF_GEN_1/2/3 | S0 | 🔴 Critico |
| **S2** | Catalogo Servizi | RF_BAR_6/7/8, RF_CLI_1 | S1 | 🔴 Critico |
| **S3** | Poltrone | RF_BAR_3/4/5, RF_CLI_2 | S1 | 🔴 Critico |
| **S4** | Orari & Disponibilità | RF_BAR_9/10, RF_CLI_3/4 | S2, S3 | 🔴 Critico |
| **S5** | Prenotazioni Core | RF_CLI_5/6, RF_CLG_1, RF_BAR_11-15, RF_CLR_4 | S4 | 🔴 Critico |
| **S6** | Notifiche SSE | RF_GEN_5, RF_BAR_16, RF_CLR_7 | S5 | 🔴 Critico |
| **S7** | Dashboard & Storico | RF_BAR_1/2, RF_CLR_1/2/3 | S5, S6 | 🟡 Importante |
| **S8** | Feature Avanzate | RF_GEN_4, RF_CLR_5/6, RF_CLG_2 | S5, S7 | 🟢 Supporto |
| **S9** | Frontend React | Tutte le schermate | S1-S8 | 🔴 Critico |
| **S10** | Security Hardening | RNF sicurezza + quality gate finale | S9 | 🟡 Importante |

### Copertura Requisiti Funzionali

| Requisito | Sprint |
|-----------|--------|
| RF_GEN_1 | S1 |
| RF_GEN_2 | S1 |
| RF_GEN_3 | S1 |
| RF_GEN_4 | S8 |
| RF_GEN_5 | S6 |
| RF_BAR_1 | S7 |
| RF_BAR_2 | S7 |
| RF_BAR_3 | S3 |
| RF_BAR_4 | S3 |
| RF_BAR_5 | S3 |
| RF_BAR_6 | S2 |
| RF_BAR_7 | S2 |
| RF_BAR_8 | S2 |
| RF_BAR_9 | S4 |
| RF_BAR_10 | S4 |
| RF_BAR_11 | S5 |
| RF_BAR_12 | S5 |
| RF_BAR_13 | S5 |
| RF_BAR_14 | S5 |
| RF_BAR_15 | S5 |
| RF_BAR_16 | S6 |
| RF_CLI_1 | S2 |
| RF_CLI_2 | S3 |
| RF_CLI_3 | S4 |
| RF_CLI_4 | S4 |
| RF_CLI_5 | S5 |
| RF_CLI_6 | S5 |
| RF_CLR_1 | S7 |
| RF_CLR_2 | S7 |
| RF_CLR_3 | S7 |
| RF_CLR_4 | S5 |
| RF_CLR_5 | S8 |
| RF_CLR_6 | S8 |
| RF_CLR_7 | S6 |
| RF_CLG_1 | S5 |
| RF_CLG_2 | S8 |

---

## Convenzioni di Sviluppo

### Branching Strategy
```
main           ← produzione stabile, protetto
develop        ← integrazione, protetto
feature/S{n}-{descrizione}   ← branch per ogni user story
```

**Regola**: ogni feature branch deve passare il CI completo prima del merge su `develop`. Il merge su `main` avviene solo a sprint completato con tutti i quality gate verdi.

### Naming Conventions
| Artefatto | Convenzione |
|-----------|-------------|
| Migrazione Flyway | `V{n}__{descrizione_snake_case}.sql` |
| Branch feature | `feature/S{sprint}-{cosa}` (es. `feature/S1-jwt-auth`) |
| DTO Request | `{Entity}RequestDto` |
| DTO Response | `{Entity}ResponseDto` |
| Mapper | `{Entity}Mapper` (interfaccia MapStruct) |
| Exception | `{Entity}{Motivo}Exception` (es. `BookingNotFoundException`) |
| Evento di dominio | `{Entity}{Azione}Event` (es. `BookingAcceptedEvent`) |
| Test unit | `{ClasseTestata}Test` |
| Test integration | `{ClasseTestata}IntegrationTest` |

### Commit Message Convention (Conventional Commits)
```
feat(S1): implement JWT authentication with refresh token rotation
fix(S5): prevent double booking race condition via optimistic lock
test(S4): add 10 unit tests for StandardAvailabilityStrategy
chore(S0): configure GitHub Actions CI pipeline
```

---

*Piano elaborato il 22/04/2026 — In attesa di approvazione*
