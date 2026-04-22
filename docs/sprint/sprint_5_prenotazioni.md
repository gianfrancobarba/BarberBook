# Sprint 5 — Prenotazioni (Core)
> **Stato**: ⬜ Non iniziato  
> **Dipende da**: Sprint 4 ✅ (disponibilità) + Sprint 1 ✅ (utenti)  
> **Obiettivo**: Il cuore operativo di BarberBook. Tutto il ciclo di vita delle prenotazioni — dalla richiesta del cliente all'approvazione del BAR, fino all'annullamento — con garanzie di consistenza a doppio livello (applicativo + database).

---

## Requisiti Funzionali Coperti

| RF | Nome | Attore | Priorità |
|----|------|--------|----------|
| RF_CLI_5 | Selezione Servizio in Prenotazione | CLI | Alta |
| RF_CLI_6 | Invio Richiesta di Prenotazione | CLI | Alta |
| RF_CLG_1 | Form Dati Ospite | CLG | Alta |
| RF_BAR_11 | Creazione Prenotazione Diretta | BAR | Alta |
| RF_BAR_12 | Modifica Prenotazione | BAR | Alta |
| RF_BAR_13 | Cancellazione Prenotazione | BAR | Alta |
| RF_BAR_14 | Accettazione Richiesta | BAR | Alta |
| RF_BAR_15 | Rifiuto Richiesta | BAR | Alta |
| RF_CLR_4 | Annullamento Prenotazione con motivazione | CLR | Alta |

---

## Indice Fasi

