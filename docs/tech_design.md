# Tech Design Document — BarberBook
> Documento di progettazione tecnica  
> Autore: Gianfranco Barba | Revisione: 0.1 | Data: 15/04/2026  
> Stato: **IN REVISIONE** — in attesa di approvazione

---

## Indice
1. [Executive Summary](#1-executive-summary)
2. [Vincoli e Principi Guida](#2-vincoli-e-principi-guida)
3. [Stack Tecnologico — Scelte e Rationale](#3-stack-tecnologico--scelte-e-rationale)
   - 3.1 Backend
   - 3.2 Frontend
   - 3.3 Database
   - 3.4 Comunicazione Real-Time
4. [Architettura del Sistema](#4-architettura-del-sistema)
5. [Strategia di Testing](#5-strategia-di-testing)
6. [Pipeline DevOps e Quality Gates](#6-pipeline-devops-e-quality-gates)
7. [Sicurezza](#7-sicurezza)
8. [Containerizzazione (Docker)](#8-containerizzazione-docker)
9. [Roadmap di Implementazione Modulare](#9-roadmap-di-implementazione-modulare)
10. [Riepilogo Stack — Vista d'insieme](#10-riepilogo-stack--vista-dinsieme)

---

## 1. Executive Summary

BarberBook è un gestionale web per barbieri. Il sistema ha due macro-aree funzionali:
- **Gestionale (BAR)**: dashboard, agenda, servizi, poltrone, orari, gestione richieste.
- **Portale Clienti (CLR/CLG)**: vetrina pubblica, prenotazioni, notifiche, storico.

La progettazione adotta un approccio **"Solid by Design"**: ogni modulo viene implementato e testato prima di procedere al successivo, con quality gate automatizzati che impediscono il merge di codice che non rispetta gli standard qualitativi definiti.

---

## 2. Vincoli e Principi Guida

| Vincolo | Valore |
|---------|--------|
| Backend | **Java** (obbligatorio) |
| Frontend | Libero — raccomandato React |
| Database | Libero — valutato PostgreSQL |
| Testing | JUnit 5, Mockito, JaCoCo, PiTest (JMH opzionale) |
| DevOps | Docker, GitHub Actions, SonarQube, Snyk, GitGuardian |
| Lingua UI | Italiano (RNF_X_1) |
| Sicurezza | Autenticazione sicura, gestione password, no bypass |
| Approccio | Modulo per modulo + test completi prima del next module |

**Principi guida adottati:**
- **Test-First Mindset**: ogni modulo è testabile in isolamento.
- **Fail Fast**: il CI blocca il merge se i quality gate non passano.
- **Security by Design**: la sicurezza è un requisito architetturale, non un add-on.
- **Separation of Concerns**: backend e frontend sono indipendenti e comunicano solo via REST API.

---

## 3. Stack Tecnologico — Scelte e Rationale

### 3.1 Backend — Spring Boot 3.x (Java 21)

**Scelta: Spring Boot 3.3.x con Java 21 LTS**

#### Rationale
Spring Boot è la scelta naturale per un backend Java professionale nel 2026:

| Criterio | Spring Boot 3.x | Alternative valutate |
|----------|-----------------|----------------------|
| Maturità ecosistema | Ottimo | Quarkus Molto Buono, Micronaut Buono |
| Testing support nativo | Spring Boot Test, @DataJpaTest, MockMvc | Buono anche su Quarkus |
| Community & documentazione | Eccellente | Buona |
| Integrazione DevOps tools | Actuator, Prometheus, Micrometer nativi | Comparabile |
| Curva di apprendimento | Ottima (già noto in ambito accademico) | Quarkus più ripido |
| Supporto Java 21 VT | Sì (Virtual Threads via Tomcat) | Sì (Quarkus) |

> **Perché non Quarkus?** Quarkus nasce per ambienti GraalVM native e serverless. Per un progetto di tirocinio con testing approfondito, Spring Boot offre un ecosistema di test più maturo e documentazione substantially migliore.

#### Dipendenze principali Spring Boot

```
spring-boot-starter-web          → REST API con Jackson
spring-boot-starter-data-jpa     → ORM con Hibernate 6
spring-boot-starter-security     → autenticazione e autorizzazione
spring-boot-starter-validation   → Bean Validation (input sanitization)
spring-boot-starter-actuator     → health, metrics, monitoring
spring-boot-starter-websocket    → notifiche real-time (o SSE)
flyway-core                      → database migrations versionabili
jjwt-api + jjwt-impl            → JSON Web Tokens
lombok                           → riduzione boilerplate
mapstruct                        → mapping DTO ↔ Entity
```

#### Struttura dei package (Layered + Domain-Oriented)

```
com.barberbook/
├── config/           → SecurityConfig, CorsConfig, WebSocketConfig
├── domain/           → Entità JPA (pure, senza logica di presentazione)
│   ├── model/        → Booking, Service, Chair, Schedule, User, Notification
│   └── enums/        → BookingStatus, UserRole, DayOfWeek
├── repository/       → Spring Data JPA Repositories
├── service/          → Logica di business (testabile in isolamento)
│   ├── BookingService.java
│   ├── AvailabilityService.java     ← logica slot più complessa
│   ├── NotificationService.java
│   └── AuthService.java
├── controller/       → REST Controllers (@RestController)
├── dto/              → Request/Response DTO (separati dalle entity)
├── security/         → JwtFilter, JwtUtil, UserDetailsServiceImpl
├── exception/        → GlobalExceptionHandler, custom exceptions
└── util/             → helper, slot calculator
```

---

### 3.2 Frontend — React 18 + Vite + TypeScript

**Scelta: React 18 + Vite 6 + TypeScript 5**

#### Rationale

Considerato che:
- Il backend è uno Spring Boot REST API separato.
- BarberBook è un'app **autenticata** (dashboard BAR) + una sezione **quasi-pubblica** (portale cliente, accessibile senza login ma non SEO-critical).
- Non serve SSR né SEO in senso stretto.
- La semplicità di setup e la velocità di sviluppo sono prioritari.

| Criterio | React + Vite (SPA) | Next.js 15 |
|----------|-------------------|------------|
| Separazione BE/FE | Perfetta | Tende a mescolare |
| Setup e semplicità | Eccellente | Complesso (App Router) |
| Performance HMR | Migliore (Vite è il più veloce) | Ottimo |
| Deploy (CDN statico) | Ottimo (nginx sul Docker) | Richiede Node runtime |
| Learning curve | Ottima | Media |
| SEO | Non necessario | Non necessario |

**Verdetto: React + Vite è la scelta più pragmatica, pulita e professionale per questo caso d'uso.**

#### Librerie Frontend

| Categoria | Libreria | Motivo |
|-----------|----------|--------|
| Build tool | **Vite 6** | HMR velocissimo, bundling moderno |
| Language | **TypeScript 5** | Type safety end-to-end |
| Routing | **React Router v7** | Standard de facto per SPA React |
| Server state | **TanStack Query v5** | Caching, invalidazione, loading states |
| Client state | **Zustand** | Leggero, semplice, no boilerplate Redux |
| HTTP client | **Axios** | Interceptors per JWT, error handling |
| UI components | **shadcn/ui** | Componenti accessibili, personalizzabili, no lock-in vendor |
| Styling | **Tailwind CSS v4** | Utility-first, ottimo con shadcn |
| Forms | **React Hook Form + Zod** | Validazione type-safe, performance |
| Date/Time | **date-fns** | Leggero, tree-shakable |
| Real-time | **native EventSource** | Per notifiche SSE in-app |
| Icons | **Lucide React** | Design coerente, leggero |

> **Nota su shadcn/ui**: a differenza di Material UI o Ant Design, shadcn/ui non è una dipendenza vera — i componenti vengono copiati nel progetto e sono completamente controllabili. Ideale per personalizzazione totale dell'UI in italiano.

---

### 3.3 Database — PostgreSQL 16

**Scelta: PostgreSQL 16**

#### Rationale

Il requisito **RNF_R_1 (no double-booking)** è il driver principale della scelta del database.

| Criterio | PostgreSQL 16 | MySQL 8 | SQLite |
|----------|--------------|---------|--------|
| ACID completo | Eccellente | Molto Buono | Buono |
| Exclusion Constraints (GiST) | Nativi per intervalli temporali | Non disponibile | Non disponibile |
| Row-level locking | Eccellente | Molto Buono | Scarso |
| JSON support | Eccellente | Buono | Scarso |
| Integrazione Spring JPA | Eccellente | Eccellente | Buono |
| Concorrenza | MVCC (excellente) | MVCC | Scarsa |
| Tooling | pgAdmin, psql | MySQL Workbench | — |

**Punto chiave**: PostgreSQL supporta nativamente gli **Exclusion Constraints con GiST** per intervalli temporali, che rappresentano la soluzione più robusta al problema del double-booking:

```sql
-- Esempio: nessuna prenotazione può sovrapporsi sulla stessa poltrona
ALTER TABLE prenotazioni
ADD CONSTRAINT no_overlap
EXCLUDE USING GIST (
  chair_id WITH =,
  tstzrange(start_time, end_time) WITH &&
)
WHERE (status IN ('IN_ATTESA', 'ACCETTATA'));
```

Questo constraint è **garantito a livello di database** — indipendente dall'applicazione, immune da race conditions.

#### Strategia di Migrazione: Flyway

Le migrazioni del database sono gestite con **Flyway** (integrato in Spring Boot):
- Versioni versionabili in `src/main/resources/db/migration/`
- Nomenclatura: `V1__init_schema.sql`, `V2__add_chairs.sql`, ecc.
- Le migrazioni vengono eseguite automaticamente all'avvio dell'applicazione.
- Testcontainers in CI usa un PostgreSQL reale per i test di integrazione.

---

### 3.4 Comunicazione Real-Time — Server-Sent Events (SSE)

**Scelta: Spring SSE (Server-Sent Events) via SseEmitter**

Per le notifiche in-app in real-time (RF_GEN_5, RF_BAR_16, RF_CLR_7).

| Criterio | SSE (SseEmitter) | WebSocket |
|----------|-----------------|-----------|
| Semplicità implementazione | Eccellente | Media |
| Unidirezionale (server→client) | Perfetto per notifiche | Bidirezionale (overkill) |
| Reconnect automatico | Nativo browser | Manuale |
| Compatibilità browser | Eccellente | Molto buona |
| Spring integration | Nativo con SseEmitter | Richiede STOMP/broker setup |
| Overhead | Molto basso | Più alto |

> **Perché non WebSocket?** BarberBook necessita solo di notifiche server→client (unidirezionali). SSE è più semplice, nativo nel browser, con reconnect automatico. WebSocket sarebbe overkill e richiederebbe un message broker (es. RabbitMQ) per scalare correttamente.

---

## 4. Architettura del Sistema

```
CLIENT LAYER
  React + Vite + TypeScript (SPA)
    Dashboard BAR (autenticato) | Portale Cliente (semi-pubblico)
    Axios + TanStack Query
    SSE EventSource (notifiche)
          |
          | HTTPS / REST JSON + SSE stream
          |
API GATEWAY (Nginx)
  Reverse Proxy: /api/* → Spring Boot | /* → React (static files)
          |
BACKEND LAYER (Spring Boot 3)
  REST Controllers | Security Filter (JWT) | SSE Controller
          |
  SERVICE LAYER
    BookingService | AvailabilityService | NotificationService
    AuthService | ChairService | ServiceCatalogService
          |
  REPOSITORY LAYER (Spring Data JPA)
          |
          | JDBC
          |
DATA LAYER (PostgreSQL 16)
  Tables: users | chairs | services | bookings | schedules | breaks | notifications | refresh_tokens
  Constraints: Exclusion GiST (no overlap), FK, Unique, Check
```

### Pattern Architetturali Adottati

| Pattern | Applicazione |
|---------|-------------|
| **Layered Architecture** | Controller → Service → Repository → DB |
| **DTO Pattern** | Nessuna entity esposta direttamente via API |
| **Repository Pattern** | Spring Data JPA come astrazione |
| **Strategy Pattern** | Calcolo disponibilità slot (AvailabilityService) |
| **Observer Pattern** | Notifiche via SSE (publish/subscribe interno) |
| **Guard Clauses** | Validazione input nei service layer |

---

## 5. Strategia di Testing

La strategia segue la **Testing Pyramid** e il principio **"test prima di avanzare"**.

```
                  ┌──────────────┐
                  │  E2E Tests   │  ← opzionale/futuro
                  │   (pochi)    │
              ┌───┴──────────────┴───┐
              │  Integration Tests   │  ← Testcontainers + MockMvc
              │    (moderati)        │
          ┌───┴──────────────────────┴───┐
          │       Unit Tests             │  ← JUnit 5 + Mockito
          │  (molti — base della piramide)│
          └──────────────────────────────┘
```

### 5.1 Unit Testing — JUnit 5 + Mockito + AssertJ

**Scope**: Logica di business in isolamento (Service layer principalmente).

```java
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock AvailabilityService availabilityService;
    @InjectMocks BookingService bookingService;

    @Test
    @DisplayName("createRequest: deve lanciare eccezione se slot non disponibile")
    void shouldThrowWhenSlotNotAvailable() {
        given(availabilityService.isSlotAvailable(any())).willReturn(false);
        assertThatThrownBy(() -> bookingService.createRequest(any()))
            .isInstanceOf(SlotNotAvailableException.class);
    }
}
```

**Librerie**:
- `JUnit 5 (Jupiter)` — framework principale
- `Mockito 5` — mocking
- `AssertJ` — asserzioni fluenti (più espressive di JUnit assertions)
- `Mockito-BDD` — stile given/when/then

**Target copertura**: ≥ 80% line coverage sui Service layer (enforced da JaCoCo quality gate).

### 5.2 Integration Testing — Testcontainers + Spring Boot Test

**Scope**: Test con database PostgreSQL reale (non H2 in-memory).

> **Perché Testcontainers e non H2?** H2 non supporta i costrutti PostgreSQL-specifici (Exclusion Constraints GiST). I test con H2 possono passare ma fallire in produzione. Testcontainers avvia un container Docker PostgreSQL reale durante i test.

```java
@SpringBootTest
@Testcontainers
class BookingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
    }

    @Test
    void shouldPreventDoubleBookingOnSameSlot() {
        // test con DB reale — verifica Exclusion Constraint
    }
}
```

**Scope aggiuntivo**:
- `@WebMvcTest` + `MockMvc` per i Controller (senza DB)
- `@DataJpaTest` + Testcontainers per Repository layer

### 5.3 Coverage — JaCoCo

**Soglie minime configurate in Maven**:

```xml
<limit>
    <counter>LINE</counter>
    <value>COVEREDRATIO</value>
    <minimum>0.80</minimum>  <!-- 80% minimo, build fallisce sotto -->
</limit>
<limit>
    <counter>BRANCH</counter>
    <value>COVEREDRATIO</value>
    <minimum>0.75</minimum>
</limit>
```

Il report JaCoCo (XML) è integrato nel workflow CI e inviato a SonarCloud.

**Esclusioni dal coverage** (no senso testarle):
- DTO (record/POJO puri)
- Classi di configurazione Spring (`@Configuration`)
- Entry point (`main()`)
- Enum semplici

### 5.4 Mutation Testing — PiTest

PiTest introduce mutazioni nel bytecode e verifica che i test le rilevano.

```xml
<configuration>
    <targetClasses>
        <param>com.barberbook.service.*</param>
    </targetClasses>
    <mutationThreshold>70</mutationThreshold>
    <coverageThreshold>80</coverageThreshold>
</configuration>
```

**Target**: Mutation Score ≥ 70% sul Service layer.  
**Frequenza**: Eseguito in CI solo sul branch `main` (non su ogni PR — troppo lento).

### 5.5 Microbenchmarking — JMH (Priorità Bassa)

**Scope**: Performance della logica di calcolo degli slot disponibili (AvailabilityService).

```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class AvailabilityBenchmark {
    @Benchmark
    public List<TimeSlot> calculateAvailableSlots(BenchmarkState state) {
        return state.availabilityService.getAvailableSlots(...);
    }
}
```

**Attivazione**: Solo manualmente (`mvn jmh:run`), non nel CI standard.

### 5.6 Frontend Testing (React)

| Livello | Tool | Scope |
|---------|------|-------|
| Unit | **Vitest** | Funzioni utility, hook custom |
| Component | **React Testing Library** | Componenti UI in isolamento |
| API mock | **MSW (Mock Service Worker)** | Mock delle API REST in test |

---

## 6. Pipeline DevOps e Quality Gates

### 6.1 Struttura del Repository

```
barberbook/
├── backend/              → Maven project (Spring Boot)
├── frontend/             → Vite project (React + TS)
├── docker/               → Dockerfile, nginx config
├── docker-compose.yml
├── .github/
│   └── workflows/
│       ├── ci-backend.yml     → build + test + coverage + sonar
│       ├── ci-frontend.yml    → lint + test + build
│       └── security-scan.yml  → Snyk
└── docs/                 → RAD, tech_design, altri documenti
```

### 6.2 Workflow GitHub Actions — CI Backend (semplificato)

```yaml
name: CI Backend
on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0   # necessario per SonarQube

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Build & Test con JaCoCo
        run: mvn -B verify jacoco:report
        working-directory: backend

      - name: SonarCloud Analysis
        uses: SonarSource/sonarqube-scan-action@v5
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}

      - name: Snyk Security Scan
        uses: snyk/actions/maven@master
        with:
          args: --severity-threshold=high
        env:
          SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
```

### 6.3 Quality Gates — Definizione

Un Quality Gate blocca il merge se anche una sola condizione non è soddisfatta.

| Gate | Tool | Soglia | Blocca PR? |
|------|------|--------|------------|
| Line Coverage ≥ 80% | JaCoCo + SonarCloud | 80% | Sì |
| Branch Coverage ≥ 75% | JaCoCo + SonarCloud | 75% | Sì |
| Nessun nuovo Bug Critical/Major | SonarCloud | 0 | Sì |
| Nessun Code Smell bloccante | SonarCloud | 0 | Sì |
| Nessuna vulnerabilità HIGH/CRITICAL | Snyk | 0 | Sì |
| Nessun segreto nel codice | GitGuardian | 0 | Sì |
| Build success | Maven | pass | Sì |
| Tutti i test passano | JUnit 5 | 100% | Sì |
| Mutation Score ≥ 70% | PiTest | 70% | Solo branch main |

### 6.4 Tool DevOps — Dettaglio

#### SonarCloud (raccomandato rispetto a SonarQube self-hosted)
- Analisi statica del codice Java
- Ricezione report JaCoCo e visualizzazione coverage
- Dashboard qualitativa per ogni modulo
- **Vantaggio su SonarQube Community**: gratuito per progetti open source, supporta PR decoration su ogni branch (non solo main), zero infrastruttura da gestire.

#### Snyk
- Scansione dipendenze Maven per vulnerabilità CVE.
- Blocca il CI se trova vulnerabilità HIGH o CRITICAL.
- Suggerisce la versione sicura della libreria.

#### GitGuardian
- Scansione di ogni commit per segreti accidentalmente inclusi (password, API key, token).
- Si integra come GitHub App — zero configurazione nel workflow.
- Blocca la PR se rileva un segreto.

---

## 7. Sicurezza

### 7.1 Autenticazione — JWT + Refresh Token Rotation

```
Client                          Server
  │                               │
  │── POST /auth/login ──────────▶│ verifica BCrypt
  │                               │ genera Access Token (15 min)
  │◀── { accessToken,  ──────────│ genera Refresh Token (7 giorni)
  │      refreshToken }           │ salva RT hashed in DB
  │                               │
  │── API Request ───────────────▶│
  │  Authorization: Bearer {AT}   │ verifica firma JWT
  │◀── Response ─────────────────│
  │                               │
  │── POST /auth/refresh ────────▶│ [AT scaduto]
  │      {refreshToken}           │ cerca RT in DB
  │                               │ invalida RT vecchio (rotazione)
  │◀── { nuovo AT, nuovo RT } ───│ emette nuovi token
  │                               │
  │── POST /auth/logout ─────────▶│
  │      {refreshToken}           │ cancella RT dal DB
  │◀── 200 OK ────────────────────│
```

**Dettagli**:
- **Access Token**: 15 minuti, HMAC-SHA256, payload: `userId`, `role`, `email`.
- **Refresh Token**: 7 giorni, salvato come SHA-256 hash in DB, mai esposto in chiaro dopo il primo invio.
- **Refresh Token Rotation**: ogni utilizzo genera una coppia nuova e invalida il precedente. RT già invalidato presentato → segnale di furto → invalida tutta la sessione dell'utente.
- **Storage client**: Access Token in memoria (Zustand), Refresh Token in cookie `HttpOnly; Secure; SameSite=Strict`.

### 7.2 Password Hashing — BCrypt

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);  // strength 12 ≈ 300ms/hash
}
```

Strength 12 è abbastanza lento da scoraggiare brute-force, accettabile per l'UX.

### 7.3 Autorizzazione — RBAC

```java
@PreAuthorize("hasRole('BARBER')")
ResponseEntity<?> acceptBooking(...) { ... }

@PreAuthorize("hasRole('CLIENT') or hasRole('BARBER')")
ResponseEntity<?> getAvailableSlots(...) { ... }
```

| Ruolo | Entità | Descrizione |
|-------|--------|-------------|
| `ROLE_BARBER` | BAR | Accesso completo al gestionale |
| `ROLE_CLIENT` | CLR | Accesso al portale cliente |
| Guest (anonimo) | CLG | Solo endpoint pubblici |

### 7.4 Altre Misure di Sicurezza

| Misura | Implementazione |
|--------|----------------|
| CORS | Origini whitelist esplicite (no wildcard) |
| HTTPS | TLS termination via Nginx |
| SQL Injection | Impossibile con JPA/Hibernate (query parametrizzate) |
| XSS | Content Security Policy via Nginx headers |
| Input Validation | @Valid + Bean Validation su tutti i DTO |
| Secrets | Variabili d'ambiente via Docker / .env (mai hardcoded) |

---

## 8. Containerizzazione (Docker)

### 8.1 Docker Compose (Development)

```yaml
version: '3.9'
services:

  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: barberbook
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER}"]

  backend:
    build: ./backend
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/barberbook
      JWT_SECRET: ${JWT_SECRET}
    ports:
      - "8080:8080"

  frontend:
    build: ./frontend
    ports:
      - "3000:80"

  nginx:
    image: nginx:alpine
    volumes:
      - ./docker/nginx.conf:/etc/nginx/nginx.conf
    ports:
      - "80:80"
      - "443:443"

  sonarqube:
    image: sonarqube:lts-community
    ports:
      - "9000:9000"
    profiles:
      - "dev-tools"  # solo in sviluppo locale

volumes:
  postgres_data:
```

### 8.2 Dockerfile Backend (Multi-stage)

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw package -DskipTests

# Stage 2: Runtime (immagine minimale)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 8.3 Dockerfile Frontend (Multi-stage)

```dockerfile
# Stage 1: Build
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: Serve con Nginx
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY docker/nginx-frontend.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

---

## 9. Roadmap di Implementazione Modulare

Ogni fase termina con test completi (unit + integration) prima di passare alla successiva.

| Fase | Modulo | Componenti | Test richiesti |
|------|--------|-----------|----------------|
| **0** | Setup Infrastruttura | Docker, DB, Flyway, GitHub Actions, SonarCloud setup | Pipeline CI verde |
| **1** | Auth & Utenti | Registrazione CLR, Login BAR/CLR, Logout, JWT, BCrypt | Unit (AuthService), Integration (DB + API) |
| **2** | Catalogo Servizi | CRUD servizi (RF_BAR_6/7/8), Vetrina pubblica (RF_CLI_1) | Unit + Integration |
| **3** | Poltrone | CRUD poltrone (RF_BAR_3/4/5), Visualizzazione (RF_CLI_2) | Unit + Integration |
| **4** | Orari e Disponibilità | Orari apertura (RF_BAR_9), Pause (RF_BAR_10), Calcolo slot (RF_CLI_3/4) | Unit intensivi su AvailabilityService, Integration, JMH |
| **5** | Prenotazioni (core) | Flusso richiesta CLR/CLG, Accetta/Rifiuta BAR, CRUD diretto BAR, Annullamento CLR | Unit, Integration (test no-double-booking), PiTest |
| **6** | Notifiche | SSE in-app BAR e CLR (RF_GEN_5, RF_BAR_16, RF_CLR_7) | Unit (NotificationService), Integration SSE |
| **7** | Dashboard e Storico | Dashboard settimanale/giornaliera BAR, Storico CLR, Homepage CLR | Unit + Integration |
| **8** | Funzionalità accessorie | Recupero password, Modifica profilo, Riprenotazione rapida, Registrazione post-CLG | Unit + Integration |
| **9** | Frontend completo | React (sviluppo parallelo al BE dalla Fase 2 con MSW per mock API) | Vitest + RTL |
| **10** | Security Hardening | Revisione OWASP, rate limiting, CSP, audit Snyk | Security review |

---

## 10. Riepilogo Stack — Vista d'insieme

| Layer | Tecnologia |
|-------|-----------|
| **Backend** | Java 21 LTS + Spring Boot 3.3.x + Spring Security + Spring Data JPA + Flyway |
| **Frontend** | React 18 + Vite 6 + TypeScript 5 + React Router v7 + TanStack Query + Zustand + shadcn/ui + Tailwind CSS v4 |
| **Database** | PostgreSQL 16 (Exclusion Constraints GiST per no double-booking) |
| **Real-time** | Spring SSE (Server-Sent Events) |
| **Testing BE** | JUnit 5 + Mockito 5 + AssertJ + Testcontainers + JaCoCo + PiTest + JMH (low priority) |
| **Testing FE** | Vitest + React Testing Library + MSW |
| **DevOps** | GitHub Actions + SonarCloud + Snyk + GitGuardian + Docker + Docker Compose + Nginx |
| **Sicurezza** | JWT (15 min) + Refresh Token Rotation (HttpOnly cookie) + BCrypt(12) + RBAC |

---

## Punti Aperti — Richiesta Approvazione

> [!IMPORTANT]
> Le seguenti scelte richiedono conferma esplicita prima di iniziare la Fase 0.

| # | Decisione | Opzione scelta | Alternativa |
|---|-----------|---------------|-------------|
| 1 | **SonarCloud vs SonarQube self-hosted** | **SonarCloud** (gratuito, PR decoration, zero infra) | SonarQube in Docker (più controllo, solo branch main) |
| 2 | **Struttura repository** | **Monorepo** (backend/ + frontend/ nella stessa repo) | Due repo separate |
| 3 | **SSE vs WebSocket** | **SSE** (più semplice, unidirezionale) | WebSocket (bidirezionale, più complesso) |
| 4 | **Build tool backend** | **Maven** (standard Java, ottima integrazione CI) | Gradle |
| 5 | **JMH** | **Integrato, eseguito manualmente** | Escluso completamente |
| 6 | **Frontend testing** | **Vitest + RTL + MSW** | Solo test manuali (non raccomandato) |

---

*Documento generato: 15/04/2026 — In attesa di approvazione per procedere alla Fase 0*
