# Sprint 4 — Orari & Calcolo Disponibilità
> **Stato**: ⬜ Non iniziato  
> **Dipende da**: Sprint 2 ✅ (servizi con durata) + Sprint 3 ✅ (poltrone)  
> **Obiettivo**: Implementare il motore di calcolo degli slot disponibili — il cuore algoritmico del sistema. Dopo questo sprint il sistema è in grado di rispondere "quali orari sono liberi per questo servizio in questo giorno su questa poltrona?".

---

## Requisiti Funzionali Coperti

| RF | Nome | Attore | Priorità |
|----|------|--------|----------|
| RF_BAR_9 | Definizione Orari di Apertura | BAR | Alta |
| RF_BAR_10 | Gestione Pause | BAR | Alta |
| RF_CLI_3 | Filtro per Giorno | CLI | Alta |
| RF_CLI_4 | Visualizzazione Slot Liberi | CLI | Alta |

---

## Indice Fasi

1. [Fase 4.1 — Modello di Dominio](#fase-41--modello-di-dominio)
2. [Fase 4.2 — Migrazione Flyway](#fase-42--migrazione-flyway)
3. [Fase 4.3 — Strategy Pattern: AvailabilityStrategy](#fase-43--strategy-pattern-availabilitystrategy)
4. [Fase 4.4 — AvailabilityService (Orchestratore)](#fase-44--availabilityservice-orchestratore)
5. [Fase 4.5 — ScheduleService (Gestione Orari BAR)](#fase-45--scheduleservice-gestione-orari-bar)
6. [Fase 4.6 — REST Controllers](#fase-46--rest-controllers)
7. [Fase 4.7 — Unit Test (Intensivi)](#fase-47--unit-test-intensivi)
8. [Fase 4.8 — Integration Test](#fase-48--integration-test)
9. [Fase 4.9 — Verifica Quality Gate](#fase-49--verifica-quality-gate)

---

## Fase 4.1 — Modello di Dominio

**Obiettivo**: Definire le entità che rappresentano la configurazione temporale del salone.

### Enum — `DayOfWeek.java`
> Usare `java.time.DayOfWeek` nativo — non ridefinire.

### Enum — `ScheduleType.java`
```java
public enum ScheduleType {
    APERTURA,   // Fascia oraria lavorativa (slot prenotabili)
    PAUSA       // Fascia di pausa (slot bloccati)
}
```

### Entità JPA — `FasciaOraria.java`
```java
@Entity
@Table(name = "schedules",
       indexes = {
           @Index(name = "idx_schedule_chair_day", columnList = "chair_id, giorno_settimana"),
           @Index(name = "idx_schedule_type", columnList = "tipo")
       })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FasciaOraria {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chair_id", nullable = false)
    private Poltrona poltrona;

    @Enumerated(EnumType.STRING)
    @Column(name = "giorno_settimana", nullable = false)
    private DayOfWeek giornoSettimana;

    @Column(name = "ora_inizio", nullable = false)
    private LocalTime oraInizio;

    @Column(name = "ora_fine", nullable = false)
    private LocalTime oraFine;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private ScheduleType tipo;           // APERTURA | PAUSA

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
```

### Value Object — `TimeSlot.java` (NON persistito)
```java
/**
 * Value Object immutabile che rappresenta uno slot temporale disponibile.
 * Non è un'entità JPA — viene calcolato a runtime da AvailabilityStrategy.
 */
public record TimeSlot(LocalTime start, LocalTime end) {

    public TimeSlot {
        Objects.requireNonNull(start, "start non può essere null");
        Objects.requireNonNull(end, "end non può essere null");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("start deve essere prima di end");
        }
    }

    /** Verifica se questo slot si sovrappone con un altro */
    public boolean overlapsWith(TimeSlot other) {
        return this.start.isBefore(other.end) && other.start.isBefore(this.end);
    }

    /** Verifica se questo slot si sovrappone con un intervallo start-end */
    public boolean overlapsWith(LocalTime otherStart, LocalTime otherEnd) {
        return overlapsWith(new TimeSlot(otherStart, otherEnd));
    }

    /** Verifica se questo slot è contenuto interamente nell'orario di apertura */
    public boolean fitsWithin(LocalTime openTime, LocalTime closeTime) {
        return !start.isBefore(openTime) && !end.isAfter(closeTime);
    }
}
```

### Context Object — `AvailabilityContext.java`
```java
/**
 * Raggruppa tutti i dati necessari all'algoritmo di calcolo.
 * Pattern: Parameter Object — riduce la firma del metodo Strategy.
 */
public record AvailabilityContext(
    FasciaOraria schedule,              // fascia APERTURA per quel giorno
    List<FasciaOraria> breaks,          // fasce PAUSA per quella poltrona in quel giorno
    List<Prenotazione> existingBookings // prenotazioni IN_ATTESA + ACCETTATA in quel giorno
) {}
```

### DTO

```java
// Response: slot disponibili per poltrona
public record AvailabilityResponseDto(
    Long chairId,
    String chairName,
    List<TimeSlotDto> availableSlots
) {}

public record TimeSlotDto(
    String start,   // "HH:mm"
    String end      // "HH:mm"
) {}

// Request/Response per gestione orari BAR
public record CreateScheduleRequestDto(
    @NotNull Long chairId,
    @NotNull DayOfWeek giornoSettimana,
    @NotNull LocalTime oraInizio,
    @NotNull LocalTime oraFine,
    @NotNull ScheduleType tipo
) {}

public record ScheduleResponseDto(
    Long id,
    Long chairId,
    String chairName,
    DayOfWeek giornoSettimana,
    LocalTime oraInizio,
    LocalTime oraFine,
    ScheduleType tipo
) {}
```

### Attività
- [ ] Creare enum `ScheduleType`
- [ ] Creare entità `FasciaOraria.java`
- [ ] Creare Value Object `TimeSlot.java` (record immutabile)
- [ ] Creare `AvailabilityContext.java`
- [ ] Creare tutti i DTO
- [ ] Creare `FasciaOrariaRepository extends JpaRepository<FasciaOraria, Long>`

---

## Fase 4.2 — Migrazione Flyway

**Obiettivo**: Creare la tabella `schedules` e popolarla con gli orari iniziali del salone.

### `V8__schedules_schema.sql`
```sql
CREATE TABLE schedules (
    id                BIGSERIAL PRIMARY KEY,
    chair_id          BIGINT NOT NULL REFERENCES chairs(id) ON DELETE CASCADE,
    giorno_settimana  VARCHAR(20) NOT NULL,   -- MONDAY, TUESDAY, ecc.
    ora_inizio        TIME NOT NULL,
    ora_fine          TIME NOT NULL,
    tipo              VARCHAR(20) NOT NULL,   -- APERTURA | PAUSA
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_schedule_hours CHECK (ora_inizio < ora_fine),
    CONSTRAINT chk_schedule_type  CHECK (tipo IN ('APERTURA', 'PAUSA'))
);

CREATE INDEX idx_schedule_chair_day ON schedules(chair_id, giorno_settimana);
CREATE INDEX idx_schedule_tipo      ON schedules(tipo);
```

### `V9__seed_schedules.sql`
```sql
-- Orari di apertura: Lunedì-Sabato, 9:00-19:00, per entrambe le poltrone
-- Pausa pranzo: 13:00-15:00
-- Domenica: chiuso (nessuna fascia APERTURA = giorno chiuso)

-- Poltrona 1 (id=1): orari di apertura
INSERT INTO schedules (chair_id, giorno_settimana, ora_inizio, ora_fine, tipo)
SELECT 1, day, '09:00', '19:00', 'APERTURA'
FROM (VALUES ('MONDAY'),('TUESDAY'),('WEDNESDAY'),('THURSDAY'),('FRIDAY'),('SATURDAY')) AS t(day);

-- Poltrona 1: pausa pranzo
INSERT INTO schedules (chair_id, giorno_settimana, ora_inizio, ora_fine, tipo)
SELECT 1, day, '13:00', '15:00', 'PAUSA'
FROM (VALUES ('MONDAY'),('TUESDAY'),('WEDNESDAY'),('THURSDAY'),('FRIDAY'),('SATURDAY')) AS t(day);

-- Poltrona 2 (id=2): stessa configurazione
INSERT INTO schedules (chair_id, giorno_settimana, ora_inizio, ora_fine, tipo)
SELECT 2, day, '09:00', '19:00', 'APERTURA'
FROM (VALUES ('MONDAY'),('TUESDAY'),('WEDNESDAY'),('THURSDAY'),('FRIDAY'),('SATURDAY')) AS t(day);

INSERT INTO schedules (chair_id, giorno_settimana, ora_inizio, ora_fine, tipo)
SELECT 2, day, '13:00', '15:00', 'PAUSA'
FROM (VALUES ('MONDAY'),('TUESDAY'),('WEDNESDAY'),('THURSDAY'),('FRIDAY'),('SATURDAY')) AS t(day);
```

### Attività
- [ ] Creare `V8__schedules_schema.sql`
- [ ] Creare `V9__seed_schedules.sql`
- [ ] Verificare CHECK constraints: `ora_inizio < ora_fine`, tipo valido
- [ ] Verificare seed: 12 record APERTURA + 12 PAUSA = 24 record totali

---

## Fase 4.3 — Strategy Pattern: AvailabilityStrategy

**Obiettivo**: Implementare l'algoritmo di calcolo degli slot con Strategy Pattern per garantire testabilità e sostituibilità futura.

### Interfaccia — `AvailabilityStrategy.java`
```java
/**
 * Strategy Pattern: definisce il contratto per il calcolo degli slot liberi.
 * Implementazioni diverse possono essere iniettate da Spring senza modificare
 * il Service che le usa.
 */
public interface AvailabilityStrategy {
    /**
     * Calcola gli slot temporali liberi per una poltrona in un dato giorno.
     *
     * @param date            il giorno per cui calcolare la disponibilità
     * @param serviceDuration la durata del servizio selezionato
     * @param context         orari di apertura, pause, prenotazioni esistenti
     * @return lista ordinata di slot disponibili (può essere vuota)
     */
    List<TimeSlot> calculateAvailableSlots(
        LocalDate date,
        Duration serviceDuration,
        AvailabilityContext context
    );
}
```

### Implementazione Standard — `StandardAvailabilityStrategy.java`
```java
@Component
@Primary  // implementazione default — può essere sostituita senza modificare AvailabilityService
public class StandardAvailabilityStrategy implements AvailabilityStrategy {

    private static final int SLOT_GRANULARITY_MINUTES = 15;

    @Override
    public List<TimeSlot> calculateAvailableSlots(
            LocalDate date,
            Duration serviceDuration,
            AvailabilityContext context) {

        // 1. Nessuna fascia di apertura per questo giorno → salone chiuso
        if (context.schedule() == null) {
            return Collections.emptyList();
        }

        // 2. Genera tutti gli slot teorici del giorno con la granularità di 15 min
        List<TimeSlot> allSlots = generateAllSlots(
            context.schedule().getOraInizio(),
            context.schedule().getOraFine(),
            serviceDuration
        );

        // 3. Filtra gli slot che cadono in una pausa
        List<TimeSlot> notInBreaks = filterBreaks(allSlots, context.breaks());

        // 4. Filtra gli slot che si sovrappongono con prenotazioni esistenti
        return filterBookedSlots(notInBreaks, context.existingBookings(), serviceDuration);
    }

    /**
     * Genera tutti gli slot del giorno con step di SLOT_GRANULARITY_MINUTES.
     * Uno slot è incluso solo se TERMINA entro l'orario di chiusura.
     */
    private List<TimeSlot> generateAllSlots(LocalTime openTime, LocalTime closeTime,
                                             Duration duration) {
        List<TimeSlot> slots = new ArrayList<>();
        LocalTime current = openTime;

        while (true) {
            LocalTime slotEnd = current.plus(duration);
            if (slotEnd.isAfter(closeTime)) break;  // lo slot sfora → stop
            slots.add(new TimeSlot(current, slotEnd));
            current = current.plusMinutes(SLOT_GRANULARITY_MINUTES);
        }

        return slots;
    }

    /**
     * Rimuove gli slot che si sovrappongono con almeno una fascia di pausa.
     */
    private List<TimeSlot> filterBreaks(List<TimeSlot> slots, List<FasciaOraria> breaks) {
        return slots.stream()
            .filter(slot -> breaks.stream().noneMatch(b ->
                slot.overlapsWith(b.getOraInizio(), b.getOraFine())
            ))
            .collect(Collectors.toList());
    }

    /**
     * Rimuove gli slot che si sovrappongono con prenotazioni IN_ATTESA o ACCETTATA.
     */
    private List<TimeSlot> filterBookedSlots(List<TimeSlot> slots,
                                              List<Prenotazione> bookings,
                                              Duration duration) {
        return slots.stream()
            .filter(slot -> bookings.stream().noneMatch(b ->
                slot.overlapsWith(
                    b.getStartTime().toLocalTime(),
                    b.getEndTime().toLocalTime()
                )
            ))
            .collect(Collectors.toList());
    }
}
```

### Attività
- [ ] Creare interfaccia `AvailabilityStrategy`
- [ ] Creare `StandardAvailabilityStrategy` con i 3 step (genera → filtra pause → filtra prenotazioni)
- [ ] Creare `TimeSlot` record con metodo `overlapsWith`
- [ ] Verificare che la Strategy non abbia dipendenze da Spring (POJO puro → testabilità massima)

---

## Fase 4.4 — AvailabilityService (Orchestratore)

**Obiettivo**: Orchestrare la raccolta dei dati necessari e delegare il calcolo alla Strategy.

### `AvailabilityService.java`
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityService {

    private final AvailabilityStrategy availabilityStrategy;
    private final PoltronaRepository poltronaRepository;
    private final FasciaOrariaRepository fasciaOrariaRepository;
    private final ServizioRepository servizioRepository;
    private final PrenotazioneRepository prenotazioneRepository;  // Sprint 5 — forward reference

    /**
     * RF_CLI_3 + RF_CLI_4 — Calcola gli slot disponibili per tutte le poltrone attive.
     *
     * @param date      il giorno richiesto dal cliente
     * @param serviceId il servizio selezionato (ne serve la durata)
     * @return mappa poltrona → lista slot disponibili
     */
    public List<AvailabilityResponseDto> getAvailableSlots(LocalDate date, Long serviceId) {
        // Recupera il servizio (con la durata)
        Servizio servizio = servizioRepository.findByIdAndAttivoTrue(serviceId)
            .orElseThrow(() -> new ResourceNotFoundException("Servizio non trovato: " + serviceId));
        Duration duration = Duration.ofMinutes(servizio.getDurataMinuti());

        // Recupera tutte le poltrone attive
        List<Poltrona> poltrone = poltronaRepository.findByAttivaTrue();

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        return poltrone.stream().map(poltrona -> {
            // Fascia di APERTURA per questa poltrona in questo giorno
            FasciaOraria apertura = fasciaOrariaRepository
                .findByPoltronaAndGiornoSettimanaAndTipo(poltrona, dayOfWeek, ScheduleType.APERTURA)
                .orElse(null);  // null = giorno chiuso

            // Fasce di PAUSA per questa poltrona in questo giorno
            List<FasciaOraria> pause = fasciaOrariaRepository
                .findByPoltronaAndGiornoSettimanaAndTipo(poltrona, dayOfWeek, ScheduleType.PAUSA);

            // Prenotazioni esistenti IN_ATTESA + ACCETTATA per questa poltrona in questo giorno
            List<Prenotazione> prenotazioni = prenotazioneRepository
                .findActiveBookingsByChairAndDate(poltrona.getId(), date);

            AvailabilityContext context = new AvailabilityContext(apertura, pause, prenotazioni);

            List<TimeSlot> slots = availabilityStrategy.calculateAvailableSlots(
                date, duration, context);

            return new AvailabilityResponseDto(
                poltrona.getId(),
                poltrona.getNome(),
                slots.stream()
                    .map(s -> new TimeSlotDto(
                        s.start().toString(),
                        s.end().toString()
                    ))
                    .collect(Collectors.toList())
            );
        }).collect(Collectors.toList());
    }

    /**
     * Verifica se uno specifico slot è disponibile per una poltrona.
     * Usato da BookingService (Sprint 5) per validare la richiesta di prenotazione.
     */
    public void assertSlotIsAvailable(Long chairId, LocalDateTime startTime, LocalDateTime endTime) {
        boolean hasOverlap = prenotazioneRepository
            .existsActiveBookingInSlot(chairId, startTime, endTime);
        if (hasOverlap) {
            throw new SlotNotAvailableException("Lo slot selezionato non è più disponibile");
        }
    }
}
```

> **Nota**: `PrenotazioneRepository` è referenziato in anticipo rispetto a Sprint 5. In questo sprint, il metodo `findActiveBookingsByChairAndDate` e `existsActiveBookingInSlot` possono ritornare liste vuote (nessuna prenotazione ancora) e il calcolo funzionerà comunque correttamente.

### Repository aggiuntivo — `FasciaOrariaRepository.java`
```java
public interface FasciaOrariaRepository extends JpaRepository<FasciaOraria, Long> {

    // Fascia di APERTURA per poltrona+giorno (al massimo 1)
    Optional<FasciaOraria> findByPoltronaAndGiornoSettimanaAndTipo(
        Poltrona poltrona, DayOfWeek giorno, ScheduleType tipo);

    // Tutte le fasce (APERTURA + PAUSA) di una poltrona
    List<FasciaOraria> findByPoltronaAndGiornoSettimana(
        Poltrona poltrona, DayOfWeek giorno);

    // Solo le PAUSE di una poltrona in un giorno
    List<FasciaOraria> findByPoltronaAndGiornoSettimanaAndTipo(
        Poltrona poltrona, DayOfWeek giorno, ScheduleType tipo);

    // Tutte le fasce di una poltrona (per configurazione BAR)
    List<FasciaOraria> findByPoltrona(Poltrona poltrona);
}
```

### Attività
- [ ] Creare `AvailabilityService` con `getAvailableSlots` e `assertSlotIsAvailable`
- [ ] Creare `FasciaOrariaRepository` con query necessarie
- [ ] Creare `SlotNotAvailableException` → `409 Conflict`
- [ ] Pianificare il `forward reference` con `PrenotazioneRepository`: creare un'interfaccia minima (vuota o con metodi stub) per consentire la compilazione

---

## Fase 4.5 — ScheduleService (Gestione Orari BAR)

**Obiettivo**: Permettere al BAR di configurare gli orari e le pause del salone.

### `ScheduleService.java`
```java
@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {

    private final FasciaOrariaRepository fasciaOrariaRepository;
    private final PoltronaRepository poltronaRepository;
    private final FasciaOrariaMapper fasciaOrariaMapper;

    // RF_BAR_9 — Ottieni tutti gli orari per una poltrona
    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> getScheduleForChair(Long chairId) {
        Poltrona poltrona = poltronaRepository.findByIdAndAttivaTrue(chairId)
            .orElseThrow(() -> new ResourceNotFoundException("Poltrona non trovata: " + chairId));
        return fasciaOrariaMapper.toDtoList(fasciaOrariaRepository.findByPoltrona(poltrona));
    }

    // RF_BAR_9 — Crea una fascia di apertura
    public ScheduleResponseDto addSchedule(CreateScheduleRequestDto dto) {
        Poltrona poltrona = poltronaRepository.findByIdAndAttivaTrue(dto.chairId())
            .orElseThrow(() -> new ResourceNotFoundException("Poltrona non trovata: " + dto.chairId()));
        validateTimeRange(dto.oraInizio(), dto.oraFine());

        FasciaOraria fascia = FasciaOraria.builder()
            .poltrona(poltrona)
            .giornoSettimana(dto.giornoSettimana())
            .oraInizio(dto.oraInizio())
            .oraFine(dto.oraFine())
            .tipo(dto.tipo())
            .createdAt(LocalDateTime.now())
            .build();

        return fasciaOrariaMapper.toDto(fasciaOrariaRepository.save(fascia));
    }

    // RF_BAR_9/10 — Elimina una fascia oraria
    public void removeSchedule(Long scheduleId) {
        FasciaOraria fascia = fasciaOrariaRepository.findById(scheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Fascia oraria non trovata: " + scheduleId));
        fasciaOrariaRepository.delete(fascia);
    }

    private void validateTimeRange(LocalTime start, LocalTime end) {
        if (!start.isBefore(end)) {
            throw new InvalidTimeRangeException("L'orario di inizio deve essere prima di quello di fine");
        }
    }
}
```

### Attività
- [ ] Creare `ScheduleService` con CRUD fasce orarie
- [ ] Creare `FasciaOrariaMapper`
- [ ] Creare `InvalidTimeRangeException` → `400 Bad Request`

---

## Fase 4.6 — REST Controllers

### `AvailabilityController.java` (pubblico)
```java
@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    /** RF_CLI_3 + RF_CLI_4 — Slot disponibili per un giorno e servizio */
    @GetMapping
    public ResponseEntity<List<AvailabilityResponseDto>> getAvailableSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Long serviceId) {
        return ResponseEntity.ok(availabilityService.getAvailableSlots(date, serviceId));
    }
}
```

### `ScheduleController.java` (BAR-only)
```java
@RestController
@RequestMapping("/api/schedules")
@PreAuthorize("hasRole('BARBER')")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/chairs/{chairId}")
    public ResponseEntity<List<ScheduleResponseDto>> getByChair(@PathVariable Long chairId) {
        return ResponseEntity.ok(scheduleService.getScheduleForChair(chairId));
    }

    @PostMapping
    public ResponseEntity<ScheduleResponseDto> add(
            @Valid @RequestBody CreateScheduleRequestDto dto) {
        return ResponseEntity.status(201).body(scheduleService.addSchedule(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        scheduleService.removeSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
```

### API Endpoints Riepilogo
| Metodo | Path | Auth | RF |
|--------|------|------|----|
| GET | `/api/availability?date={date}&serviceId={id}` | Pubblico | RF_CLI_3, RF_CLI_4 |
| GET | `/api/schedules/chairs/{chairId}` | BARBER | RF_BAR_9 |
| POST | `/api/schedules` | BARBER | RF_BAR_9, RF_BAR_10 |
| DELETE | `/api/schedules/{id}` | BARBER | RF_BAR_9, RF_BAR_10 |

### Attività
- [ ] Creare `AvailabilityController` (pubblico)
- [ ] Creare `ScheduleController` (BARBER-only)
- [ ] Aggiornare `SecurityConfig`: `/api/availability` è pubblico

---

## Fase 4.7 — Unit Test (Intensivi)

**Obiettivo**: Questa fase è la più ricca di test unitari dell'intero progetto. La Strategy è un POJO puro — nessun mock necessario.

### `StandardAvailabilityStrategyTest.java` — Test PURI (nessun mock)
```java
class StandardAvailabilityStrategyTest {

    private final StandardAvailabilityStrategy strategy = new StandardAvailabilityStrategy();

    // Helper per creare contesti di test
    private FasciaOraria apertura(LocalTime start, LocalTime end) { ... }
    private FasciaOraria pausa(LocalTime start, LocalTime end) { ... }
    private Prenotazione booking(LocalTime start, LocalTime end) { ... }

    // -------------------------------------------------------
    // Test: generazione slot
    // -------------------------------------------------------
    @Test void noBreaks_noBookings_returnsAllPossibleSlots_30min()
    // 9:00-13:00, servizio 30min, step 15min → 14 slot: 9:00, 9:15... 12:30
    // (slot 12:31 inizia a 12:31, finisce a 13:01 → ESCLUSO perché sfora 13:00)

    @Test void noBreaks_noBookings_returnsAllPossibleSlots_20min()
    @Test void serviceDurationEqualsOpeningHours_onlyOneSlot()
    @Test void serviceDurationExceedsOpeningHours_noSlotsReturned()
    @Test void closedDay_scheduleNull_returnsEmptyList()

    // -------------------------------------------------------
    // Test: filtro pause
    // -------------------------------------------------------
    @Test void singleBreak_pauseSlotExcluded()
    @Test void breakAtStartOfDay_slotsBeforeBreakNotAffected()
    @Test void breakCoversWholeDay_noSlotsReturned()
    @Test void multipleBreaks_allBreakSlotsExcluded()
    @Test void slotPartiallyInBreak_excluded()
    // es. slot 12:45-13:15 con pausa 13:00-15:00 → escluso (si sovrappone)

    // -------------------------------------------------------
    // Test: filtro prenotazioni
    // -------------------------------------------------------
    @Test void existingBooking_overlappingSlotRemoved()
    @Test void existingBooking_adjacentSlot_notRemoved()
    // es. prenotazione 10:00-10:30 → slot 10:30-11:00 è LIBERO
    @Test void existingBooking_onlyInAttesaAndAccettataBlocked()
    // Note: questo test richiede mock delle prenotazioni con status diversi
    @Test void multipleBookings_allBlockedSlots_removed()
    @Test void bookingAtEndOfDay_onlyLastSlotAffected()

    // -------------------------------------------------------
    // Test: combinazioni
    // -------------------------------------------------------
    @Test void breakAndBooking_combined_correctSlotsRemaining()
    @Test void lunchBreak_standardConfiguration_correctSlots()
    // 9:00-19:00 apertura, 13:00-15:00 pausa, servizio 30min
    // → slot mattina: 9:00..12:30 (14 slot)
    // → pausa esclusa
    // → slot pomeriggio: 15:00..18:30 (15 slot) = 29 slot totali

    @Test void multipleChairs_eachCalculatedIndependently()
    @Test void futureDateWithNoBookings_allSlotsAvailable()
}
```

### `AvailabilityServiceTest.java` — Con Mockito
```java
@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock AvailabilityStrategy availabilityStrategy;
    @Mock PoltronaRepository poltronaRepository;
    @Mock FasciaOrariaRepository fasciaOrariaRepository;
    @Mock ServizioRepository servizioRepository;
    @Mock PrenotazioneRepository prenotazioneRepository;
    @InjectMocks AvailabilityService availabilityService;

    @Test void getAvailableSlots_activeChairs_callsStrategyForEachChair()
    @Test void getAvailableSlots_closedDay_returnsEmptyListForAllChairs()
    @Test void getAvailableSlots_unknownService_throwsResourceNotFoundException()
    @Test void getAvailableSlots_inactiveService_throwsResourceNotFoundException()
    @Test void assertSlotIsAvailable_noOverlap_noException()
    @Test void assertSlotIsAvailable_overlap_throwsSlotNotAvailableException()
}
```

### `TimeSlotTest.java` — Value Object
```java
class TimeSlotTest {

    @Test void constructor_validTimes_success()
    @Test void constructor_startAfterEnd_throwsException()
    @Test void constructor_startEqualsEnd_throwsException()
    @Test void overlapsWith_overlappingSlot_returnsTrue()
    @Test void overlapsWith_adjacentSlot_returnsFalse()
    @Test void overlapsWith_sameSlot_returnsTrue()
    @Test void overlapsWith_containedSlot_returnsTrue()
    @Test void overlapsWith_nonOverlappingSlot_returnsFalse()
    @Test void fitsWithin_slotInsideOpeningHours_returnsTrue()
    @Test void fitsWithin_slotStartsAtOpenTime_returnsTrue()
    @Test void fitsWithin_slotEndsAtCloseTime_returnsTrue()
    @Test void fitsWithin_slotExceedsCloseTime_returnsFalse()
}
```

### Attività
- [ ] Implementare `StandardAvailabilityStrategyTest` — minimo 15 scenari
- [ ] Implementare `AvailabilityServiceTest`
- [ ] Implementare `TimeSlotTest`
- [ ] Coverage `StandardAvailabilityStrategy` ≥ 90% (logica critica)
- [ ] Coverage `AvailabilityService` ≥ 80%

---

## Fase 4.8 — Integration Test

**Obiettivo**: Verificare l'endpoint di disponibilità con DB reale.

### `AvailabilityIntegrationTest.java`
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class AvailabilityIntegrationTest {

    @Test void getAvailableSlots_validDateAndService_returns200()
    @Test void getAvailableSlots_closedDay_returnsEmptySlots()
    @Test void getAvailableSlots_invalidServiceId_returns404()
    @Test void getAvailableSlots_noAuth_returns200()  // endpoint pubblico
    @Test void getAvailableSlots_seedData_correctSlots()
    // Con seed (9:00-19:00, pausa 13:00-15:00, servizio 30min) → 29 slot per poltrona

    @Test void addSchedule_asBarber_returns201()
    @Test void addSchedule_invalidTimeRange_returns400()
    @Test void removeSchedule_asBarber_returns204()
}
```

### Attività
- [ ] Implementare `AvailabilityIntegrationTest`
- [ ] Verificare il risultato del seed: 29 slot disponibili per il lunedì con servizio "Capelli" (30 min)

---

## Fase 4.9 — Verifica Quality Gate

### Checklist finale Sprint 4
- [ ] `mvn verify` → BUILD SUCCESS
- [ ] JaCoCo: `StandardAvailabilityStrategy` LINE ≥ 90%
- [ ] JaCoCo: `AvailabilityService` LINE ≥ 80%
- [ ] Tutti i 15+ test unitari sulla Strategy passano
- [ ] Endpoint `/api/availability` risponde correttamente con slot vuoti per domenica (giorno chiuso)
- [ ] Slot che sfora l'orario di chiusura NON incluso (test critico!)
- [ ] Push su `develop` → GitHub Actions verde

---

## Definition of Done — Sprint 4

| Criterio | Verifica |
|----------|----------|
| ✅ RF_BAR_9 Orari di apertura | BAR configura orari per giorno e poltrona |
| ✅ RF_BAR_10 Pause | BAR aggiunge/rimuove pause per giorno e poltrona |
| ✅ RF_CLI_3 Filtro per giorno | `GET /api/availability?date=...` funzionante |
| ✅ RF_CLI_4 Slot liberi | Risposta include per ogni poltrona attiva i suoi slot liberi |
| ✅ Strategy Pattern implementato | `AvailabilityStrategy` iniettabile, sostituibile |
| ✅ TimeSlot Value Object | Record immutabile con `overlapsWith` testato |
| ✅ Granularità 15 minuti | Slot generati ogni 15 min all'interno del orario di apertura |
| ✅ Pause filtrate correttamente | Slot sovrapposti con pause NON inclusi |
| ✅ Prenotazioni esistenti filtrate | Slot sovrapposti con prenotazioni ACCETTATE/IN_ATTESA NON inclusi |
| ✅ Slot che sfora l'orario | Non incluso se termina dopo il closing time |
| ✅ Domenica = chiuso | Lista vuota quando non c'è fascia APERTURA |
| ✅ ≥ 15 unit test sulla Strategy | Tutti i casi limite coperti |
| ✅ CI pipeline verde | GitHub Actions passa |

---

## Note Operative

- La **granularità di 15 minuti** è una scelta di design: gli slot "iniziano" ogni 15 minuti. Per un servizio di 30 min, i possibili inizi sono 9:00, 9:15, 9:30... — non solo 9:00, 9:30.
- Lo **stato delle prenotazioni** considerato nel filtro: `IN_ATTESA` e `ACCETTATA`. Le prenotazioni `RIFIUTATA` e `ANNULLATA` non bloccano lo slot (verranno aggiunte in Sprint 5 insieme all'implementazione completa di `PrenotazioneRepository`).
- Il **JMH benchmark** su `AvailabilityService` è pianificato per essere eseguito manualmente in questa fase (non in CI). Eseguire: `mvn jmh:run` dopo aver implementato il modulo JMH.

---

*Sprint 4 — Ultima modifica: 22/04/2026*
