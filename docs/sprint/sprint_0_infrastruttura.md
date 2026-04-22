# Sprint 0 — Infrastruttura & Scaffolding
> **Stato**: ⬜ Non iniziato  
> **Dipende da**: —  
> **Obiettivo**: Avere un progetto che compila, si avvia, supera la CI e ha un database raggiungibile. Zero feature di business — solo la fondamenta tecnica su cui tutto il resto si costruirà.

---

## Indice Fasi

1. [Fase 0.1 — Struttura Monorepo](#fase-01--struttura-monorepo)
2. [Fase 0.2 — Backend Spring Boot](#fase-02--backend-spring-boot)
3. [Fase 0.3 — Database & Flyway](#fase-03--database--flyway)
4. [Fase 0.4 — Docker & Docker Compose](#fase-04--docker--docker-compose)
5. [Fase 0.5 — GitHub Actions CI](#fase-05--github-actions-ci)
6. [Fase 0.6 — SonarCloud & DevOps Tools](#fase-06--sonarcloud--devops-tools)
7. [Fase 0.7 — Endpoint Health & Verifica Finale](#fase-07--endpoint-health--verifica-finale)

---

## Fase 0.1 — Struttura Monorepo

**Obiettivo**: Creare la struttura di directory del progetto completa e coerente con il tech design.

### Attività
- [ ] Creare la root del progetto con struttura monorepo:
  ```
  barberbook/
  ├── backend/          ← Maven project (Spring Boot)
  ├── frontend/         ← Vite project (React + TS) — placeholder per ora
  ├── docker/           ← Dockerfile e nginx config
  ├── docs/             ← Documentazione di progetto
  ├── docker-compose.yml
  ├── .github/
  │   └── workflows/
  ├── .gitignore        ← già presente
  └── README.md
  ```
- [ ] Creare `README.md` root con descrizione progetto e istruzioni di avvio
- [ ] Verificare `.gitignore` copra: `target/`, `node_modules/`, `.env`, `*.class`, `.idea/`

### Output
- Directory `backend/` esistente (anche vuota)
- Directory `frontend/` esistente (placeholder)
- Directory `docker/` esistente
- `README.md` con descrizione minima

---

## Fase 0.2 — Backend Spring Boot

**Obiettivo**: Creare il progetto Maven con tutte le dipendenze definite nel tech design. Il progetto deve compilare senza errori.

### Dipendenze Maven da configurare in `pom.xml`
```xml
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-validation
spring-boot-starter-actuator
flyway-core
postgresql (runtime)
lombok
mapstruct
mapstruct-processor
jjwt-api
jjwt-impl
jjwt-jackson

<!-- Test -->
spring-boot-starter-test
testcontainers (BOM)
testcontainers-postgresql
mockito-core
```

### Struttura package da creare
```
src/main/java/com/barberbook/
├── BarberBookApplication.java     ← entry point
├── config/                        ← configurazioni Spring (vuote per ora)
├── domain/
│   ├── model/                     ← Entità JPA (vuote per ora)
│   └── enums/                     ← Enum di dominio (vuoti per ora)
├── repository/                    ← Spring Data Repositories (vuoti)
├── service/                       ← Service layer (vuoti)
├── controller/                    ← REST Controllers
│   └── HealthController.java      ← unico controller di questo sprint
├── dto/                           ← DTO Request/Response
├── security/                      ← JWT, filtri (vuoti per ora)
├── exception/                     ← Eccezioni custom (vuote per ora)
└── util/                          ← Utility (vuote per ora)
```

### File di configurazione
- [ ] `src/main/resources/application.yml` — configurazione base (datasource, jpa, flyway, actuator)
- [ ] `src/main/resources/application-dev.yml` — configurazione sviluppo locale
- [ ] `src/main/resources/application-prod.yml` — configurazione produzione (valori da env)
- [ ] `src/test/resources/application-test.yml` — configurazione test (Testcontainers)

### Configurazione `application.yml` base
```yaml
spring:
  application:
    name: barberbook
  profiles:
    active: dev
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate      # Flyway gestisce lo schema, Hibernate solo valida
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    locations: classpath:db/migration
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

### Output
- `mvn compile` verde senza warning/errori
- Struttura package completa (anche con classi placeholder)

---

## Fase 0.3 — Database & Flyway

**Obiettivo**: Prima migrazione Flyway funzionante. Il DB si inizializza automaticamente all'avvio.

### Directory migrazioni
```
src/main/resources/db/migration/
└── V1__init_schema.sql
```

### Contenuto `V1__init_schema.sql`
Schema base minimale — solo la tabella `users` con i campi strettamente necessari alla compilazione JPA. Verrà arricchita in Sprint 1.

```sql
-- V1: Schema iniziale BarberBook
-- Tabella users (base, verrà estesa in Sprint 1)
CREATE TABLE IF NOT EXISTS users (
    id         BIGSERIAL PRIMARY KEY,
    nome       VARCHAR(100) NOT NULL,
    cognome    VARCHAR(100) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    telefono   VARCHAR(20),
    ruolo      VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_ruolo ON users(ruolo);
```

### Attività
- [ ] Creare directory `src/main/resources/db/migration/`
- [ ] Creare `V1__init_schema.sql` con schema base
- [ ] Verificare che Flyway applichi la migrazione all'avvio senza errori

### Output
- Tabella `users` creata in PostgreSQL
- Flyway `flyway_schema_history` popolata con V1

---

## Fase 0.4 — Docker & Docker Compose

**Obiettivo**: L'intero stack si avvia con un singolo `docker-compose up`.

### File da creare

#### `docker-compose.yml` (root)
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

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/barberbook
      DB_USER: ${DB_USER}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      SPRING_PROFILES_ACTIVE: dev
    ports:
      - "8080:8080"

  nginx:
    image: nginx:alpine
    volumes:
      - ./docker/nginx.conf:/etc/nginx/nginx.conf:ro
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  postgres_data:
```

#### `docker/Dockerfile` (backend — multi-stage)
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

#### `docker/nginx.conf`
```nginx
events { worker_connections 1024; }

http {
    server {
        listen 80;

        # Proxy API calls al backend
        location /api/ {
            proxy_pass http://backend:8080;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }

        # Placeholder: frontend (Sprint 9)
        location / {
            return 200 '{"message":"Frontend coming in Sprint 9"}';
            add_header Content-Type application/json;
        }
    }
}
```

#### `.env` (NON committato in git — solo template)
Creare `.env.example`:
```env
DB_USER=barberbook_user
DB_PASSWORD=your_secure_password_here
JWT_SECRET=your_256_bit_secret_here_minimum_32_chars
```

### Attività
- [ ] Creare `docker-compose.yml`
- [ ] Creare `docker/Dockerfile` backend multi-stage
- [ ] Creare `docker/nginx.conf`
- [ ] Creare `.env.example` e aggiungere `.env` al `.gitignore`
- [ ] Aggiungere `mvnw` e `mvnw.cmd` al backend (Maven Wrapper)
- [ ] Testare: `docker-compose up` → stack si avvia senza errori
- [ ] Verificare healthcheck: `docker-compose ps` mostra `healthy`

### Output
- `docker-compose up` avvia postgres + backend + nginx
- Backend raggiungibile su `http://localhost:8080`

---

## Fase 0.5 — GitHub Actions CI

**Obiettivo**: Ogni push su `main` e `develop` esegue la pipeline CI automaticamente.

### File da creare: `.github/workflows/ci-backend.yml`
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
      - name: Checkout
        uses: actions/checkout@v4
        with:
          fetch-depth: 0   # necessario per SonarCloud analisi completa

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Build & Test con JaCoCo
        run: mvn -B verify jacoco:report
        working-directory: backend

      - name: Upload JaCoCo Report
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-report
          path: backend/target/site/jacoco/

      - name: SonarCloud Analysis
        uses: SonarSource/sonarqube-scan-action@v5
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: https://sonarcloud.io

      - name: Snyk Security Scan
        uses: snyk/actions/maven@master
        with:
          args: --severity-threshold=high
        env:
          SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
```

### Configurazione JaCoCo in `pom.xml`
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <excludes>
            <exclude>**/dto/**</exclude>
            <exclude>**/config/**</exclude>
            <exclude>**/*Application.class</exclude>
            <exclude>**/domain/enums/**</exclude>
        </excludes>
    </configuration>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
        </execution>
        <execution>
            <id>check</id>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                            <limit>
                                <counter>BRANCH</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.75</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

> **Nota Sprint 0**: Le soglie JaCoCo (80% line, 75% branch) sono configurate ma **non bloccanti** in questo sprint perché non esistono ancora test di business. Le soglie entreranno in vigore a partire da Sprint 1.

### Attività
- [ ] Creare `.github/workflows/ci-backend.yml`
- [ ] Configurare secrets su GitHub: `SONAR_TOKEN`, `SNYK_TOKEN`
- [ ] Prima push → pipeline verde (0 test, 0 errori build)

### Output
- Badge CI verde nel README
- Pipeline eseguita con successo su ogni push

---

## Fase 0.6 — SonarCloud & DevOps Tools

**Obiettivo**: Strumenti di qualità e sicurezza configurati e collegati al repository.

### SonarCloud
- [ ] Creare account/progetto su SonarCloud collegato al repository GitHub
- [ ] Generare `SONAR_TOKEN` e aggiungerlo come secret GitHub
- [ ] Creare `backend/sonar-project.properties`:
  ```properties
  sonar.projectKey=gianfrancobarba_BarberBook
  sonar.organization=gianfrancobarba
  sonar.sources=src/main/java
  sonar.tests=src/test/java
  sonar.java.coveragePlugin=jacoco
  sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
  sonar.exclusions=**/dto/**,**/config/**,**/*Application.java
  ```

### Snyk
- [ ] Creare account Snyk e collegare al repository GitHub
- [ ] Generare `SNYK_TOKEN` e aggiungerlo come secret GitHub

### GitGuardian
- [ ] Installare GitGuardian come GitHub App sul repository
- [ ] Zero configurazione aggiuntiva richiesta (si configura da sola)

### Branch Protection Rules (GitHub)
- [ ] Proteggere branch `main`:
  - Require PR before merging
  - Require status checks: `CI Backend`
  - Dismiss stale reviews
- [ ] Proteggere branch `develop` (stesse regole)

### Output
- SonarCloud dashboard attiva (coverage 0% — accettabile in Sprint 0)
- Snyk scansione iniziale completata (0 vulnerabilità nel progetto base)
- GitGuardian attivo su ogni commit

---

## Fase 0.7 — Endpoint Health & Verifica Finale

**Obiettivo**: Verificare che l'intero stack funzioni end-to-end prima di chiudere lo sprint.

### `HealthController.java`
```java
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "BarberBook"));
    }
}
```

### Checklist verifica finale
- [ ] `mvn verify` in locale → BUILD SUCCESS, 0 test failure
- [ ] `docker-compose up` → tutti i servizi `healthy`/`running`
- [ ] `curl http://localhost:8080/api/health` → `{"status":"UP","service":"BarberBook"}`
- [ ] `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`
- [ ] Flyway: `V1__init_schema.sql` applicata e presente in `flyway_schema_history`
- [ ] Push su `develop` → GitHub Actions verde
- [ ] SonarCloud riceve il primo report

---

## Definition of Done — Sprint 0

| Criterio | Verifica |
|----------|----------|
| ✅ Struttura monorepo completa | Directory backend/, frontend/, docker/, docs/ presenti |
| ✅ `mvn verify` verde | 0 errori di compilazione, 0 test failure |
| ✅ `docker-compose up` funzionante | Tutti i servizi avviati e healthy |
| ✅ `GET /api/health` risponde 200 | `{"status":"UP"}` |
| ✅ Flyway V1 applicata | Tabella `users` creata in PostgreSQL |
| ✅ CI Pipeline verde | GitHub Actions build pass su primo push |
| ✅ SonarCloud attivo | Dashboard riceve primo report |
| ✅ `.env` mai committato | Solo `.env.example` in git |
| ✅ Branch protection attive | `main` e `develop` protetti |

---

## Note Operative

- Il **frontend** in questo sprint è un placeholder — lo scaffolding React avviene in Sprint 9.
- Le **soglie JaCoCo** sono configurate ma disabilitate per Sprint 0 (nessun Service layer ancora).
- Il Dockerfile frontend **non viene creato** in questo sprint.
- L'account BAR verrà creato via migrazione Flyway in **Sprint 1** (non ora).

---

*Sprint 0 — Ultima modifica: 22/04/2026*
