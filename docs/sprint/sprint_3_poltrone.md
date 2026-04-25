# Sprint 3 — Gestione Poltrone
> **Stato**: ✅ Completato  
> **Dipende da**: Sprint 1 ✅  
> **Obiettivo**: Il BAR gestisce le risorse fisiche del salone. Le poltrone sono l'unità base su cui si organizza tutta l'agenda — ogni prenotazione è associata a una poltrona specifica.

---

## Requisiti Funzionali Coperti

| RF | Nome | Attore | Priorità |
|----|------|--------|----------|
| RF_BAR_3 | Aggiunta Poltrona | BAR | Media |
| RF_BAR_4 | Rimozione Poltrona | BAR | Media |
| RF_BAR_5 | Rinomina Poltrona | BAR | Bassa |
| RF_CLI_2 | Visualizzazione Poltrone Disponibili | CLI | Alta |

---

## Indice Fasi

1. [Fase 3.1 — Modello di Dominio](#fase-31--modello-di-dominio)
2. [Fase 3.2 — Migrazione Flyway](#fase-32--migrazione-flyway)
3. [Fase 3.3 — Repository & Service](#fase-33--repository--service)
4. [Fase 3.4 — REST Controller](#fase-34--rest-controller)
5. [Fase 3.5 — Unit Test](#fase-35--unit-test)
6. [Fase 3.6 — Integration Test](#fase-36--integration-test)
7. [Fase 3.7 — Verifica Quality Gate](#fase-37--verifica-quality-gate)

---

## Fase 3.1 — Modello di Dominio

**Obiettivo**: Definire l'entità `Poltrona` con i suoi DTO e mapper.

### Entità JPA — `Poltrona.java`
```java
@Entity
@Table(name = "chairs",
       uniqueConstraints = @UniqueConstraint(columnNames = "nome", name = "uq_chair_nome"))
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Poltrona {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;                // nome personalizzato (es. "Poltrona Mario")

    @Column(nullable = false)
    @Builder.Default
    private boolean attiva = true;      // soft-delete

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;
}
```

> **Vincolo di unicità**: Il nome della poltrona è `UNIQUE` a livello di DB. Due poltrone non possono avere lo stesso nome, indipendentemente dal fatto che siano attive o meno.

### DTO

```java
// Request: aggiunta
public record CreateChairRequestDto(
    @NotBlank @Size(max = 100) String nome
) {}

// Request: rinomina (RF_BAR_5)
public record UpdateChairRequestDto(
    @NotBlank @Size(max = 100) String nome
) {}

// Response: pubblica (usata sia in vetrina che in prenotazione e dashboard)
public record ChairResponseDto(
    Long id,
    String nome
) {}
```

### Mapper — `PoltronaMapper.java`
```java
@Mapper(componentModel = "spring")
public interface PoltronaMapper {
    ChairResponseDto toDto(Poltrona poltrona);
    List<ChairResponseDto> toDtoList(List<Poltrona> poltrone);
    Poltrona toEntity(CreateChairRequestDto dto);
}
```

### Attività
- [x] Creare `Poltrona.java` con unique constraint su `nome`
- [x] Creare `CreateChairRequestDto`, `UpdateChairRequestDto`, `ChairResponseDto`
- [x] Creare `PoltronaMapper` con MapStruct
- [x] Creare `PoltronaRepository extends JpaRepository<Poltrona, Long>`

---

## Fase 3.2 — Migrazione Flyway

**Obiettivo**: Creare la tabella `chairs` con i vincoli necessari e le due poltrone iniziali del salone.

### `V6__chairs_schema.sql`
```sql
CREATE TABLE chairs (
    id         BIGSERIAL PRIMARY KEY,
    nome       VARCHAR(100) NOT NULL,
    attiva     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT uq_chair_nome UNIQUE (nome)
);

CREATE INDEX idx_chairs_attiva ON chairs(attiva);
```

### `V7__seed_chairs.sql`
```sql
-- Due poltrone iniziali del salone (Scenario 5 del RAD)
-- I nomi possono essere personalizzati dal BAR via UI dopo il primo accesso
INSERT INTO chairs (nome) VALUES
    ('Poltrona 1'),
    ('Poltrona 2');
```

### Attività
- [x] Creare `V6__chairs_schema.sql` con UNIQUE constraint su `nome`
- [x] Creare `V7__seed_chairs.sql` con le 2 poltrone iniziali
- [x] Verificare migrazioni applicate correttamente
- [x] Verificare che il UNIQUE constraint sia attivo: inserimento duplicato → errore DB

---

## Fase 3.3 — Repository & Service

**Obiettivo**: Implementare la logica di business per la gestione delle poltrone.

### Repository — `PoltronaRepository.java`
```java
public interface PoltronaRepository extends JpaRepository<Poltrona, Long> {

    // Tutte le poltrone attive (vetrina pubblica + prenotazione)
    List<Poltrona> findByAttivaTrue();

    // Verifica unicità nome tra tutte le poltrone (attive e non)
    boolean existsByNome(String nome);

    // Poltrona attiva per ID (usato in validazione prenotazione Sprint 5)
    Optional<Poltrona> findByIdAndAttivaTrue(Long id);
}
```

### Service — `ChairService.java`
```java
@Service
@RequiredArgsConstructor
@Transactional
public class ChairService {

    private final PoltronaRepository poltronaRepository;
    private final PoltronaMapper poltronaMapper;

    // -------------------------------------------------------
    // RF_CLI_2 — Lista poltrone attive (pubblica)
    // -------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ChairResponseDto> getAllActive() {
        return poltronaMapper.toDtoList(poltronaRepository.findByAttivaTrue());
    }

    @Transactional(readOnly = true)
    public ChairResponseDto getById(Long id) {
        Poltrona p = poltronaRepository.findByIdAndAttivaTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Poltrona non trovata: " + id));
        return poltronaMapper.toDto(p);
    }

    // -------------------------------------------------------
    // RF_BAR_3 — Aggiunta poltrona
    // -------------------------------------------------------

    public ChairResponseDto create(CreateChairRequestDto dto) {
        // Verifica unicità del nome (anche tra poltrone disattivate)
        if (poltronaRepository.existsByNome(dto.nome())) {
            throw new ChairNameAlreadyExistsException(
                "Esiste già una poltrona con il nome: " + dto.nome());
        }
        Poltrona p = poltronaMapper.toEntity(dto);
        p.setAttiva(true);
        p.setCreatedAt(LocalDateTime.now());
        return poltronaMapper.toDto(poltronaRepository.save(p));
    }

    // -------------------------------------------------------
    // RF_BAR_5 — Rinomina poltrona
    // -------------------------------------------------------

    public ChairResponseDto rename(Long id, UpdateChairRequestDto dto) {
        Poltrona p = poltronaRepository.findByIdAndAttivaTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Poltrona non trovata: " + id));

        // Verifica unicità del nuovo nome (solo se cambia effettivamente)
        if (!p.getNome().equals(dto.nome()) && poltronaRepository.existsByNome(dto.nome())) {
            throw new ChairNameAlreadyExistsException(
                "Esiste già una poltrona con il nome: " + dto.nome());
        }

        p.setNome(dto.nome());
        p.setUpdatedAt(LocalDateTime.now());
        return poltronaMapper.toDto(poltronaRepository.save(p));
    }

    // -------------------------------------------------------
    // RF_BAR_4 — Rimozione poltrona (soft-delete)
    // -------------------------------------------------------

    public void deactivate(Long id) {
        Poltrona p = poltronaRepository.findByIdAndAttivaTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Poltrona non trovata: " + id));

        // Nota: le prenotazioni future su questa poltrona rimangono valide
        // Il BAR è responsabile di gestire le prenotazioni in conflitto manualmente
        p.setAttiva(false);
        p.setUpdatedAt(LocalDateTime.now());
        poltronaRepository.save(p);
    }
}
```

### Eccezioni custom da aggiungere
```java
public class ChairNameAlreadyExistsException extends RuntimeException  // 409 Conflict
```

Aggiungere al `GlobalExceptionHandler`:
```java
@ExceptionHandler(ChairNameAlreadyExistsException.class)
public ResponseEntity<ErrorResponseDto> handleChairConflict(ChairNameAlreadyExistsException ex) {
    return ResponseEntity.status(409).body(new ErrorResponseDto(ex.getMessage()));
}
```

### Attività
- [x] Creare `PoltronaRepository` con query custom
- [x] Creare `ChairService` con tutti i metodi
- [x] Creare `ChairNameAlreadyExistsException` → `409 Conflict`
- [x] Aggiungere handler nel `GlobalExceptionHandler`

---

## Fase 3.4 — REST Controller

**Obiettivo**: Esporre gli endpoint con la corretta autorizzazione RBAC.

### `ChairController.java`
```java
@RestController
@RequestMapping("/api/chairs")
@RequiredArgsConstructor
public class ChairController {

    private final ChairService chairService;

    // -------------------------------------------------------
    // PUBBLICI
    // -------------------------------------------------------

    /** RF_CLI_2 — Lista poltrone attive */
    @GetMapping
    public ResponseEntity<List<ChairResponseDto>> getAll() {
        return ResponseEntity.ok(chairService.getAllActive());
    }

    /** RF_CLI_2 — Dettaglio singola poltrona */
    @GetMapping("/{id}")
    public ResponseEntity<ChairResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(chairService.getById(id));
    }

    // -------------------------------------------------------
    // RISERVATI BAR
    // -------------------------------------------------------

    /** RF_BAR_3 — Aggiunge nuova poltrona */
    @PostMapping
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<ChairResponseDto> create(
            @Valid @RequestBody CreateChairRequestDto dto) {
        return ResponseEntity.status(201).body(chairService.create(dto));
    }

    /** RF_BAR_5 — Rinomina poltrona esistente */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<ChairResponseDto> rename(
            @PathVariable Long id,
            @Valid @RequestBody UpdateChairRequestDto dto) {
        return ResponseEntity.ok(chairService.rename(id, dto));
    }

    /** RF_BAR_4 — Disattiva poltrona (soft-delete) */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        chairService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
```

### API Endpoints Riepilogo
| Metodo | Path | Auth | Status | RF |
|--------|------|------|--------|----|
| GET | `/api/chairs` | Pubblico | 200 | RF_CLI_2 |
| GET | `/api/chairs/{id}` | Pubblico | 200 / 404 | RF_CLI_2 |
| POST | `/api/chairs` | BARBER | 201 / 400 / 403 / 409 | RF_BAR_3 |
| PATCH | `/api/chairs/{id}` | BARBER | 200 / 400 / 403 / 404 / 409 | RF_BAR_5 |
| DELETE | `/api/chairs/{id}` | BARBER | 204 / 403 / 404 | RF_BAR_4 |

### Attività
- [x] Creare `ChairController` con tutti gli endpoint
- [x] Aggiornare `SecurityConfig`: `GET /api/chairs` è pubblico
- [x] Verificare risposta `409` per nome duplicato
- [x] Verificare risposta `404` per poltrona non trovata

---

## Fase 3.5 — Unit Test

**Obiettivo**: Coprire con test unitari tutta la logica di `ChairService` in isolamento.

### `ChairServiceTest.java`
```java
@ExtendWith(MockitoExtension.class)
class ChairServiceTest {

    @Mock PoltronaRepository poltronaRepository;
    @Mock PoltronaMapper poltronaMapper;
    @InjectMocks ChairService chairService;

    // --- getAllActive ---
    @Test void getAllActive_returnsOnlyActiveChairs()
    @Test void getAllActive_emptyList_returnsEmptyList()

    // --- getById ---
    @Test void getById_existingActiveChair_returnsDto()
    @Test void getById_inactiveChair_throwsResourceNotFoundException()
    @Test void getById_nonExistentId_throwsResourceNotFoundException()

    // --- create ---
    @Test void create_uniqueName_success()
    @Test void create_duplicateName_throwsChairNameAlreadyExistsException()
    @Test void create_emptyName_validationFails()  // DTO @NotBlank
    @Test
    @DisplayName("create: la poltrona è attiva al momento della creazione")
    void create_newChair_isActiveByDefault()

    // --- rename ---
    @Test void rename_newUniqueName_success()
    @Test void rename_sameName_success()  // rinominare con lo stesso nome → OK (no conflict)
    @Test void rename_nameAlreadyUsed_throwsChairNameAlreadyExistsException()
    @Test void rename_inactiveChair_throwsResourceNotFoundException()

    // --- deactivate ---
    @Test void deactivate_activeChair_setsAttivaFalse()
    @Test void deactivate_alreadyInactiveChair_throwsResourceNotFoundException()
    @Test void deactivate_nonExistentChair_throwsResourceNotFoundException()
    @Test
    @DisplayName("deactivate: la poltrona non appare più in getAllActive")
    void deactivate_thenGetAllActive_chairNotReturned()
}
```

### Attività
- [x] Implementare tutti i test `ChairServiceTest`
- [x] Verificare coverage `ChairService` ≥ 80%

---

## Fase 3.6 — Integration Test

**Obiettivo**: Verificare il comportamento degli endpoint con DB reale, inclusi i vincoli di unicità.

### `ChairIntegrationTest.java`
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class ChairIntegrationTest {

    // --- Scenari pubblici ---
    @Test void getAll_noAuth_returns200WithSeedChairs()
    @Test void getAll_returns2ChairsFromSeed()
    @Test void getById_existingChair_returns200()
    @Test void getById_nonExistent_returns404()

    // --- Scenari BARBER ---
    @Test void create_asBarber_uniqueName_returns201()
    @Test void create_asBarber_duplicateName_returns409()
    @Test void rename_asBarber_newName_returns200()
    @Test void rename_asBarber_conflictingName_returns409()
    @Test void deactivate_asBarber_returns204()
    @Test
    @DisplayName("Poltrona disattivata non appare in GET /api/chairs")
    void deactivate_thenGetAll_chairNotInList()

    // --- Scenari RBAC ---
    @Test void create_asClient_returns403()
    @Test void rename_asClient_returns403()
    @Test void deactivate_asClient_returns403()

    // --- Vincoli DB ---
    @Test
    @DisplayName("UNIQUE constraint su nome gestito correttamente")
    void create_duplicateNameAtDbLevel_returns409()

    // --- Seed ---
    @Test
    @DisplayName("Il seed crea esattamente 2 poltrone all'avvio")
    void seedChairs_twoChairsPresentOnStartup()
}
```

### Attività
- [x] Implementare `ChairIntegrationTest` con Testcontainers
- [x] Verificare che il UNIQUE constraint a livello DB sia gestito senza stacktrace raw (GlobalExceptionHandler)

---

## Fase 3.7 — Verifica Quality Gate

### Checklist finale Sprint 3
- [ ] `mvn verify` → BUILD SUCCESS
- [ ] JaCoCo: `ChairService` LINE ≥ 80%, BRANCH ≥ 75%
- [ ] SonarCloud: 0 Bug Critical/Major
- [ ] UNIQUE constraint su `nome` attivo e testato
- [ ] Soft-delete funzionante: poltrona rimossa non appare in lista pubblica
- [ ] Integrità con prenotazioni: le prenotazioni esistenti su una poltrona disattivata rimangono nel DB
- [ ] Push su `develop` → GitHub Actions verde

---

## Definition of Done — Sprint 3

| Criterio | Verifica |
|----------|----------|
| ✅ RF_BAR_3 Aggiunta poltrona | `POST /api/chairs` crea poltrona con nome univoco |
| ✅ RF_BAR_4 Rimozione soft | `DELETE /api/chairs/{id}` setta `attiva=false` |
| ✅ RF_BAR_5 Rinomina | `PATCH /api/chairs/{id}` aggiorna il nome |
| ✅ RF_CLI_2 Lista pubblica | `GET /api/chairs` accessibile senza auth, solo poltrone attive |
| ✅ Nome univoco | Nomi duplicati → 409 Conflict |
| ✅ Integrità referenziale | La disattivazione non cancella fisicamente la poltrona |
| ✅ Unit test ≥ 80% coverage | ChairService completamente coperto |
| ✅ Integration test | UNIQUE constraint e soft-delete testati con DB reale |
| ✅ CI pipeline verde | GitHub Actions passa |

---

## Note Operative

- Il **vincolo di unicità** del nome include anche le poltrone disattivate. Questo evita confusioni: non è possibile creare "Poltrona Mario" se una poltrona con quel nome è già stata disattivata in precedenza.
- In **Sprint 4** (Orari), ogni `FasciaOraria` avrà un riferimento a una `Poltrona`. È importante che il modello `Poltrona` sia stabile prima di procedere.
- In **Sprint 5** (Prenotazioni), ogni `Prenotazione` referenzia una `Poltrona`. Il soft-delete garantisce che le prenotazioni storiche non siano orfane.
- **Scenario 5 del RAD**: il BAR rinomina le poltrone durante la configurazione iniziale. La funzionalità di rinomina (`RF_BAR_5`) copre esattamente questo scenario.

---

*Sprint 3 — Ultima modifica: 22/04/2026*
