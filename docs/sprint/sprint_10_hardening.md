# Sprint 10 — Security Hardening & Quality Final
> **Stato**: ⬜ Non iniziato  
> **Dipende da**: Sprint 9 ✅  
> **Obiettivo**: Revisione finale di sicurezza, performance e qualità globale. Questo sprint non aggiunge feature — consolida, rafforza e certifica che il sistema è sicuro, manutenibile e pronto per il rilascio.

---

## Indice Fasi

1. [Fase 10.1 — Rate Limiting (Brute Force Protection)](#fase-101--rate-limiting-brute-force-protection)
2. [Fase 10.2 — Security Headers & CORS Review](#fase-102--security-headers--cors-review)
3. [Fase 10.3 — Audit Finale Snyk & GitGuardian](#fase-103--audit-finale-snyk--gitguardian)
4. [Fase 10.4 — SonarCloud Quality Gate Finale](#fase-104--sonarcloud-quality-gate-finale)
5. [Fase 10.5 — PiTest Mutation Testing Finale](#fase-105--pitest-mutation-testing-finale)
6. [Fase 10.6 — Docker Multi-Stage & Compose Finale](#fase-106--docker-multi-stage--compose-finale)
7. [Fase 10.7 — Test E2E Scenari RAD](#fase-107--test-e2e-scenari-rad)
8. [Fase 10.8 — Revisione Architetturale Finale](#fase-108--revisione-architetturale-finale)
9. [Fase 10.9 — Documentazione Finale](#fase-109--documentazione-finale)
10. [Fase 10.10 — Verifica Quality Gate Totale](#fase-1010--verifica-quality-gate-totale)

---

## Fase 10.1 — Rate Limiting (Brute Force Protection)

**Obiettivo**: Proteggere gli endpoint di autenticazione da attacchi brute force e abusi.

### Dipendenza da aggiungere (`pom.xml`)
```xml
<dependency>
    <groupId>com.github.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.x.x</version>
</dependency>
```

### `RateLimitFilter.java`
```java
/**
 * Filtro rate limiting per endpoint sensibili.
 * Algoritmo: Token Bucket (Bucket4j)
 * Limiti:
 *   - POST /api/auth/login: 5 tentativi per minuto per IP
 *   - POST /api/auth/register: 3 tentativi per minuto per IP
 *   - POST /api/auth/forgot-password: 3 tentativi ogni 5 minuti per IP
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    // Cache IP → Bucket (thread-safe)
    private final Cache<String, Bucket> loginBuckets = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.MINUTES)
        .build();

    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/forgot-password"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equals(method) && RATE_LIMITED_PATHS.contains(path)) {
            String clientIp = getClientIp(request);
            Bucket bucket = loginBuckets.get(clientIp, this::createBucketForPath);

            if (!bucket.tryConsume(1)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                    """
                    {
                        "error": "Troppi tentativi. Attendi prima di riprovare.",
                        "retryAfter": 60
                    }
                    """
                );
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private Bucket createBucketForPath() {
        // Default: 5 token, ricarica 5 ogni minuto
        return Bucket.builder()
            .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))))
            .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isEmpty())
            ? xff.split(",")[0].trim()
            : request.getRemoteAddr();
    }
}
```

> **Nota**: Bucket4j richiede anche Caffeine come cache in-memory oppure Redis per deploy scalato. In questo progetto usiamo Caffeine (in-memory) coerentemente con un'architettura monolite.

### Attività
- [ ] Aggiungere dipendenze `bucket4j-core` e `caffeine` al `pom.xml`
- [ ] Creare `RateLimitFilter` e registrarlo prima di `JwtAuthFilter`
- [ ] Testare: 6 tentativi di login in 60 secondi → il 6° riceve `429 Too Many Requests`
- [ ] Verificare che il rate limiting operi per IP, non per sessione

---

## Fase 10.2 — Security Headers & CORS Review

**Obiettivo**: Configurare gli header HTTP di sicurezza standard e verificare la configurazione CORS.

### Security Headers via Nginx

Aggiornare `docker/nginx.conf`:
```nginx
server {
    listen 80;

    # ========================
    # Security Headers
    # ========================
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Permissions-Policy "camera=(), microphone=(), location=()" always;
    add_header Content-Security-Policy
        "default-src 'self';
         script-src 'self';
         style-src 'self' 'unsafe-inline' https://fonts.googleapis.com;
         font-src 'self' https://fonts.gstatic.com;
         img-src 'self' data:;
         connect-src 'self';
         frame-ancestors 'none';" always;

    # HSTS (solo se HTTPS in produzione)
    # add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # ========================
    # Proxy Backend
    # ========================
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Frontend SPA
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;  # SPA routing
    }
}
```

### CORS Review in Spring Boot
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // WHITELIST ESPLICITA: nessun wildcard in produzione
    configuration.setAllowedOrigins(List.of(
        "http://localhost:3000",      // dev
        "https://barberbook.app"      // produzione (sostituire con dominio reale)
    ));

    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
    configuration.setAllowCredentials(true);   // necessario per cookie HttpOnly
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
}
```

### Checklist Security Headers
- [ ] `X-Frame-Options: DENY` → previene clickjacking
- [ ] `X-Content-Type-Options: nosniff` → previene MIME sniffing
- [ ] `Content-Security-Policy` configurata con allowlist esplicita
- [ ] `Referrer-Policy: strict-origin-when-cross-origin`
- [ ] CORS con whitelist esplicita (no wildcard `*`)
- [ ] Cookie RT: `HttpOnly`, `Secure`, `SameSite=Strict`
- [ ] Nessun endpoint espone stack trace in produzione

### Attività
- [ ] Aggiornare `docker/nginx.conf` con security headers
- [ ] Verificare configurazione CORS in `SecurityConfig`
- [ ] Verificare impostazioni cookie Refresh Token
- [ ] Testare con browser DevTools che gli header siano presenti

---

## Fase 10.3 — Audit Finale Snyk & GitGuardian

**Obiettivo**: Zero vulnerabilità HIGH/CRITICAL nelle dipendenze, zero segreti nel codice.

### Snyk Audit Backend
```bash
cd backend
mvn snyk:test
# Risultato atteso: 0 vulnerabilità HIGH o CRITICAL
```

### Snyk Audit Frontend
```bash
cd frontend
npx snyk test
# Risultato atteso: 0 vulnerabilità HIGH o CRITICAL
```

### Checklist GitGuardian
- [ ] Verificare nella console GitGuardian: 0 segreti rilevati
- [ ] Verificare `.gitignore`: `.env`, `*.key`, `*.pem` mai committati
- [ ] Verificare che `V3__seed_barber.sql` non contenga password in chiaro (solo hash BCrypt)
- [ ] Verificare `application-prod.yml`: tutti i valori sensibili da variabili d'ambiente `${VAR}`

### Checklist credenziali
```bash
# Verificare che nessun secret sia hardcoded
grep -r "password" backend/src/main/resources/ --include="*.yml"
grep -r "secret" backend/src/main/resources/ --include="*.yml"
# Risultato atteso: SOLO riferimenti a variabili ${...}
```

### Attività
- [ ] Eseguire `mvn snyk:test` → 0 HIGH/CRITICAL
- [ ] Eseguire `npx snyk test` nel frontend → 0 HIGH/CRITICAL
- [ ] Verificare GitGuardian dashboard → 0 segreti rilevati
- [ ] Aggiornare dipendenze con vulnerabilità MEDIUM se possibile

---

## Fase 10.4 — SonarCloud Quality Gate Finale

**Obiettivo**: Zero Bug/Vulnerability Critical e Major, nessun Code Smell bloccante.

### Quality Gate configurato su SonarCloud
```
Reliability Rating: ≥ B (0 Bug Critical/Major)
Security Rating:    ≥ B (0 Vulnerability Critical/Major)
Maintainability:    ≥ B
Coverage:           ≥ 80% su nuovo codice
Duplications:       ≤ 3%
```

### Checklist revisione finale SonarCloud
- [ ] 0 Bug Critical → no nullpointer non gestiti, no bad practice
- [ ] 0 Security Vulnerability → no SQL injection, no path traversal, no XSS
- [ ] Verifica Code Smells rimanenti: risolti se >10 minuti di tech debt ciascuno
- [ ] Coverage ≥ 80% su tutto il codice nuovo (escludendo DTO, config, entry point)

### Risoluzione issue comuni SonarCloud
| Issue tipo | Causa comune | Fix |
|------------|-------------|-----|
| Null Dereference | Optional non gestito | `.orElseThrow()` invece di `.get()` |
| Hardcoded Credentials | Costante con "password" o "secret" | Usare `@Value` da env |
| Weak Cryptography | MD5, SHA-1 | Usare BCrypt o SHA-256 |
| SQL Injection | Query concatenata | Usare `@Query` con parametri named |
| Unchecked cast | `(EntityType) object` | Instanceof check prima del cast |

### Attività
- [ ] Revisione dashboard SonarCloud
- [ ] Correzione tutti i Bug e Vulnerability Critical/Major
- [ ] Verifica coverage ≥ 80%

---

## Fase 10.5 — PiTest Mutation Testing Finale

**Obiettivo**: Verificare che il mutation score finale sia ≥ 70% sull'intero Service layer.

### Esecuzione finale PiTest
```bash
cd backend
mvn org.pitest:pitest-maven:mutationCoverage -DtimestampedReports=false
```

### Report in `target/pit-reports/index.html`
Verificare per ogni classe Service:

| Classe | Target Mutation Score | Note |
|--------|-----------------------|------|
| `BookingStatus` (enum) | ≥ 85% | State machine critica |
| `StandardAvailabilityStrategy` | ≥ 80% | Logica algoritmica critica |
| `AuthService` | ≥ 70% | Logica di autenticazione |
| `BookingService` | ≥ 70% | Facade business logic |
| `NotificationService` | ≥ 65% | Observer (più difficile da mutare) |
| `ServiceCatalogService` | ≥ 80% | CRUD — alta testabilità |

### Strategie per migliorare il mutation score
- Mutanti sopravvissuti su **condizioni boundary**: aggiungere test con valori limite esatti
- Mutanti sopravvissuti su **concatenazione di stringhe**: verificare il contenuto del messaggio nei test
- Mutanti sopravvissuti su **return values**: verificare il valore di ritorno, non solo l'assenza di eccezioni

### Attività
- [ ] Eseguire PiTest sul progetto completo
- [ ] Analizzare i mutanti sopravvissuti
- [ ] Aggiungere test mirati per i mutanti sopravvissuti fino a raggiungere ≥ 70%
- [ ] Riportare mutation score finale nel `README.md`

---

## Fase 10.6 — Docker Multi-Stage & Compose Finale

**Obiettivo**: Ottimizzare le immagini Docker per produzione.

### Dockerfile Backend — Multi-Stage ottimizzato
```dockerfile
# Stage 1: Dependency caching (ottimizza rebuild time)
FROM eclipse-temurin:21-jdk-alpine AS deps
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Stage 2: Build
FROM deps AS build
COPY src ./src
RUN ./mvnw package -DskipTests -q

# Stage 3: Runtime minimale
FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S barberbook && adduser -S barberbook -G barberbook
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER barberbook          # non eseguire come root
EXPOSE 8080
ENTRYPOINT ["java",
    "-XX:+UseContainerSupport",     # Rispetta i limiti CPU/RAM del container
    "-XX:MaxRAMPercentage=75.0",
    "-jar", "app.jar"]
```

### Dockerfile Frontend — Multi-Stage
```dockerfile
# Stage 1: Build React
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json .
RUN npm ci --quiet
COPY . .
RUN npm run build

# Stage 2: Nginx serve
FROM nginx:alpine AS runtime
COPY --from=build /app/dist /usr/share/nginx/html
COPY docker/nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
```

### `docker-compose.yml` finale
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
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER} -d barberbook"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  backend:
    build:
      context: ./backend
      dockerfile: docker/Dockerfile
      target: runtime
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/barberbook
      DB_USER: ${DB_USER}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      SPRING_PROFILES_ACTIVE: prod
    ports:
      - "8080:8080"
    restart: unless-stopped
    mem_limit: 512m

  frontend:
    build:
      context: ./frontend
      target: runtime
    ports:
      - "80:80"
    depends_on:
      - backend
    restart: unless-stopped

volumes:
  postgres_data:
```

### Attività
- [ ] Aggiornare Dockerfile backend con user non-root e JVM flags
- [ ] Creare Dockerfile frontend multi-stage
- [ ] Aggiornare `docker-compose.yml` con `mem_limit` e `restart: unless-stopped`
- [ ] Verificare: `docker-compose up --build` → stack completo funzionante

---

## Fase 10.7 — Test E2E Scenari RAD

**Obiettivo**: Eseguire manualmente (o automaticamente con Playwright) i 5 scenari del RAD per certificare il sistema.

### Scenario 1 — Gestione Agenda Giornaliera (RF_BAR_2, RF_BAR_11)
```
ATTORE: Tony (BAR)
PROCEDURA:
  1. Tony accede a /barber
  2. Visualizza la dashboard giornaliera di oggi
  3. Clicca "Crea prenotazione diretta"
  4. Inserisce: Mario Rossi, 3331234567, Capelli + Barba, Poltrona 1, 10:00
  5. Conferma la creazione
RISULTATO ATTESO:
  ✅ La prenotazione appare immediatamente nella dashboard con stato ACCETTATA
  ✅ Lo slot 10:00-10:45 è bloccato per Poltrona 1 (non appare come disponibile)
```

### Scenario 2 — Flusso Prenotazione CLR (RF_CLI_5/6, RF_BAR_14, RF_GEN_5)
```
ATTORI: Marco (CLR), Tony (BAR)
PROCEDURA:
  1. Marco accede a /book
  2. Seleziona servizio "Capelli" (30 min)
  3. Seleziona data domani
  4. Seleziona slot 11:00 su Poltrona 2
  5. Conferma la richiesta
  6. Tony riceve notifica SSE "Nuova richiesta di prenotazione"
  7. Tony clicca "Accetta" nella dashboard
  8. Marco riceve notifica SSE "Prenotazione confermata!"
RISULTATO ATTESO:
  ✅ Marco vede la prenotazione con stato ACCETTATA nella propria homepage
  ✅ Tony non vede più la prenotazione nella lista "In Attesa"
  ✅ Lo slot 11:00-11:30 non è più disponibile per Poltrona 2 domani
```

### Scenario 3 — Prenotazione Ospite (RF_CLG_1)
```
ATTORE: Luigi (CLG, nessun account)
PROCEDURA:
  1. Luigi accede a /book senza autenticarsi
  2. Seleziona servizio "Barba"
  3. Seleziona data, slot disponibile
  4. Inserisce: Luigi Bianchi, 3451234567
  5. Conferma
RISULTATO ATTESO:
  ✅ Tony riceve notifica SSE "Nuova richiesta"
  ✅ Nella dashboard BAR la prenotazione mostra "Luigi Bianchi (ospite)"
  ✅ Luigi vede conferma ricezione richiesta (nessun account richiesto)
```

### Scenario 4 — Annullamento CLR con Motivazione (RF_CLR_4)
```
ATTORE: Marco (CLR), Tony (BAR)
PRECONDIZIONE: Marco ha una prenotazione ACCETTATA
PROCEDURA:
  1. Marco accede a /my/bookings
  2. Trova la prenotazione ACCETTATA
  3. Clicca "Annulla"
  4. Inserisce motivazione: "Ho un impegno improvviso"
  5. Conferma l'annullamento
RISULTATO ATTESO:
  ✅ Stato prenotazione → ANNULLATA
  ✅ Tony riceve notifica SSE con il testo della motivazione
  ✅ Lo slot viene liberato (torna disponibile)
```

### Scenario 5 — Configurazione Salone da Zero (RF_BAR_3/5/6/9, RF_BAR_10)
```
ATTORE: Tony (BAR)
PROCEDURA:
  1. Tony accede a /barber/chairs
  2. Rinomina "Poltrona 1" in "Poltrona Mario"
  3. Aggiunge nuova poltrona "Poltrona Luca"
  4. Accede a /barber/services
  5. Aggiunge servizio: "Trattamento Premium", 60 min, €35,00
  6. Accede a /barber/schedules
  7. Modifica orario Poltrona Luca lunedì: apertura 10:00-18:00
  8. Accede a /barber
RISULTATO ATTESO:
  ✅ Dashboard mostra 3 poltrone (Poltrona Mario, Poltrona 2, Poltrona Luca)
  ✅ La vetrina mostra il nuovo servizio "Trattamento Premium"
  ✅ Gli slot disponibili per Poltrona Luca il lunedì iniziano alle 10:00
```

### Automazione con Playwright (opzionale ma consigliata)
```bash
npm install -D @playwright/test
npx playwright install

# Creare tests/e2e/scenarios.spec.ts con i 5 scenari
npx playwright test
```

### Attività
- [ ] Eseguire i 5 scenari manualmente con stack completo su Docker Compose
- [ ] Documentare eventuali bug trovati e correggerli
- [ ] Se tempo disponibile: automatizzare i 5 scenari con Playwright

---

## Fase 10.8 — Revisione Architetturale Finale

**Obiettivo**: Verificare che tutte le decisioni architetturali siano state rispettate nell'implementazione.

### Checklist Pattern Architetturali
- [ ] **State Pattern**: `BookingStatus.transitionTo()` usato ovunque — nessun `if/else` di stato nel Service layer
- [ ] **Strategy Pattern**: `AvailabilityService` inietta `AvailabilityStrategy` via costruttore — non istanzia direttamente `StandardAvailabilityStrategy`
- [ ] **Observer Pattern**: `NotificationService` usa `@EventListener` — nessuna dipendenza diretta da `BookingService`
- [ ] **Facade Pattern**: `BookingService` è il singolo punto di ingresso — i Controller non chiamano Repository direttamente
- [ ] **Chain of Responsibility**: `validators.forEach(v -> v.validate(req))` — nessuna logica di validazione inline nel Service
- [ ] **Specification Pattern**: `PrenotazioneRepository` non ha metodi come `findByClientAndStatusAndDateBetween...` (esplosione nome)
- [ ] **DTO rigorosi**: nessuna Entity JPA nel body di risposta API — MapStruct su tutti i mapper
- [ ] **Flyway migrations**: tutte le modifiche schema via file `Vn__*.sql` — nessun `ddl-auto: create-drop`
- [ ] **Soft-delete**: Servizi e Poltrone hanno `attivo/attiva` flag — nessuna cancellazione fisica

### Checklist Sicurezza
- [ ] BCrypt strength 12 configurato
- [ ] JWT con durata 15 minuti
- [ ] Refresh Token mai in chiaro nel DB (solo hash SHA-256)
- [ ] Nessun endpoint protetto accessibile senza autenticazione
- [ ] Nessuna Entity esposta via API direttamente
- [ ] Rate limiting su `/api/auth/**`
- [ ] CORS con whitelist esplicita

### Checklist Qualità Codice
- [ ] Nessun metodo con complessità ciclomatica > 10 (verifica SonarCloud)
- [ ] Nessuna duplicazione di codice significativa (> 5 righe identiche)
- [ ] Tutti i TODO rimasti documentati come issue GitHub per lavori futuri
- [ ] Tutti i `log.info/warn/error` usano un logger non raw (`@Slf4j` di Lombok)

### Attività
- [ ] Eseguire checklist pattern architetturali manualmente
- [ ] Verificare checklist sicurezza
- [ ] Aprire GitHub Issues per i TODO rimanenti

---

## Fase 10.9 — Documentazione Finale

**Obiettivo**: Aggiornare la documentazione del progetto per chi si avvicina per la prima volta al codice.

### `README.md` (root) aggiornato
```markdown
# BarberBook 🪒

Sistema gestionale per prenotazioni barber shop.

## Quick Start

### Prerequisiti
- Docker Desktop 24+
- Java 21 (solo per sviluppo locale)
- Node 20 (solo per sviluppo locale)

### Avvio con Docker
\`\`\`bash
cp .env.example .env
# Modificare .env con le proprie credenziali
docker-compose up --build
\`\`\`

Accesso:
- Frontend: http://localhost
- Backend: http://localhost:8080
- API Health: http://localhost:8080/api/health

### Accesso iniziale
- BAR: tony@hairmanbarber.it / [vedere .env.example]

## Stack Tecnologico
[tabella tech stack]

## Qualità
| Metrica | Valore |
|---------|--------|
| JaCoCo Coverage | ≥ 80% |
| Mutation Score (PiTest) | ≥ 70% |
| SonarCloud Rating | A |
| Vulnerabilità Snyk (HIGH+) | 0 |

## Migrazioni DB (Flyway)
[lista migrazioni]
```

### Attività
- [ ] Aggiornare `README.md` con istruzioni complete
- [ ] Verificare che `.env.example` sia aggiornato con tutte le variabili necessarie
- [ ] Aggiungere badge CI, SonarCloud e Snyk al README
- [ ] Creare `CHANGELOG.md` con riepilogo sprint completati

---

## Fase 10.10 — Verifica Quality Gate Totale

**Obiettivo**: Verifica finale che **tutti** i quality gate dell'intero progetto siano soddisfatti.

### Quality Gate Backend
| Gate | Target | Strumento | Verifica |
|------|--------|-----------|----------|
| Test Coverage | ≥ 80% LINE | JaCoCo | `mvn verify` |
| Branch Coverage | ≥ 75% | JaCoCo | `mvn verify` |
| Mutation Score | ≥ 70% | PiTest | `mvn pitest:mutationCoverage` |
| Security Vulnerabilities | 0 HIGH/CRITICAL | Snyk | CI |
| Code Bugs | 0 Critical/Major | SonarCloud | CI |
| Code Smells | ≤ 30 min tech debt totale | SonarCloud | Dashboard |
| Duplications | ≤ 3% | SonarCloud | Dashboard |

### Quality Gate Frontend
| Gate | Target | Strumento | Verifica |
|------|--------|-----------|----------|
| Component Tests | 0 failure | Vitest | `npm test` |
| Build | 0 error/warning | Vite | `npm run build` |
| TypeScript | 0 type error | tsc | `npm run typecheck` |
| ESLint | 0 error | ESLint | `npm run lint` |

### Quality Gate Sicurezza
| Gate | Target | Verifica |
|------|--------|----------|
| Segreti in repo | 0 | GitGuardian dashboard |
| Security headers | CSP, X-Frame, nosniff | Browser DevTools |
| CORS | Whitelist esplicita, no wildcard | Review `SecurityConfig` |
| Rate Limiting | 429 al 6° tentativo login | Test manuale |
| Cookie RT | HttpOnly + Secure + SameSite | Browser DevTools |

### Quality Gate E2E
| Gate | Target | Verifica |
|------|--------|----------|
| Scenario 1 (Agenda BAR) | Funziona | Test manuale |
| Scenario 2 (Prenotazione CLR) | Funziona | Test manuale |
| Scenario 3 (Ospite) | Funziona | Test manuale |
| Scenario 4 (Annullamento) | Funziona | Test manuale |
| Scenario 5 (Configurazione) | Funziona | Test manuale |

---

## Definition of Done — Sprint 10

| Criterio | Verifica |
|----------|----------|
| ✅ Rate Limiting attivo | 429 al sesto tentativo di login da stesso IP |
| ✅ Security Headers configurati | CSP, X-Frame-Options, nosniff presenti in risposta |
| ✅ CORS whitelist esplicita | No wildcard `*` in produzione |
| ✅ Snyk 0 HIGH/CRITICAL (backend) | Report CI verde |
| ✅ Snyk 0 HIGH/CRITICAL (frontend) | Report CI verde |
| ✅ GitGuardian 0 segreti | Dashboard nessun alert |
| ✅ SonarCloud Quality Gate verde | A rating su Security e Reliability |
| ✅ JaCoCo ≥ 80% LINE coverage | `mvn verify` passa il check gate |
| ✅ PiTest ≥ 70% mutation score | Report HTML verificato |
| ✅ Docker immagini non-root | Dockerfile con `USER` non-root |
| ✅ 5 scenari RAD eseguiti | Nessun bug bloccante riscontrato |
| ✅ README aggiornato | Quick start funzionante da zero |
| ✅ CI pipeline verde | GitHub Actions verde su main |

---

## Il Progetto è Completo Quando...

```
✅ Sprint 0:  Infrastruttura         — CI verde, stack Docker avviato
✅ Sprint 1:  Auth & Utenti          — JWT + RBAC funzionante
✅ Sprint 2:  Catalogo Servizi       — CRUD BAR + vetrina pubblica
✅ Sprint 3:  Poltrone               — CRUD BAR + lista pubblica
✅ Sprint 4:  Orari & Disponibilità  — Slot liberi calcolati correttamente
✅ Sprint 5:  Prenotazioni Core      — Ciclo vita completo, no-double-booking
✅ Sprint 6:  Notifiche SSE          — Push real-time funzionante
✅ Sprint 7:  Dashboard & Storico    — Viste operative complete
✅ Sprint 8:  Feature Avanzate       — Password recovery, modifica profilo
✅ Sprint 9:  Frontend React         — UI completa, responsive
✅ Sprint 10: Security Hardening     — Quality gate finale superato
```

**BarberBook è pronto per il deploy. 🎉**

---

*Sprint 10 — Ultima modifica: 22/04/2026*