1. [Fase 5.1 — State Machine: BookingStatus](#fase-51--state-machine-bookingstatus)
2. [Fase 5.2 — Entità Prenotazione & GuestData](#fase-52--entità-prenotazione--guestdata)
3. [Fase 5.3 — Migrazione Flyway (con Exclusion Constraint GiST)](#fase-53--migrazione-flyway-con-exclusion-constraint-gist)
4. [Fase 5.4 — Repository & ValidationChain](#fase-54--repository--validationchain)
5. [Fase 5.5 — BookingService (Facade)](#fase-55--bookingservice-facade)
6. [Fase 5.6 — REST Controller](#fase-56--rest-controller)
7. [Fase 5.7 — Scheduled Task: transizione a PASSATA](#fase-57--scheduled-task-transizione-a-passata)
8. [Fase 5.8 — Unit Test (State Machine)](#fase-58--unit-test-state-machine)
9. [Fase 5.9 — Unit Test (BookingService & ValidationChain)](#fase-59--unit-test-bookingservice--validationchain)
10. [Fase 5.10 — Integration Test (no-double-booking)](#fase-510--integration-test-no-double-booking)
11. [Fase 5.11 — PiTest Mutation Testing](#fase-511--pitest-mutation-testing)
12. [Fase 5.12 — Verifica Quality Gate](#fase-512--verifica-quality-gate)

---

## Fase 5.1 — State Machine: BookingStatus

**Obiettivo**: Implementare la state machine come enum Java con transizioni validate e immutabili.  
Questo è il **Pattern State** — ogni transizione illegale lancia un'eccezione prima ancora di toccare il database.

### `BookingStatus.java`
```java
public enum BookingStatus {
    IN_ATTESA,
    ACCETTATA,
    RIFIUTATA,
    ANNULLATA,
    PASSATA;

    // Mappa immutabile delle transizioni valide
    private static final Map<BookingStatus, Set<BookingStatus>> VALID_TRANSITIONS =
        Map.of(
            IN_ATTESA, Set.of(ACCETTATA, RIFIUTATA),
            ACCETTATA, Set.of(ANNULLATA, PASSATA),
            RIFIUTATA, Set.of(),    // stato terminale
            ANNULLATA, Set.of(),    // stato terminale
            PASSATA,   Set.of()     // stato terminale
        );

    /**
     * Verifica se la transizione verso 'next' è legale.
     */
    public boolean canTransitionTo(BookingStatus next) {
        return VALID_TRANSITIONS.get(this).contains(next);
    }

    /**
     * Esegue la transizione o lancia InvalidBookingTransitionException.
     *
     * @param next lo stato di destinazione
     * @return lo stato 'next' se la transizione è valida
     * @throws InvalidBookingTransitionException se la transizione è illegale
     */
    public BookingStatus transitionTo(BookingStatus next) {
        if (!canTransitionTo(next)) {
            throw new InvalidBookingTransitionException(
                String.format("Transizione non valida: %s → %s", this, next)
            );
        }
        return next;
    }

    /** Ritorna true se questo stato non ha ulteriori transizioni possibili */
    public boolean isTerminal() {
        return VALID_TRANSITIONS.get(this).isEmpty();
    }
}
```

### Implementazione State Machine (diagramma)
```
        [CLR/CLG invia richiesta]        [BAR crea prenotazione diretta]
                   │                                   │
                   ▼                                   ▼
            ┌─────────────┐                    ┌─────────────┐
            │  IN_ATTESA  │                    │  ACCETTATA  │ ← stato iniziale diretto
            └──────┬──────┘                    └──────┬──────┘
                   │                                  │
    [BAR: accetta] │ [BAR: rifiuta]   [CLR/BAR: annulla] │ [tempo scaduto]
                   │                                  │
         ┌─────────┴──────────────┐            ┌─────┴─────────────┐
         ▼                        ▼             ▼                    ▼
  ┌─────────────┐         ┌──────────────┐ ┌──────────┐      ┌──────────┐
  │  ACCETTATA  │         │  RIFIUTATA   │ │ ANNULLATA│      │  PASSATA │
  └─────────────┘         └────────────--┘ └──────────┘      └──────────┘
                                ↑ terminale    ↑ terminale     ↑ terminale
```

### Eccezione custom
```java
public class InvalidBookingTransitionException extends RuntimeException {
    public InvalidBookingTransitionException(String message) {
        super(message);
    }
}
// → GlobalExceptionHandler → 409 Conflict
```

### Attività
- [ ] Creare enum `BookingStatus` con `VALID_TRANSITIONS`, `canTransitionTo`, `transitionTo`, `isTerminal`
- [ ] Creare `InvalidBookingTransitionException` → `409 Conflict`
- [ ] Aggiungere handler nel `GlobalExceptionHandler`

---

## Fase 5.2 — Entità Prenotazione & GuestData

**Obiettivo**: Definire l'entità centrale del sistema con tutti i campi, associazioni e DTO.

### Embeddable — `GuestData.java`
```java
@Embeddable
public class GuestData {
    @Column(name = "guest_nome")
    private String nome;

    @Column(name = "guest_cognome")
    private String cognome;

    @Column(name = "guest_telefono")
    private String telefono;
}
```

### Entità JPA — `Prenotazione.java`
```java
@Entity
@Table(name = "bookings",
       indexes = {
           @Index(name = "idx_booking_chair_date", columnList = "chair_id, start_time"),
           @Index(name = "idx_booking_client", columnList = "client_id"),
           @Index(name = "idx_booking_status", columnList = "status")
       })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Prenotazione {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chair_id", nullable = false)
    private Poltrona poltrona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Servizio servizio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")   // nullable: null per CLG
    private User client;

    @Embedded
    private GuestData guestData;        // valorizzato per CLG, null per CLR

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;      // calcolato: startTime + servizio.durataMinuti

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BookingStatus status = BookingStatus.IN_ATTESA;

    @Column(name = "cancellation_reason", length = 1000)
    private String cancellationReason;  // obbligatorio solo per ANNULLATA da CLR (RF_CLR_4)

    @Version
    private Long version;               // Optimistic Locking — previene race condition

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    // --- Metodi di utilità ---

    /** Verifica se questa prenotazione si sovrappone con l'intervallo dato */
    public boolean overlaps(LocalDateTime start, LocalDateTime end) {
        return this.startTime.isBefore(end) && start.isBefore(this.endTime);
    }

    /** Nome da visualizzare in dashboard e notifiche */
    public String getCustomerDisplayName() {
        if (client != null) {
            return client.getNome() + " " + client.getCognome();
        }
        if (guestData != null) {
            return guestData.getNome() + " " + guestData.getCognome() + " (ospite)";
        }
        return "Cliente sconosciuto";
    }
}
```

### DTO

```java
// Request: CLR invia richiesta
public record BookingRequestDto(
    @NotNull Long chairId,
    @NotNull Long serviceId,
    @NotNull LocalDate date,
    @NotNull LocalTime startTime
) {}

// Request: CLG aggiunge dati ospite
public record GuestBookingRequestDto(
    @NotNull Long chairId,
    @NotNull Long serviceId,
    @NotNull LocalDate date,
    @NotNull LocalTime startTime,
    @NotBlank String guestNome,
    @NotBlank String guestCognome,
    @NotBlank @Pattern(regexp = "\\+?[0-9\\s\\-]{8,15}") String guestTelefono
) {}

// Request: BAR crea prenotazione diretta
public record DirectBookingRequestDto(
    @NotNull Long chairId,
    @NotNull Long serviceId,
    @NotNull LocalDate date,
    @NotNull LocalTime startTime,
    @NotBlank String customerName,    // nome libero (potrebbero essere clienti walk-in)
    @NotBlank String customerSurname,
    String customerPhone
) {}

// Request: BAR modifica prenotazione
public record UpdateBookingRequestDto(
    Long chairId,           // opzionale — solo se si cambia poltrona
    Long serviceId,         // opzionale — solo se si cambia servizio
    LocalDate date,         // opzionale — solo se si cambia giorno
    LocalTime startTime     // opzionale — solo se si cambia orario
) {}

// Request: CLR annulla con motivazione
public record CancelBookingRequestDto(
    @NotBlank @Size(min = 5, max = 1000) String reason
) {}

// Response: prenotazione (per tutti gli attori)
public record BookingResponseDto(
    Long id,
    Long chairId,
    String chairName,
    Long serviceId,
    String serviceName,
    Integer serviceDurationMinutes,
    LocalDateTime startTime,
    LocalDateTime endTime,
    BookingStatus status,
    String customerName,     // nombre cliente o ospite
    boolean isGuest,
    String guestPhone,       // null per CLR
    String cancellationReason,
    LocalDateTime createdAt
) {}
```

### Attività
- [ ] Creare `GuestData` embeddable
- [ ] Creare `Prenotazione` entity con tutti i campi + `@Version`
- [ ] Creare tutti i DTO di request e response
- [ ] Creare `BookingMapper` con MapStruct
- [ ] Creare `PrenotazioneRepository extends JpaRepository, JpaSpecificationExecutor`

---

## Fase 5.3 — Migrazione Flyway (con Exclusion Constraint GiST)

**Obiettivo**: Creare la tabella `bookings` con il constraint GiST che garantisce il no-double-booking a livello di database — indipendente dall'applicazione.

### `V10__bookings_schema.sql`
```sql
-- Abilita l'estensione btree_gist (necessaria per GiST con type range + eguaglianza)
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE bookings (
    id                  BIGSERIAL PRIMARY KEY,
    chair_id            BIGINT NOT NULL REFERENCES chairs(id),
    service_id          BIGINT NOT NULL REFERENCES services(id),
    client_id           BIGINT REFERENCES users(id),    -- null per CLG

    -- Dati ospite (CLG)
    guest_nome          VARCHAR(100),
    guest_cognome       VARCHAR(100),
    guest_telefono      VARCHAR(20),

    start_time          TIMESTAMP NOT NULL,
    end_time            TIMESTAMP NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'IN_ATTESA',

    cancellation_reason VARCHAR(1000),
    version             BIGINT NOT NULL DEFAULT 0,      -- Optimistic Locking
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,

    CONSTRAINT chk_booking_times   CHECK (start_time < end_time),
    CONSTRAINT chk_booking_status  CHECK (status IN ('IN_ATTESA','ACCETTATA','RIFIUTATA','ANNULLATA','PASSATA')),
    CONSTRAINT chk_guest_or_client CHECK (
        (client_id IS NOT NULL AND guest_nome IS NULL)
     OR (client_id IS NULL AND guest_nome IS NOT NULL AND guest_cognome IS NOT NULL AND guest_telefono IS NOT NULL)
    )
);

CREATE INDEX idx_booking_chair_date ON bookings(chair_id, start_time);
CREATE INDEX idx_booking_client     ON bookings(client_id);
CREATE INDEX idx_booking_status     ON bookings(status);
```

### `V11__bookings_gist_constraint.sql`
```sql
-- Exclusion Constraint con GiST: nessuna prenotazione può sovrapporsi
-- sulla stessa poltrona se entrambe sono IN_ATTESA o ACCETTATA.
-- Questo è il guard finale contro il double-booking, immune da race conditions.

ALTER TABLE bookings
ADD CONSTRAINT no_overlap_booking
EXCLUDE USING GIST (
    chair_id WITH =,
    tstzrange(start_time, end_time, '[)') WITH &&
)
WHERE (status IN ('IN_ATTESA', 'ACCETTATA'));
```

> **Perché `[)` (closed-open)?** L'intervallo `[start, end)` è semiaperto: due prenotazioni **adiacenti** (es. 10:00-10:30 e 10:30-11:00) sono permesse. Solo gli intervalli che si **sovrappongono** realmente vengono bloccati.

### Attività
- [ ] Creare `V10__bookings_schema.sql`
- [ ] Creare `V11__bookings_gist_constraint.sql`
- [ ] Verificare che `btree_gist` sia abilitato nel container di test
- [ ] Testare manualmente: INSERT di due prenotazioni con stesso slot → errore DB

---

## Fase 5.4 — Repository & ValidationChain

### Repository — `PrenotazioneRepository.java`
```java
public interface PrenotazioneRepository
        extends JpaRepository<Prenotazione, Long>, JpaSpecificationExecutor<Prenotazione> {

    // Prenotazioni attive (IN_ATTESA + ACCETTATA) per una poltrona in un giorno
    @Query("""
        SELECT p FROM Prenotazione p
        WHERE p.poltrona.id = :chairId
          AND DATE(p.startTime) = :date
          AND p.status IN ('IN_ATTESA', 'ACCETTATA')
        ORDER BY p.startTime
        """)
    List<Prenotazione> findActiveBookingsByChairAndDate(
        @Param("chairId") Long chairId,
        @Param("date") LocalDate date
    );

    // Verifica sovrapposizione per assertSlotIsAvailable (AvailabilityService)
    @Query("""
        SELECT COUNT(p) > 0 FROM Prenotazione p
        WHERE p.poltrona.id = :chairId
          AND p.status IN ('IN_ATTESA', 'ACCETTATA')
          AND p.startTime < :endTime
          AND p.endTime > :startTime
        """)
    boolean existsActiveBookingInSlot(
        @Param("chairId") Long chairId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    // Prenotazioni in attesa (per area notifiche BAR)
    List<Prenotazione> findByStatusOrderByCreatedAtAsc(BookingStatus status);

    // Prenotazioni del cliente (con filtro opzionale per stato)
    List<Prenotazione> findByClientOrderByStartTimeDesc(User client);

    // Prenotazioni future confermate del cliente (homepage CLR)
    @Query("""
        SELECT p FROM Prenotazione p
        WHERE p.client.id = :clientId
          AND p.status = 'ACCETTATA'
          AND p.startTime > :now
        ORDER BY p.startTime ASC
        """)
    List<Prenotazione> findUpcomingConfirmedByClient(
        @Param("clientId") Long clientId,
        @Param("now") LocalDateTime now
    );

    // Dashboard giornaliera BAR
    @Query("""
        SELECT p FROM Prenotazione p
        WHERE p.poltrona.id = :chairId
          AND DATE(p.startTime) = :date
          AND p.status NOT IN ('RIFIUTATA', 'ANNULLATA')
        ORDER BY p.startTime
        """)
    List<Prenotazione> findDailyBookingsByChair(
        @Param("chairId") Long chairId,
        @Param("date") LocalDate date
    );
}
```

### ValidationChain — Pattern Chain of Responsibility

**Interfaccia base**
```java
public interface BookingValidator {
    void validate(BookingValidationRequest request);
}

// Context object per la catena
public record BookingValidationRequest(
    Poltrona chair,
    Servizio service,
    FasciaOraria schedule,
    List<FasciaOraria> breaks,
    LocalDateTime startTime,
    LocalDateTime endTime,
    List<Prenotazione> existingBookings,
    Long excludeBookingId    // null per nuova prenotazione; usato per modifica
) {}
```

**Validator 1** — poltrona attiva
```java
@Component
@Order(1)
public class ChairActiveValidator implements BookingValidator {
    @Override
    public void validate(BookingValidationRequest req) {
        if (!req.chair().isAttiva()) {
            throw new BookingValidationException("La poltrona selezionata non è disponibile.");
        }
    }
}
```

**Validator 2** — slot nell'orario di apertura
```java
@Component
@Order(2)
public class SlotWithinScheduleValidator implements BookingValidator {
    @Override
    public void validate(BookingValidationRequest req) {
        if (req.schedule() == null) {
            throw new BookingValidationException("Il salone è chiuso nel giorno selezionato.");
        }
        LocalTime startT = req.startTime().toLocalTime();
        LocalTime endT = req.endTime().toLocalTime();
        if (startT.isBefore(req.schedule().getOraInizio()) ||
            endT.isAfter(req.schedule().getOraFine())) {
            throw new BookingValidationException("Lo slot richiesto è fuori dagli orari di apertura.");
        }
    }
}
```

**Validator 3** — slot non in pausa
```java
@Component
@Order(3)
public class SlotNotInBreakValidator implements BookingValidator {
    @Override
    public void validate(BookingValidationRequest req) {
        boolean inBreak = req.breaks().stream().anyMatch(b -> {
            LocalTime slotStart = req.startTime().toLocalTime();
            LocalTime slotEnd = req.endTime().toLocalTime();
            return slotStart.isBefore(b.getOraFine()) && b.getOraInizio().isBefore(slotEnd);
        });
        if (inBreak) {
            throw new BookingValidationException("Lo slot richiesto cade in una pausa del salone.");
        }
    }
}
```

**Validator 4** — nessuna sovrapposizione a livello applicativo
```java
@Component
@Order(4)
public class NoOverlapValidator implements BookingValidator {
    @Override
    public void validate(BookingValidationRequest req) {
        boolean hasOverlap = req.existingBookings().stream()
            .filter(b -> !b.getId().equals(req.excludeBookingId()))  // escludi sé stessa in modifica
            .anyMatch(b -> b.overlaps(req.startTime(), req.endTime()));
        if (hasOverlap) {
            throw new SlotNotAvailableException("Lo slot selezionato non è più disponibile.");
        }
    }
}
```

### Attività
- [ ] Completare `PrenotazioneRepository` con tutte le query JPQL
- [ ] Creare `BookingValidator` interfaccia
- [ ] Creare `BookingValidationRequest` record
- [ ] Creare i 4 validator con `@Order`
- [ ] Creare `BookingValidationException` → `400 Bad Request`
- [ ] Verificare che Spring inietti automaticamente la lista di validator ordinata

---

## Fase 5.5 — BookingService (Facade)

**Obiettivo**: Orchestrare l'intero ciclo di vita delle prenotazioni come Facade tra Controller e sub-sistemi.

### `BookingService.java`
```java
@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final PrenotazioneRepository prenotazioneRepository;
    private final PoltronaRepository poltronaRepository;
    private final ServizioRepository servizioRepository;
    private final FasciaOrariaRepository fasciaOrariaRepository;
    private final List<BookingValidator> validators;    // Chain: Spring inietta tutti in ordine @Order
    private final ApplicationEventPublisher eventPublisher;
    private final BookingMapper bookingMapper;

    // -------------------------------------------------------
    // RF_CLI_6 — CLR invia richiesta di prenotazione
    // -------------------------------------------------------
    public BookingResponseDto createRequest(BookingRequestDto dto, User client) {
        Poltrona chair = getActiveChairOrThrow(dto.chairId());
        Servizio service = getActiveServiceOrThrow(dto.serviceId());

        LocalDateTime start = dto.startTime().atDate(dto.date());
        LocalDateTime end = start.plusMinutes(service.getDurataMinuti());

        runValidationChain(chair, service, start, end, null);

        Prenotazione booking = Prenotazione.builder()
            .poltrona(chair).servizio(service).client(client)
            .startTime(start).endTime(end)
            .status(BookingStatus.IN_ATTESA)
            .createdAt(LocalDateTime.now())
            .build();

        Prenotazione saved = prenotazioneRepository.save(booking);
        eventPublisher.publishEvent(new BookingRequestCreatedEvent(this, saved));
        return bookingMapper.toDto(saved);
    }

    // -------------------------------------------------------
    // RF_CLG_1 — CLG invia richiesta di prenotazione (senza account)
    // -------------------------------------------------------
    public BookingResponseDto createGuestRequest(GuestBookingRequestDto dto) {
        Poltrona chair = getActiveChairOrThrow(dto.chairId());
        Servizio service = getActiveServiceOrThrow(dto.serviceId());

        LocalDateTime start = dto.startTime().atDate(dto.date());
        LocalDateTime end = start.plusMinutes(service.getDurataMinuti());

        runValidationChain(chair, service, start, end, null);

        GuestData guestData = new GuestData(dto.guestNome(), dto.guestCognome(), dto.guestTelefono());

        Prenotazione booking = Prenotazione.builder()
            .poltrona(chair).servizio(service).client(null)
            .guestData(guestData)
            .startTime(start).endTime(end)
            .status(BookingStatus.IN_ATTESA)
            .createdAt(LocalDateTime.now())
            .build();

        Prenotazione saved = prenotazioneRepository.save(booking);
        eventPublisher.publishEvent(new BookingRequestCreatedEvent(this, saved));
        return bookingMapper.toDto(saved);
    }

    // -------------------------------------------------------
    // RF_BAR_14 — BAR accetta richiesta
    // -------------------------------------------------------
    public void acceptRequest(Long bookingId) {
        Prenotazione booking = findOrThrow(bookingId);
        booking.setStatus(booking.getStatus().transitionTo(BookingStatus.ACCETTATA));  // State
        booking.setUpdatedAt(LocalDateTime.now());
        prenotazioneRepository.save(booking);
        eventPublisher.publishEvent(new BookingAcceptedEvent(this, booking));           // Observer
    }

    // -------------------------------------------------------
    // RF_BAR_15 — BAR rifiuta richiesta
    // -------------------------------------------------------
    public void rejectRequest(Long bookingId) {
        Prenotazione booking = findOrThrow(bookingId);
        booking.setStatus(booking.getStatus().transitionTo(BookingStatus.RIFIUTATA)); // State
        booking.setUpdatedAt(LocalDateTime.now());
        prenotazioneRepository.save(booking);
        eventPublisher.publishEvent(new BookingRejectedEvent(this, booking));          // Observer
    }

    // -------------------------------------------------------
    // RF_CLR_4 — CLR annulla con motivazione
    // -------------------------------------------------------
    public void cancelByClient(Long bookingId, String reason, User requester) {
        Prenotazione booking = findOrThrow(bookingId);

        // Verifica ownership: solo il proprietario può annullare
        if (booking.getClient() == null || !booking.getClient().getId().equals(requester.getId())) {
            throw new UnauthorizedOperationException("Non sei autorizzato ad annullare questa prenotazione");
        }

        booking.setStatus(booking.getStatus().transitionTo(BookingStatus.ANNULLATA)); // State
        booking.setCancellationReason(reason);
        booking.setUpdatedAt(LocalDateTime.now());
        prenotazioneRepository.save(booking);
        eventPublisher.publishEvent(new BookingCancelledByClientEvent(this, booking, reason));
    }

    // -------------------------------------------------------
    // RF_BAR_13 — BAR cancella prenotazione
    // -------------------------------------------------------
    public void cancelByBarber(Long bookingId) {
        Prenotazione booking = findOrThrow(bookingId);
        booking.setStatus(booking.getStatus().transitionTo(BookingStatus.ANNULLATA)); // State
        booking.setUpdatedAt(LocalDateTime.now());
        prenotazioneRepository.save(booking);
        eventPublisher.publishEvent(new BookingCancelledByBarberEvent(this, booking));
    }

    // -------------------------------------------------------
    // RF_BAR_11 — BAR crea prenotazione diretta (già ACCETTATA)
    // -------------------------------------------------------
    public BookingResponseDto createDirect(DirectBookingRequestDto dto) {
        Poltrona chair = getActiveChairOrThrow(dto.chairId());
        Servizio service = getActiveServiceOrThrow(dto.serviceId());

        LocalDateTime start = dto.startTime().atDate(dto.date());
        LocalDateTime end = start.plusMinutes(service.getDurataMinuti());

        runValidationChain(chair, service, start, end, null);

        GuestData guestData = new GuestData(dto.customerName(), dto.customerSurname(), dto.customerPhone());

        Prenotazione booking = Prenotazione.builder()
            .poltrona(chair).servizio(service).client(null)
            .guestData(guestData)
            .startTime(start).endTime(end)
            .status(BookingStatus.ACCETTATA)   // direttamente confermata (SD_2)
            .createdAt(LocalDateTime.now())
            .build();

        return bookingMapper.toDto(prenotazioneRepository.save(booking));
        // Nessun evento: il BAR non notifica sé stesso per prenotazioni dirette
    }

    // -------------------------------------------------------
    // RF_BAR_12 — BAR modifica prenotazione
    // -------------------------------------------------------
    public BookingResponseDto update(Long bookingId, UpdateBookingRequestDto dto) {
        Prenotazione booking = findOrThrow(bookingId);

        Poltrona chair = dto.chairId() != null ? getActiveChairOrThrow(dto.chairId()) : booking.getPoltrona();
        Servizio service = dto.serviceId() != null ? getActiveServiceOrThrow(dto.serviceId()) : booking.getServizio();

        LocalDate date = dto.date() != null ? dto.date() : booking.getStartTime().toLocalDate();
        LocalTime time = dto.startTime() != null ? dto.startTime() : booking.getStartTime().toLocalTime();

        LocalDateTime newStart = time.atDate(date);
        LocalDateTime newEnd = newStart.plusMinutes(service.getDurataMinuti());

        // Escludi la prenotazione stessa dalla verifica di sovrapposizione
        runValidationChain(chair, service, newStart, newEnd, bookingId);

        booking.setPoltrona(chair);
        booking.setServizio(service);
        booking.setStartTime(newStart);
        booking.setEndTime(newEnd);
        booking.setUpdatedAt(LocalDateTime.now());

        return bookingMapper.toDto(prenotazioneRepository.save(booking));
    }

    // -------------------------------------------------------
    // Recupero prenotazioni
    // -------------------------------------------------------

    @Transactional(readOnly = true)
    public List<BookingResponseDto> getPendingRequests() {
        return bookingMapper.toDtoList(
            prenotazioneRepository.findByStatusOrderByCreatedAtAsc(BookingStatus.IN_ATTESA));
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDto> getClientBookings(User client) {
        return bookingMapper.toDtoList(
            prenotazioneRepository.findByClientOrderByStartTimeDesc(client));
    }

    // -------------------------------------------------------
    // Metodi privati
    // -------------------------------------------------------

    private void runValidationChain(Poltrona chair, Servizio service,
                                     LocalDateTime start, LocalDateTime end, Long excludeId) {
        DayOfWeek dayOfWeek = start.getDayOfWeek();
        FasciaOraria schedule = fasciaOrariaRepository
            .findByPoltronaAndGiornoSettimanaAndTipo(chair, dayOfWeek, ScheduleType.APERTURA)
            .orElse(null);
        List<FasciaOraria> breaks = fasciaOrariaRepository
            .findByPoltronaAndGiornoSettimanaAndTipo(chair, dayOfWeek, ScheduleType.PAUSA);
        List<Prenotazione> existing = prenotazioneRepository
            .findActiveBookingsByChairAndDate(chair.getId(), start.toLocalDate());

        BookingValidationRequest req = new BookingValidationRequest(
            chair, service, schedule, breaks, start, end, existing, excludeId);

        validators.forEach(v -> v.validate(req));   // Chain of Responsibility
    }

    private Prenotazione findOrThrow(Long id) {
        return prenotazioneRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Prenotazione non trovata: " + id));
    }

    private Poltrona getActiveChairOrThrow(Long id) {
        return poltronaRepository.findByIdAndAttivaTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Poltrona non trovata: " + id));
    }

    private Servizio getActiveServiceOrThrow(Long id) {
        return servizioRepository.findByIdAndAttivoTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Servizio non trovato: " + id));
    }
}
```

### Domain Events (da creare in `domain/events/`)
```java
public abstract class BookingEvent extends ApplicationEvent {
    private final Prenotazione booking;
    // costruttore + getter
}

public class BookingRequestCreatedEvent extends BookingEvent { ... }
public class BookingAcceptedEvent extends BookingEvent { ... }
public class BookingRejectedEvent extends BookingEvent { ... }
public class BookingCancelledByClientEvent extends BookingEvent {
    private final String cancellationReason;
    // costruttore + getter
}
public class BookingCancelledByBarberEvent extends BookingEvent { ... }
```

### Attività
- [ ] Creare `BookingService` con tutti i metodi CRUD e transizione di stato
- [ ] Creare tutti i domain events
- [ ] Creare `UnauthorizedOperationException` → `403 Forbidden`
- [ ] Completare `BookingMapper`

---

## Fase 5.6 — REST Controller

### `BookingController.java`
```java
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /** RF_CLI_6 — CLR invia richiesta */
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<BookingResponseDto> createRequest(
            @Valid @RequestBody BookingRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(201).body(
            bookingService.createRequest(dto, principal.getUser()));
    }

    /** RF_CLG_1 — CLG invia richiesta senza account */
    @PostMapping("/guest")
    public ResponseEntity<BookingResponseDto> createGuestRequest(
            @Valid @RequestBody GuestBookingRequestDto dto) {
        return ResponseEntity.status(201).body(bookingService.createGuestRequest(dto));
    }

    /** RF_BAR_14 — BAR accetta */
    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<Void> accept(@PathVariable Long id) {
        bookingService.acceptRequest(id);
        return ResponseEntity.ok().build();
    }

    /** RF_BAR_15 — BAR rifiuta */
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<Void> reject(@PathVariable Long id) {
        bookingService.rejectRequest(id);
        return ResponseEntity.ok().build();
    }

    /** RF_BAR_11 — BAR crea prenotazione diretta */
    @PostMapping("/direct")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<BookingResponseDto> createDirect(
            @Valid @RequestBody DirectBookingRequestDto dto) {
        return ResponseEntity.status(201).body(bookingService.createDirect(dto));
    }

    /** RF_BAR_12 — BAR modifica */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<BookingResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingRequestDto dto) {
        return ResponseEntity.ok(bookingService.update(id, dto));
    }

    /** RF_BAR_13 — BAR cancella */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<Void> cancelByBarber(@PathVariable Long id) {
        bookingService.cancelByBarber(id);
        return ResponseEntity.noContent().build();
    }

    /** RF_CLR_4 — CLR annulla con motivazione */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Void> cancelByClient(
            @PathVariable Long id,
            @Valid @RequestBody CancelBookingRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        bookingService.cancelByClient(id, dto.reason(), principal.getUser());
        return ResponseEntity.ok().build();
    }

    /** RF_BAR_16 (preparatorio) — lista richieste in attesa */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<List<BookingResponseDto>> getPending() {
        return ResponseEntity.ok(bookingService.getPendingRequests());
    }

    /** Storico prenotazioni CLR */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<BookingResponseDto>> getMyBookings(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(bookingService.getClientBookings(principal.getUser()));
    }
}
```

---

## Fase 5.7 — Scheduled Task: transizione a PASSATA

**Obiettivo**: Le prenotazioni ACCETTATE il cui orario è trascorso vengono automaticamente marcate come PASSATE.

```java
@Component
@RequiredArgsConstructor
public class BookingStatusScheduler {

    private final PrenotazioneRepository prenotazioneRepository;

    @Scheduled(cron = "0 0 * * * *")  // ogni ora
    @Transactional
    public void markExpiredBookingsAsPassed() {
        List<Prenotazione> expired = prenotazioneRepository
            .findByStatusAndEndTimeBefore(BookingStatus.ACCETTATA, LocalDateTime.now());

        expired.forEach(b -> {
            b.setStatus(BookingStatus.PASSATA);
            b.setUpdatedAt(LocalDateTime.now());
        });

        prenotazioneRepository.saveAll(expired);
    }
}
```

### Attività
- [ ] Creare `BookingStatusScheduler`
- [ ] Abilitare `@EnableScheduling` nella configurazione Spring
- [ ] Aggiungere query `findByStatusAndEndTimeBefore` al repository

---

## Fase 5.8 — Unit Test (State Machine)

### `BookingStatusTest.java` — Test PURI sull'enum
```java
class BookingStatusTest {

    // --- Transizioni valide ---
    @Test void inAttesa_canTransitionTo_accettata()
    @Test void inAttesa_canTransitionTo_rifiutata()
    @Test void accettata_canTransitionTo_annullata()
    @Test void accettata_canTransitionTo_passata()

    // --- Transizioni non valide ---
    @Test void rifiutata_cannotTransitionTo_accettata()
    @Test void rifiutata_cannotTransitionTo_inAttesa()
    @Test void annullata_cannotTransitionTo_accettata()
    @Test void passata_cannotTransitionTo_annullata()
    @Test void inAttesa_cannotTransitionTo_passata()

    // --- Stati terminali ---
    @Test void rifiutata_isTerminal()
    @Test void annullata_isTerminal()
    @Test void passata_isTerminal()
    @Test void inAttesa_isNotTerminal()
    @Test void accettata_isNotTerminal()

    // --- Eccezioni ---
    @Test void rifiutata_transitionTo_accettata_throwsInvalidBookingTransitionException()
    @Test void passata_transitionTo_annullata_throwsInvalidBookingTransitionException()
    @Test void annullata_transitionTo_inAttesa_throwsInvalidBookingTransitionException()

    // --- canTransitionTo ---
    @Test void canTransitionTo_validTransition_returnsTrue()
    @Test void canTransitionTo_invalidTransition_returnsFalse()
}
```

---

## Fase 5.9 — Unit Test (BookingService & ValidationChain)

### `BookingServiceTest.java`
```java
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock PrenotazioneRepository prenotazioneRepository;
    @Mock PoltronaRepository poltronaRepository;
    @Mock ServizioRepository servizioRepository;
    @Mock FasciaOrariaRepository fasciaOrariaRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock BookingMapper bookingMapper;
    @Mock List<BookingValidator> validators;  // mock della catena
    @InjectMocks BookingService bookingService;

    // --- createRequest ---
    @Test void createRequest_slotAvailable_createsWithStatusInAttesa()
    @Test void createRequest_validationFails_throwsException()
    @Test void createRequest_inactiveChair_throwsResourceNotFoundException()
    @Test void createRequest_publishesBookingRequestCreatedEvent()

    // --- acceptRequest ---
    @Test void acceptRequest_fromInAttesa_statusBecomesAccettata()
    @Test void acceptRequest_fromRifiutata_throwsInvalidTransitionException()
    @Test void acceptRequest_publishesBookingAcceptedEvent()

    // --- rejectRequest ---
    @Test void rejectRequest_fromInAttesa_statusBecomesRifiutata()
    @Test void rejectRequest_fromAccettata_throwsInvalidTransitionException()

    // --- cancelByClient ---
    @Test void cancelByClient_ownBooking_statusBecomesAnnullata()
    @Test void cancelByClient_notOwner_throwsUnauthorizedException()
    @Test void cancelByClient_savesCancellationReason()
    @Test void cancelByClient_publishesCancelledEvent()

    // --- cancelByBarber ---
    @Test void cancelByBarber_accettata_statusBecomesAnnullata()
    @Test void cancelByBarber_inAttesa_statusBecomesAnnullata()

    // --- createDirect ---
    @Test void createDirect_byBarber_statusIsAccettataDirectly()
    @Test void createDirect_doesNotPublishEvent()

    // --- update ---
    @Test void update_newSlotFree_updatesSuccessfully()
    @Test void update_newSlotOccupied_throwsSlotNotAvailableException()
}
```

### `ValidatorTest.java` — Ogni validator isolato
```java
class ChairActiveValidatorTest {
    @Test void inactiveChair_throwsBookingValidationException()
    @Test void activeChair_passesValidation()
}

class SlotWithinScheduleValidatorTest {
    @Test void noSchedule_closedDay_throwsBookingValidationException()
    @Test void slotBeforeOpenTime_throwsException()
    @Test void slotAfterCloseTime_throwsException()
    @Test void slotWithinHours_passesValidation()
}

class SlotNotInBreakValidatorTest {
    @Test void slotDuringBreak_throwsException()
    @Test void slotAdjacentToBreak_passesValidation()
    @Test void noBreaks_passesValidation()
}

class NoOverlapValidatorTest {
    @Test void overlappingBooking_throwsSlotNotAvailableException()
    @Test void adjacentBooking_passesValidation()
    @Test void excludedBookingId_notConsideredOverlap()  // per modifica prenotazione
    @Test void noExistingBookings_passesValidation()
}
```

---

## Fase 5.10 — Integration Test (no-double-booking)

### `BookingIntegrationTest.java`
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class BookingIntegrationTest {

    // --- Flusso base ---
    @Test void createRequest_asClient_returns201()
    @Test void createGuestRequest_noAuth_returns201()
    @Test void acceptRequest_asBarber_statusBecomesAccettata()
    @Test void rejectRequest_asBarber_statusBecomesRifiutata()
    @Test void cancelByClient_ownBooking_statusBecomesAnnullata()
    @Test void cancelByBarber_returns204()
    @Test void createDirect_asBarber_statusIsAccettata()

    // --- Autenticazione/Autorizzazione ---
    @Test void acceptRequest_asClient_returns403()
    @Test void cancelByClient_notOwner_returns403()
    @Test void createDirect_asClient_returns403()

    // --- No double-booking ---
    @Test
    @DisplayName("CRITICO: due prenotazioni sullo stesso slot → solo una accettata")
    void doubleBooking_sameSlotSameChair_secondFails()

    @Test
    @DisplayName("CRITICO: 100 tentativi concorrenti → esatto 1 prenotazione accettata")
    void concurrentBookings_100Attempts_exactly1Accepted() throws InterruptedException {
        // Eseguire 100 thread concorrenti che tentano di prenotare lo stesso slot
        // Solo 1 dovrebbe andare a buon fine (Exclusion Constraint GiST)
        // → Verifica: COUNT(*) WHERE status = 'ACCETTATA' = 1
    }

    // --- State machine ---
    @Test void acceptAlreadyAccepted_returns409()
    @Test void rejectAlreadyRifiutata_returns409()
    @Test void cancelAnnullata_returns409()

    // --- Transizione PASSATA ---
    @Test void scheduledTask_expiredBooking_markedAsPassed()
}
```

---

## Fase 5.11 — PiTest Mutation Testing

**Obiettivo**: Validare la qualità dei test del Service layer con il mutation testing.

### Configurazione PiTest in `pom.xml`
```xml
<plugin>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-maven</artifactId>
    <configuration>
        <targetClasses>
            <param>com.barberbook.service.*</param>
            <param>com.barberbook.domain.enums.*</param>
        </targetClasses>
        <targetTests>
            <param>com.barberbook.service.*Test</param>
            <param>com.barberbook.domain.*Test</param>
        </targetTests>
        <mutationThreshold>70</mutationThreshold>
        <coverageThreshold>80</coverageThreshold>
        <outputFormats><outputFormat>HTML</outputFormat></outputFormats>
    </configuration>
</plugin>
```

### Esecuzione
```bash
cd backend
mvn org.pitest:pitest-maven:mutationCoverage
```

### Target
- Mutation Score ≥ 70% sul Service layer
- Mutation Score ≥ 85% su `BookingStatus` (logica critica)

---

## Fase 5.12 — Verifica Quality Gate

### Checklist finale Sprint 5
- [ ] Tutti i test State Machine passano (18+ test)
- [ ] Tutti i test BookingService passano
- [ ] Tutti i test Validator passano (4 classi di test × ~3 test ciascuna)
- [ ] Test no-double-booking con 100 thread concorrenti supera il gate
- [ ] Exclusion Constraint GiST attivo e funzionante
- [ ] Optimistic Locking (`@Version`) attivo sull'entity
- [ ] PiTest mutation score ≥ 70%
- [ ] JaCoCo: Service layer LINE ≥ 80%, BRANCH ≥ 75%
- [ ] CI pipeline verde
- [ ] Transizione a `PASSATA` schedulata e testata

---

## Definition of Done — Sprint 5

| Criterio | Verifica |
|----------|----------|
| ✅ RF_CLI_6 Richiesta prenotazione | CLR invia richiesta → stato IN_ATTESA |
| ✅ RF_CLG_1 Form ospite | CLG compila nome/cognome/telefono obbligatori |
| ✅ RF_BAR_14 Accettazione | BAR accetta → ACCETTATA, CLR notificato |
| ✅ RF_BAR_15 Rifiuto | BAR rifiuta → RIFIUTATA, CLR notificato |
| ✅ RF_BAR_11 Prenotazione diretta | BAR crea → direttamente ACCETTATA |
| ✅ RF_BAR_12 Modifica | BAR modifica → disponibilità ri-verificata |
| ✅ RF_BAR_13 Cancellazione | BAR cancella → ANNULLATA, CLR notificato |
| ✅ RF_CLR_4 Annullamento cliente | CLR annulla con motivazione → ANNULLATA |
| ✅ No double-booking (applicativo) | ValidationChain blocca prima del DB |
| ✅ No double-booking (DB) | Exclusion Constraint GiST blocca sempre |
| ✅ State machine robusta | Transizioni illegali → eccezione |
| ✅ PiTest ≥ 70% | Mutation testing superato |
| ✅ CI pipeline verde | GitHub Actions green |

---

*Sprint 5 — Ultima modifica: 22/04/2026*
