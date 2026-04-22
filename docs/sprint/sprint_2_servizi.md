# Sprint 2 — Catalogo Servizi
> **Stato**: ⬜ Non iniziato  
> **Dipende da**: Sprint 1 ✅  
> **Obiettivo**: Il BAR gestisce il proprio catalogo in modo dinamico. I clienti (autenticati e ospiti) consultano la vetrina pubblica senza autenticazione.

---

## Requisiti Funzionali Coperti

| RF | Nome | Attore | Priorità |
|----|------|--------|----------|
| RF_BAR_6 | Creazione Servizio | BAR | Alta |
| RF_BAR_7 | Modifica Servizio | BAR | Alta |
| RF_BAR_8 | Eliminazione Servizio | BAR | Media |
| RF_CLI_1 | Vetrina Servizi pubblica | CLI (CLR + CLG) | Alta |

---

## Indice Fasi

1. [Fase 2.1 — Modello di Dominio](#fase-21--modello-di-dominio)
2. [Fase 2.2 — Migrazione Flyway](#fase-22--migrazione-flyway)
3. [Fase 2.3 — Repository & Service](#fase-23--repository--service)
4. [Fase 2.4 — REST Controller](#fase-24--rest-controller)
5. [Fase 2.5 — Unit Test](#fase-25--unit-test)
6. [Fase 2.6 — Integration Test](#fase-26--integration-test)
7. [Fase 2.7 — Verifica Quality Gate](#fase-27--verifica-quality-gate)

---

## Fase 2.1 — Modello di Dominio

**Obiettivo**: Definire l'entità `Servizio` con i suoi DTO e mapper.

### Entità JPA — `Servizio.java`
```java
@Entity
@Table(name = "services")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Servizio {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 500)
    private String descrizione;

    @Column(nullable = false)
    @Min(1)
    private Integer durataMinuti;       // durata stimata in minuti (> 0)

    @Column(nullable = false, precision = 8, scale = 2)
    @DecimalMin("0.00")
    private BigDecimal prezzo;          // in EUR

    @Column(nullable = false)
    @Builder.Default
    private boolean attivo = true;      // soft-delete: false = eliminato

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;
}
```

> **Design Decision — Soft Delete**: L'eliminazione non è fisica. Il servizio viene disattivato (`attivo = false`). Questo preserva l'integrità referenziale con le prenotazioni storiche che referenziano quel servizio.

### DTO

```java
// Request: creazione
public record CreateServiceRequestDto(
    @NotBlank @Size(max = 100) String nome,
    @Size(max = 500) String descrizione,
    @NotNull @Min(1) Integer durataMinuti,
    @NotNull @DecimalMin("0.00") BigDecimal prezzo
) {}

// Request: modifica (tutti i campi opzionali — PATCH semantics)
public record UpdateServiceRequestDto(
    @Size(max = 100) String nome,
    @Size(max = 500) String descrizione,
    @Min(1) Integer durataMinuti,
    @DecimalMin("0.00") BigDecimal prezzo
) {}

// Response: pubblico (usato sia in vetrina che in prenotazione)
public record ServiceResponseDto(
    Long id,
    String nome,
    String descrizione,
    Integer durataMinuti,
    BigDecimal prezzo
) {}
```

### Mapper — `ServizioMapper.java`
```java
@Mapper(componentModel = "spring")
public interface ServizioMapper {
    ServiceResponseDto toDto(Servizio servizio);
    List<ServiceResponseDto> toDtoList(List<Servizio> servizi);
    Servizio toEntity(CreateServiceRequestDto dto);
}
```

### Attività
- [ ] Creare `Servizio.java` con tutti i campi e annotazioni JPA
- [ ] Creare `CreateServiceRequestDto`, `UpdateServiceRequestDto`, `ServiceResponseDto`
- [ ] Creare `ServizioMapper` con MapStruct
- [ ] Creare `ServizioRepository extends JpaRepository<Servizio, Long>`

---

## Fase 2.2 — Migrazione Flyway

**Obiettivo**: Creare la tabella `services` con vincoli di integrità e popolarla con servizi iniziali di esempio.

### `V4__services_schema.sql`
```sql
CREATE TABLE services (
    id             BIGSERIAL PRIMARY KEY,
    nome           VARCHAR(100) NOT NULL,
    descrizione    VARCHAR(500),
    durata_minuti  INTEGER NOT NULL CHECK (durata_minuti > 0),
    prezzo         NUMERIC(8, 2) NOT NULL CHECK (prezzo >= 0),
    attivo         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP
);

CREATE INDEX idx_services_attivo ON services(attivo);
```

### `V5__seed_services.sql`
```sql
-- Catalogo iniziale del salone Hair Man Tony
INSERT INTO services (nome, descrizione, durata_minuti, prezzo) VALUES
    ('Capelli',          'Taglio capelli classico',                    30, 15.00),
    ('Barba',            'Rifinitura e cura della barba',              20, 10.00),
    ('Capelli + Barba',  'Taglio capelli completo con cura barba',     45, 22.00),
    ('Capelli Junior',   'Taglio per bambini fino a 12 anni',          25, 12.00),
    ('Trattamento cute', 'Trattamento idratante e rigenerante cute',   30, 18.00);
```

### Attività
- [ ] Creare `V4__services_schema.sql`
- [ ] Creare `V5__seed_services.sql` con i servizi iniziali del salone
- [ ] Verificare migrazioni applicate correttamente
- [ ] Verificare CHECK constraints funzionanti: `durata_minuti > 0`, `prezzo >= 0`

---

## Fase 2.3 — Repository & Service

**Obiettivo**: Implementare la logica di business per la gestione del catalogo servizi.

### Repository — `ServizioRepository.java`
```java
public interface ServizioRepository extends JpaRepository<Servizio, Long> {

    // Tutti i servizi attivi (vetrina pubblica)
    List<Servizio> findByAttivoTrue();

    // Verifica unicità del nome tra i servizi attivi
    boolean existsByNomeAndAttivoTrue(String nome);

    // Singolo servizio attivo (usato in prenotazione per recuperare durata)
    Optional<Servizio> findByIdAndAttivoTrue(Long id);
}
```

### Service — `ServiceCatalogService.java`
```java
@Service
@RequiredArgsConstructor
@Transactional
public class ServiceCatalogService {

    private final ServizioRepository servizioRepository;
    private final ServizioMapper servizioMapper;

    // -------------------------------------------------------
    // RF_CLI_1 — Vetrina pubblica (lettura, nessun auth)
    // -------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ServiceResponseDto> getAllActive() {
        return servizioMapper.toDtoList(servizioRepository.findByAttivoTrue());
    }

    @Transactional(readOnly = true)
    public ServiceResponseDto getById(Long id) {
        Servizio s = servizioRepository.findByIdAndAttivoTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Servizio non trovato: " + id));
        return servizioMapper.toDto(s);
    }

    // -------------------------------------------------------
    // RF_BAR_6 — Creazione servizio
    // -------------------------------------------------------

    public ServiceResponseDto create(CreateServiceRequestDto dto) {
        // Nessun vincolo di unicità forzato — il BAR può avere servizi con stesso nome
        Servizio s = servizioMapper.toEntity(dto);
        s.setAttivo(true);
        s.setCreatedAt(LocalDateTime.now());
        return servizioMapper.toDto(servizioRepository.save(s));
    }

    // -------------------------------------------------------
    // RF_BAR_7 — Modifica servizio
    // -------------------------------------------------------

    public ServiceResponseDto update(Long id, UpdateServiceRequestDto dto) {
        Servizio s = servizioRepository.findByIdAndAttivoTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Servizio non trovato: " + id));

        // Aggiorna solo i campi non null (PATCH semantics)
        if (dto.nome() != null)          s.setNome(dto.nome());
        if (dto.descrizione() != null)   s.setDescrizione(dto.descrizione());
        if (dto.durataMinuti() != null)  s.setDurataMinuti(dto.durataMinuti());
        if (dto.prezzo() != null)        s.setPrezzo(dto.prezzo());
        s.setUpdatedAt(LocalDateTime.now());

        return servizioMapper.toDto(servizioRepository.save(s));
    }

    // -------------------------------------------------------
    // RF_BAR_8 — Eliminazione servizio (soft-delete)
    // -------------------------------------------------------

    public void delete(Long id) {
        Servizio s = servizioRepository.findByIdAndAttivoTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Servizio non trovato: " + id));
        s.setAttivo(false);
        s.setUpdatedAt(LocalDateTime.now());
        servizioRepository.save(s);
        // Nota: le prenotazioni storiche mantengono il riferimento al servizio
    }
}
```

### Eccezioni custom
```java
// Già in exception/ dal Sprint 1 GlobalExceptionHandler
public class ResourceNotFoundException extends RuntimeException  // 404
```

### Attività
- [ ] Creare `ServizioRepository` con query custom
- [ ] Creare `ServiceCatalogService` con tutti e 4 i metodi
- [ ] Creare `ResourceNotFoundException` nel `GlobalExceptionHandler` → `404`
- [ ] Verificare che `@Transactional(readOnly = true)` sia applicato sulle query

---

## Fase 2.4 — REST Controller

**Obiettivo**: Esporre gli endpoint con la corretta autorizzazione RBAC.

### `ServiceController.java`
```java
@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceCatalogService serviceCatalogService;

    // -------------------------------------------------------
    // PUBBLICI — accessibili senza autenticazione
    // -------------------------------------------------------

    /** RF_CLI_1 — Vetrina: lista tutti i servizi attivi */
    @GetMapping
    public ResponseEntity<List<ServiceResponseDto>> getAll() {
        return ResponseEntity.ok(serviceCatalogService.getAllActive());
    }

    /** RF_CLI_1 — Dettaglio singolo servizio */
    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceCatalogService.getById(id));
    }

    // -------------------------------------------------------
    // RISERVATI BAR — @PreAuthorize su metodo
    // -------------------------------------------------------

    /** RF_BAR_6 — Crea nuovo servizio */
    @PostMapping
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<ServiceResponseDto> create(
            @Valid @RequestBody CreateServiceRequestDto dto) {
        return ResponseEntity.status(201).body(serviceCatalogService.create(dto));
    }

    /** RF_BAR_7 — Modifica servizio esistente */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<ServiceResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceRequestDto dto) {
        return ResponseEntity.ok(serviceCatalogService.update(id, dto));
    }

    /** RF_BAR_8 — Elimina servizio (soft-delete) */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceCatalogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### API Endpoints Riepilogo
| Metodo | Path | Auth | Status | RF |
|--------|------|------|--------|----|
| GET | `/api/services` | Pubblico | 200 | RF_CLI_1 |
| GET | `/api/services/{id}` | Pubblico | 200 / 404 | RF_CLI_1 |
| POST | `/api/services` | BARBER | 201 / 400 / 403 | RF_BAR_6 |
| PATCH | `/api/services/{id}` | BARBER | 200 / 400 / 403 / 404 | RF_BAR_7 |
| DELETE | `/api/services/{id}` | BARBER | 204 / 403 / 404 | RF_BAR_8 |

### Attività
- [ ] Creare `ServiceController` con tutti gli endpoint
- [ ] Verificare che `GET /api/services` sia pubblico (configurato in `SecurityConfig` Sprint 1)
- [ ] Verificare risposta `404` per servizio non trovato
- [ ] Verificare risposta `403` per CLR che tenta operazioni BAR-only

---

## Fase 2.5 — Unit Test

**Obiettivo**: Coprire con test unitari tutta la logica di `ServiceCatalogService` in isolamento.

### `ServiceCatalogServiceTest.java`
```java
@ExtendWith(MockitoExtension.class)
class ServiceCatalogServiceTest {

    @Mock ServizioRepository servizioRepository;
    @Mock ServizioMapper servizioMapper;
    @InjectMocks ServiceCatalogService serviceCatalogService;

    // --- getAllActive ---
    @Test
    @DisplayName("getAllActive: ritorna solo i servizi con attivo=true")
    void getAllActive_returnsOnlyActiveServices() {
        // Given: repository ritorna 2 servizi attivi
        given(servizioRepository.findByAttivoTrue()).willReturn(List.of(activeService1, activeService2));
        // When
        List<ServiceResponseDto> result = serviceCatalogService.getAllActive();
        // Then
        assertThat(result).hasSize(2);
    }

    // --- getById ---
    @Test void getById_existingActiveService_returnsDto()
    @Test void getById_inactiveService_throwsResourceNotFoundException()
    @Test void getById_nonExistentId_throwsResourceNotFoundException()

    // --- create ---
    @Test void create_validData_returnsCreatedDto()
    @Test void create_zeroDuration_validationFails()   // validato dal DTO @Min(1)
    @Test void create_negativePrice_validationFails()  // validato dal DTO @DecimalMin

    // --- update ---
    @Test void update_validData_updatesOnlyProvidedFields()
    @Test void update_nullFields_doesNotOverwriteExistingValues()
    @Test void update_inactiveService_throwsResourceNotFoundException()
    @Test void update_nonExistentService_throwsResourceNotFoundException()

    // --- delete (soft-delete) ---
    @Test void delete_activeService_setsAttivoFalse()
    @Test void delete_inactiveService_throwsResourceNotFoundException()
    @Test void delete_serviceNotInRepository_throwsResourceNotFoundException()
    @Test
    @DisplayName("delete: il servizio eliminato non appare più in vetrina")
    void delete_thenGetAllActive_deletedServiceNotReturned()
}
```

### Attività
- [ ] Implementare tutti i test `ServiceCatalogServiceTest`
- [ ] Verificare coverage `ServiceCatalogService` ≥ 80%

---

## Fase 2.6 — Integration Test

**Obiettivo**: Verificare il comportamento degli endpoint con DB reale e autenticazione JWT.

### `ServiceIntegrationTest.java`
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class ServiceIntegrationTest {

    // Setup Testcontainers PostgreSQL...

    // --- Scenari pubblici ---
    @Test void getAll_noAuth_returns200WithActiveServices()
    @Test void getById_existingService_returns200()
    @Test void getById_nonExistent_returns404()

    // --- Scenari BARBER ---
    @Test void create_asBarber_returns201WithNewService()
    @Test void create_asBarber_invalidDuration_returns400()
    @Test void update_asBarber_existingService_returns200Updated()
    @Test void delete_asBarber_returns204AndServiceDisappears()
    @Test
    @DisplayName("Servizi eliminati non appaiono in GET /api/services")
    void deletedService_notReturnedInPublicList()

    // --- Scenari RBAC ---
    @Test void create_asClient_returns403()
    @Test void update_asClient_returns403()
    @Test void delete_asClient_returns403()
    @Test void create_noAuth_returns401()

    // --- Scenari seed ---
    @Test void seedServices_allPresentOnStartup_returns5Services()
}
```

### Attività
- [ ] Implementare `ServiceIntegrationTest` con Testcontainers
- [ ] Creare helper per generare JWT di test (`BarberJwtHelper`, `ClientJwtHelper`)
- [ ] Verificare che i seed (V5) siano presenti nel DB di test

---

## Fase 2.7 — Verifica Quality Gate

### Checklist finale Sprint 2
- [ ] `mvn verify` → BUILD SUCCESS
- [ ] JaCoCo: `ServiceCatalogService` LINE ≥ 80%, BRANCH ≥ 75%
- [ ] SonarCloud: 0 Bug Critical/Major
- [ ] Endpoint pubblici accessibili senza token
- [ ] Soft-delete funzionante: DELETE non rimuove il record dal DB
- [ ] Le prenotazioni future che referenziano il servizio restano integre dopo il delete
- [ ] Push su `develop` → GitHub Actions verde

---

## Definition of Done — Sprint 2

| Criterio | Verifica |
|----------|----------|
| ✅ RF_BAR_6 Creazione servizio | `POST /api/services` crea servizio con nome, durata, prezzo |
| ✅ RF_BAR_7 Modifica servizio | `PATCH /api/services/{id}` aggiorna solo i campi forniti |
| ✅ RF_BAR_8 Eliminazione soft | `DELETE /api/services/{id}` setta `attivo=false`, non cancella il record |
| ✅ RF_CLI_1 Vetrina pubblica | `GET /api/services` accessibile senza autenticazione |
| ✅ Soft-delete corretto | Servizio eliminato non appare in vetrina, ma record presente in DB |
| ✅ Validazione input | Durata negativa/zero → 400, prezzo negativo → 400 |
| ✅ RBAC rispettato | CLR e guest non possono creare/modificare/eliminare servizi |
| ✅ Unit test ≥ 80% coverage | ServiceCatalogService completamente coperto |
| ✅ Integration test verdi | Tutti i flussi testati con DB reale |
| ✅ CI pipeline verde | GitHub Actions passa |

---

## Note Operative

- Il **soft-delete** è critico: Sprint 5 (Prenotazioni) referenzia i servizi nelle prenotazioni storiche. La cancellazione fisica romperebbe l'integrità referenziale.
- La **durata del servizio** (`durataMinuti`) è il dato che Sprint 4 usa per calcolare gli slot disponibili. Deve essere accurata.
- In Sprint 9 (Frontend) la vetrina servizi sarà la prima schermata visibile ai clienti e il punto di ingresso al flusso di prenotazione.

---

*Sprint 2 — Ultima modifica: 22/04/2026*
