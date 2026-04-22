# Design Patterns — BarberBook
> Documento di progettazione pattern  
> Autore: Gianfranco Barba | Revisione: 0.1 | Data: 15/04/2026  
> Stato: **IN REVISIONE** — in attesa di approvazione

---

## Premessa Metodologica

Prima di scrivere una riga di codice è fondamentale decidere **dove** e **come** applicare i design pattern. Un pattern applicato nel posto sbagliato è peggio di nessun pattern: introduce complessità gratuita. Il criterio adottato è:

> **Un pattern viene adottato solo se risolve un problema concreto già identificato nel RAD o nei requisiti. Ogni scelta è motivata da un requisito specifico.**

I pattern sono divisi in tre livelli di priorità:
- 🔴 **Critico** — Il sistema non si progetta correttamente senza di esso
- 🟡 **Importante** — Migliora significativamente testabilità e manutenibilità
- 🟢 **Supporto** — Utile per specifiche situazioni, adottato su scala ridotta

---

## Mappa dei Pattern sul Sistema

```
┌─────────────────────────────────────────────────────────────────────┐
│                         BACKEND (Spring Boot)                        │
│                                                                      │
│  Controller Layer                                                    │
│  └── [DTO Pattern] separazione API/dominio                          │
│                                                                      │
│  Service Layer                                                       │
│  ├── [Facade]          BookingService orchestra sub-service          │
│  ├── [State]           Booking state machine (5 stati + transizioni) │
│  ├── [Strategy]        AvailabilityService calcolo slot              │
│  ├── [Observer]        Spring Events → notifiche disaccoppiate       │
│  ├── [Factory Method]  NotificationFactory per tipo/attore           │
│  └── [Chain of Resp.]  ValidationChain prima di creare prenotazione  │
│                                                                      │
│  Domain Layer                                                        │
│  └── [Builder]         Costruzione di oggetti Booking complessi      │
│                                                                      │
│  Repository Layer                                                    │
│  └── [Specification]   Query composte per filtri prenotazioni        │
│                                                                      │
├─────────────────────────────────────────────────────────────────────┤
│                         FRONTEND (React)                             │
│                                                                      │
│  ├── [Container/Presenter]  Separazione data-fetching / rendering    │
│  └── [Custom Hook]          Logica riusabile incapsulata             │
└─────────────────────────────────────────────────────────────────────┘
```

---

## PARTE I — Pattern Architetturali (Base del Sistema)

### P0 — Layered Architecture + DTO Pattern
🔴 **Critico** | Già approvato in `tech_design.md`

**Problema**: evitare che le JPA Entity vengano esposte direttamente via REST API, evitare coupling tra layer.

**Soluzione**: tre layer netti (Controller → Service → Repository) con oggetti DTO dedicati per la comunicazione API.

```
Controller ←→ DTO ←→ Service ←→ Entity ←→ Repository ←→ DB
                    (MapStruct)          (Hibernate)
```

**Mapping DTOs**:

```java
// Esempio: la Entity Booking non viene mai serializzata direttamente
public record BookingResponseDto(
    Long id,
    String chairName,
    String serviceName,
    LocalDateTime startTime,
    LocalDateTime endTime,
    BookingStatus status,
    String customerName
) {}

// MapStruct genera il mapper automaticamente
@Mapper(componentModel = "spring")
public interface BookingMapper {
    BookingResponseDto toDto(Booking booking);
    Booking toEntity(CreateBookingRequestDto dto);
}
```

**Regole fisse**:
- Le Entity JPA non escono mai dal Service layer.
- I Controller non conoscono le Entity — lavorano solo con DTO.
- I DTO di request (`*RequestDto`) e di response (`*ResponseDto`) sono classi separate.

---

## PARTE II — Pattern Critici di Dominio

### P1 — State Pattern (per la State Machine delle Prenotazioni)
🔴 **Critico** | Requisiti: RF_CLR_3, RF_BAR_14, RF_BAR_15, RF_CLR_4, RF_BAR_13

**Problema**: La prenotazione attraversa 5 stati con transizioni ben definite. Senza un pattern esplicito, la gestione degli stati diventa una cascata di `if/switch` sparsi nel codice, impossibile da testare sistematicamente e facile da corrompere con transizioni illegali.

**State machine completa**:

```
               [CLIENTE crea richiesta]
                         │
                         ▼
                  ┌─────────────┐
                  │  IN_ATTESA  │ ←──────────────────────────────────┐
                  └──────┬──────┘                                    │
                         │                                           │
          [BAR: accetta] │ [BAR: rifiuta]                            │
                         │                                           │
            ┌────────────┴──────────────┐                            │
            ▼                           ▼                            │
     ┌─────────────┐           ┌──────────────┐                      │
     │  ACCETTATA  │           │  RIFIUTATA   │ ←── TERMINALE        │
     └──────┬──────┘           └──────────────┘                      │
            │                                                        │
   [CLR o   │ [tempo          ┌─── ILLEGALE: rifiutata→accettata     │
    BAR:    │  scaduto]       └─── ILLEGALE: passata→annullata       │
    annulla] │                 └─── ILLEGALE: in_attesa→annullata    │
            │                                                        │
    ┌───────┴───────┐                                                │
    ▼               ▼                                                │
┌──────────┐  ┌──────────┐                                          │
│ ANNULLATA│  │  PASSATA │ ←── TERMINALE (automatica post-data)     │
│(con motiv│  └──────────┘                                          │
│  azione) │                                                        │
└──────────┘ ←── TERMINALE                                         │
                                                                    │
Nota: BAR può annullare direttamente (RF_BAR_13) ──────────────────┘
```

**Implementazione — Enum con logica di transizione**:

```java
public enum BookingStatus {
    IN_ATTESA,
    ACCETTATA,
    RIFIUTATA,
    ANNULLATA,
    PASSATA;

    // Mappa delle transizioni valide
    private static final Map<BookingStatus, Set<BookingStatus>> VALID_TRANSITIONS = Map.of(
        IN_ATTESA, Set.of(ACCETTATA, RIFIUTATA),
        ACCETTATA, Set.of(ANNULLATA, PASSATA),
        RIFIUTATA, Set.of(),   // stato terminale
        ANNULLATA, Set.of(),   // stato terminale
        PASSATA,   Set.of()    // stato terminale
    );

    public boolean canTransitionTo(BookingStatus next) {
        return VALID_TRANSITIONS.get(this).contains(next);
    }

    public BookingStatus transitionTo(BookingStatus next) {
        if (!canTransitionTo(next)) {
            throw new InvalidBookingTransitionException(
                "Transizione non valida: " + this + " → " + next
            );
        }
        return next;
    }
}
```

**Uso nel Service**:

```java
@Service
public class BookingService {

    @Transactional
    public void acceptBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new BookingNotFoundException(bookingId));

        // La transizione illegale viene bloccata nell'enum stesso
        BookingStatus newStatus = booking.getStatus().transitionTo(ACCETTATA);
        booking.setStatus(newStatus);

        bookingRepository.save(booking);

        // Observer: pubblica evento per le notifiche
        eventPublisher.publishEvent(new BookingAcceptedEvent(this, booking));
    }
}
```

**Perché questa implementazione e non classi State separate (GoF classico)?**
Per BarberBook le transizioni sono semplici (nessuna logica complessa per stato) e l'Enum con `canTransitionTo()` è più idiomatico in Java, testabile, e meno verboso. Il GoF classico con 5 classi concrete sarebbe over-engineering per questo caso.

**Testing**:
```java
@Test void inAttesa_canTransitionTo_accettata() { assertTrue(IN_ATTESA.canTransitionTo(ACCETTATA)); }
@Test void rifiutata_cannotTransitionTo_accettata() { assertFalse(RIFIUTATA.canTransitionTo(ACCETTATA)); }
@Test void passata_isTerminal() { assertTrue(PASSATA.VALID_TRANSITIONS.get(PASSATA).isEmpty()); }
@Test void invalidTransition_throwsException() {
    assertThatThrownBy(() -> RIFIUTATA.transitionTo(ACCETTATA))
        .isInstanceOf(InvalidBookingTransitionException.class);
}
```

---

### P2 — Strategy Pattern (per il Calcolo della Disponibilità)
🔴 **Critico** | Requisiti: RF_CLI_3, RF_CLI_4, RF_BAR_9, RF_BAR_10

**Problema**: Il calcolo degli slot disponibili è la logica più complessa del sistema. Dipende da: orari di apertura, pause per poltrona, durata del servizio scelto, prenotazioni esistenti (`IN_ATTESA` + `ACCETTATA`). È altamente testabile ma richiede isolamento.

**Perché Strategy?** La logica di calcolo potrebbe evolvere (slot a granularità fissa vs variabile, regole di buffer tra appuntamenti, ecc.). Strategy permette di sostituire o aggiungere algoritmi senza toccare il Service che li usa.

**Interfaccia Strategy**:

```java
public interface AvailabilityStrategy {
    /**
     * Calcola gli slot temporali liberi per una poltrona in un dato giorno.
     *
     * @param chair           la poltrona per cui calcolare
     * @param date            il giorno richiesto
     * @param serviceDuration la durata del servizio selezionato
     * @param context         contesto: orari, pause, prenotazioni esistenti
     * @return lista ordinata di slot disponibili
     */
    List<TimeSlot> calculateAvailableSlots(
        Chair chair,
        LocalDate date,
        Duration serviceDuration,
        AvailabilityContext context
    );
}

// Context object — raggruppa tutti i dati necessari all'algoritmo
public record AvailabilityContext(
    Schedule schedule,          // orari di apertura per quel giorno
    List<Break> breaks,         // pause di quella poltrona
    List<Booking> existingBookings  // prenotazioni IN_ATTESA + ACCETTATA
) {}
```

**Implementazione Standard**:

```java
@Component("standardAvailability")
public class StandardAvailabilityStrategy implements AvailabilityStrategy {

    @Override
    public List<TimeSlot> calculateAvailableSlots(
            Chair chair, LocalDate date,
            Duration serviceDuration, AvailabilityContext context) {

        // 1. Genera tutti gli slot del giorno (es. 9:00, 9:30, 10:00... ogni 15 min)
        List<TimeSlot> allSlots = generateAllSlots(context.schedule(), serviceDuration);

        // 2. Rimuovi gli slot nelle pause
        List<TimeSlot> notInBreaks = filterBreaks(allSlots, context.breaks());

        // 3. Rimuovi gli slot che si sovrappongono con prenotazioni esistenti
        return filterBookedSlots(notInBreaks, context.existingBookings(), serviceDuration);
    }

    private List<TimeSlot> generateAllSlots(Schedule schedule, Duration duration) {
        // Divide l'orario di apertura in slot della durata del servizio
        List<TimeSlot> slots = new ArrayList<>();
        LocalTime current = schedule.getOpenTime();
        while (current.plus(duration).compareTo(schedule.getCloseTime()) <= 0) {
            slots.add(new TimeSlot(current, current.plus(duration)));
            current = current.plusMinutes(15); // granularità 15 minuti
        }
        return slots;
    }
    // ...metodi privati filterBreaks(), filterBookedSlots()
}
```

**Uso in AvailabilityService**:

```java
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityStrategy availabilityStrategy; // iniettata da Spring

    public Map<Chair, List<TimeSlot>> getAvailableSlots(
            List<Chair> chairs, LocalDate date, Long serviceId) {

        Service service = serviceRepository.findById(serviceId)...;
        Duration duration = Duration.ofMinutes(service.getDurationMinutes());

        return chairs.stream().collect(Collectors.toMap(
            chair -> chair,
            chair -> {
                AvailabilityContext ctx = buildContext(chair, date);
                return availabilityStrategy.calculateAvailableSlots(chair, date, duration, ctx);
            }
        ));
    }
}
```

**TimeSlot — Value Object**:

```java
public record TimeSlot(LocalTime start, LocalTime end) {
    public boolean overlapsWith(TimeSlot other) {
        return this.start.isBefore(other.end) && other.start.isBefore(this.end);
    }
    public boolean overlapsWith(LocalTime otherStart, LocalTime otherEnd) {
        return overlapsWith(new TimeSlot(otherStart, otherEnd));
    }
}
```

**Testing (molto semplice grazie a Strategy)**:

```java
@ExtendWith(MockitoExtension.class)
class StandardAvailabilityStrategyTest {
    // Nessun mock necessario — solo dati di input e verifica dell'output
    private final StandardAvailabilityStrategy strategy = new StandardAvailabilityStrategy();

    @Test
    void noBreaks_noBookings_returnsAllSlots() {
        Schedule nineToOne = new Schedule(LocalTime.of(9,0), LocalTime.of(13,0));
        AvailabilityContext ctx = new AvailabilityContext(nineToOne, List.of(), List.of());
        List<TimeSlot> slots = strategy.calculateAvailableSlots(chair, date, Duration.ofMinutes(30), ctx);
        // 9:00→12:30 con step 15min = 13 slot
        assertThat(slots).hasSize(13);
    }

    @Test
    void existingBooking_blocksOverlappingSlots() { ... }

    @Test
    void breakPeriod_isExcludedFromSlots() { ... }
}
```

---

### P3 — Observer Pattern / Event-Driven (per le Notifiche)
🔴 **Critico** | Requisiti: RF_GEN_5, RF_BAR_16, RF_CLR_7

**Problema**: Quando cambia lo stato di una prenotazione, devono succedere cose in altri moduli (notifiche, SSE push). Se `BookingService` chiama direttamente `NotificationService`, si crea un coupling stretto che rende i test difficili e il codice rigido.

**Soluzione**: Spring `ApplicationEvent` — il booking service pubblica un evento di dominio, il notification service lo ascolta. Il publisher non sa chi ascolta.

**Disaccoppiamento ottenuto**:

```
BookingService ──── publishEvent(BookingAcceptedEvent) ────▶ Spring EventBus
                                                                    │
                                              ┌─────────────────────┤
                                              ▼                     ▼
                                  NotificationService      AuditLogService (futuro)
                                  @EventListener            @EventListener
```

**Definizione degli eventi di dominio**:

```java
// Evento base con dati comuni
public abstract class BookingEvent extends ApplicationEvent {
    private final Booking booking;
    protected BookingEvent(Object source, Booking booking) {
        super(source);
        this.booking = booking;
    }
    public Booking getBooking() { return booking; }
}

// Evento specifico per ogni transizione rilevante
public class BookingRequestCreatedEvent extends BookingEvent { ... }  // CLG/CLR invia richiesta
public class BookingAcceptedEvent extends BookingEvent { ... }        // BAR accetta
public class BookingRejectedEvent extends BookingEvent { ... }        // BAR rifiuta
public class BookingCancelledByClientEvent extends BookingEvent {     // CLR annulla
    private final String cancellationReason;
    ...
}
public class BookingCancelledByBarberEvent extends BookingEvent { ... } // BAR annulla
```

**Publisher — nel BookingService**:

```java
@Service
@RequiredArgsConstructor
public class BookingService {
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void acceptBooking(Long bookingId) {
        Booking booking = ...;
        booking.setStatus(booking.getStatus().transitionTo(ACCETTATA));
        bookingRepository.save(booking);

        eventPublisher.publishEvent(new BookingAcceptedEvent(this, booking)); // 1 riga
    }
}
```

**Subscriber — NotificationService**:

```java
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseEmitterRegistry sseRegistry; // gestione connessioni SSE attive

    @EventListener
    @Async  // non blocca il thread del BookingService
    public void onBookingAccepted(BookingAcceptedEvent event) {
        Booking booking = event.getBooking();

        // Notifica al cliente registrato (se esiste)
        if (booking.getClient() != null) {
            Notification notification = NotificationFactory.createAcceptedNotification(booking);
            notificationRepository.save(notification);
            sseRegistry.pushToUser(booking.getClient().getId(), notification);
        }
        // Per CLG: nessuna notifica in-app (Tony chiama per telefono)
    }

    @EventListener
    @Async
    public void onBookingRequestCreated(BookingRequestCreatedEvent event) {
        // Notifica al BAR: c'è una nuova richiesta
        Notification notification = NotificationFactory.createNewRequestNotification(event.getBooking());
        notificationRepository.save(notification);
        sseRegistry.pushToBarber(notification);
    }

    @EventListener
    @Async
    public void onBookingCancelledByClient(BookingCancelledByClientEvent event) {
        // Notifica al BAR: il cliente ha annullato con motivazione
        Notification n = NotificationFactory.createClientCancellationNotification(
            event.getBooking(), event.getCancellationReason()
        );
        notificationRepository.save(n);
        sseRegistry.pushToBarber(n);
    }
}
```

**SseEmitterRegistry** (componente dedicato alla gestione connessioni SSE):

```java
@Component
public class SseEmitterRegistry {
    // Map: userId → SseEmitter attivo
    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter registerUser(Long userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(userId, emitter);
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        return emitter;
    }

    public void pushToUser(Long userId, Notification notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(notification));
            } catch (IOException e) {
                emitters.remove(userId);
            }
        }
    }

    public void pushToBarber(Notification notification) {
        // Il BAR ha un ruolo speciale — notifica tutti gli emitter con ruolo BARBER
        emitters.entrySet().stream()
            .filter(e -> isBarber(e.getKey()))
            .forEach(e -> push(e.getValue(), notification));
    }
}
```

---

## PARTE III — Pattern di Supporto

### P4 — Factory Method (per le Notifiche)
🟡 **Importante** | Requisiti: RF_GEN_5, RF_BAR_16, RF_CLR_7

**Problema**: Esistono molti tipi di notifiche (nuova richiesta per BAR, conferma per CLR, rifiuto per CLR, annullamento da cliente per BAR, annullamento da BAR per CLR). Creare una `Notification` inline ogni volta con tutti i suoi campi è fragile e non centralizzato.

**Soluzione**: `NotificationFactory` — un posto solo dove le notifiche vengono costruite in modo consistente.

```java
public final class NotificationFactory {

    private NotificationFactory() {} // utility class, non istanziabile

    public static Notification createNewRequestNotification(Booking booking) {
        return Notification.builder()
            .recipientId(BARBER_ID)      // il BAR riceve sempre
            .type(NotificationType.NUOVA_RICHIESTA)
            .title("Nuova richiesta di prenotazione")
            .message(String.format(
                "%s ha richiesto '%s' per il %s alle %s sulla %s",
                booking.getCustomerDisplayName(),
                booking.getService().getName(),
                booking.getStartTime().toLocalDate(),
                booking.getStartTime().toLocalTime(),
                booking.getChair().getName()
            ))
            .bookingId(booking.getId())
            .read(false)
            .createdAt(LocalDateTime.now())
            .build();
    }

    public static Notification createAcceptedNotification(Booking booking) {
        return Notification.builder()
            .recipientId(booking.getClient().getId())
            .type(NotificationType.PRENOTAZIONE_ACCETTATA)
            .title("Prenotazione confermata!")
            .message(String.format(
                "La tua prenotazione per '%s' il %s alle %s è stata confermata.",
                booking.getService().getName(),
                booking.getStartTime().toLocalDate(),
                booking.getStartTime().toLocalTime()
            ))
            .bookingId(booking.getId())
            .read(false)
            .createdAt(LocalDateTime.now())
            .build();
    }

    public static Notification createRejectedNotification(Booking booking) { ... }
    public static Notification createClientCancellationNotification(Booking booking, String reason) { ... }
    public static Notification createBarberCancellationNotification(Booking booking) { ... }
}
```

**Enum NotificationType**:

```java
public enum NotificationType {
    NUOVA_RICHIESTA,          // → BAR
    PRENOTAZIONE_ACCETTATA,   // → CLR
    PRENOTAZIONE_RIFIUTATA,   // → CLR
    ANNULLAMENTO_DA_CLIENTE,  // → BAR
    ANNULLAMENTO_DA_BARBIERE  // → CLR
}
```

---

### P5 — Builder Pattern (per la Creazione di Prenotazioni)
🟡 **Importante** | Requisiti: RF_BAR_11, RF_CLI_6

**Problema**: Una `Booking` ha molti campi, alcuni obbligatori, altri condizionali (es. `clientData` per CLG, `client` per CLR, `cancellationReason` per ANNULLATA). Un costruttore con 8+ parametri è illeggibile e bug-prone.

**Soluzione**: Lombok `@Builder` — genera il builder automaticamente.

```java
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(nullable = false)
    private Chair chair;

    @ManyToOne @JoinColumn(nullable = false)
    private Service service;

    @ManyToOne  // null per CLG
    private User client;

    @Embedded   // valorizzato per CLG, null per CLR
    private GuestData guestData;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.IN_ATTESA;

    private String cancellationReason; // solo quando ANNULLATA

    @Version
    private Long version; // per optimistic locking (PostgreSQL + JPA)

    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**Uso nei Service**:

```java
// Prenotazione da CLR (client registrato)
Booking booking = Booking.builder()
    .chair(chair)
    .service(service)
    .client(currentUser)
    .startTime(requestedSlot.start().atDate(date))
    .endTime(requestedSlot.end().atDate(date))
    .createdAt(LocalDateTime.now())
    .build(); // status = IN_ATTESA di default

// Prenotazione diretta da BAR (RF_BAR_11) — già accettata
Booking directBooking = Booking.builder()
    .chair(chair)
    .service(service)
    .guestData(new GuestData(name, surname, phone)) // o client se è un CLR
    .startTime(...)
    .endTime(...)
    .status(BookingStatus.ACCETTATA) // direttamente confermata
    .createdAt(LocalDateTime.now())
    .build();
```

---

### P6 — Facade Pattern (BookingService come Orchestratore)
🟡 **Importante** | Requisiti: RF_CLI_6, RF_BAR_11, RF_BAR_14, RF_BAR_15

**Problema**: Il Controller non deve conoscere la sequenza di operazioni necessaria per gestire una prenotazione (verifica disponibilità, applicazione dello state pattern, salvataggio, pubblicazione evento). Se il Controller coordina tutto, diventa un God Object.

**Soluzione**: `BookingService` funge da Facade — offre operazioni di alto livello al Controller, nascondendo la collaborazione tra `AvailabilityService`, `BookingRepository`, `ApplicationEventPublisher`.

```java
@Service
@RequiredArgsConstructor
public class BookingService {

    // collaboratori (nascosti al Controller)
    private final BookingRepository bookingRepository;
    private final AvailabilityService availabilityService;
    private final ApplicationEventPublisher eventPublisher;
    private final BookingMapper bookingMapper;

    // API pubblica (Facade)

    /** Cliente invia richiesta di prenotazione */
    public BookingResponseDto createRequest(CreateBookingRequestDto dto, User requester) {
        // 1. Verifica disponibilità (Strategy internamente)
        availabilityService.assertSlotIsAvailable(dto.chairId(), dto.startTime(), dto.endTime());
        // 2. Costruisce (Builder)
        Booking booking = bookingMapper.toEntity(dto);
        booking.setClient(requester);
        // 3. Salva
        Booking saved = bookingRepository.save(booking);
        // 4. Pubblica evento (Observer)
        eventPublisher.publishEvent(new BookingRequestCreatedEvent(this, saved));
        // 5. Ritorna DTO
        return bookingMapper.toDto(saved);
    }

    /** BAR accetta una richiesta */
    public void acceptRequest(Long bookingId) {
        Booking booking = findOrThrow(bookingId);
        booking.setStatus(booking.getStatus().transitionTo(ACCETTATA)); // State
        bookingRepository.save(booking);
        eventPublisher.publishEvent(new BookingAcceptedEvent(this, booking)); // Observer
    }

    /** BAR rifiuta una richiesta */
    public void rejectRequest(Long bookingId) { ... }

    /** CLR annulla una prenotazione con motivazione */
    public void cancelByClient(Long bookingId, String reason, User requester) {
        Booking booking = findOrThrow(bookingId);
        assertOwner(booking, requester);
        booking.setStatus(booking.getStatus().transitionTo(ANNULLATA)); // State
        booking.setCancellationReason(reason);
        bookingRepository.save(booking);
        eventPublisher.publishEvent(new BookingCancelledByClientEvent(this, booking, reason));
    }
}
```

Il `BookingController` risultante è minimalista:

```java
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService; // conosce solo la Facade

    @PostMapping
    public ResponseEntity<BookingResponseDto> createRequest(
            @Valid @RequestBody CreateBookingRequestDto dto,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.status(201).body(bookingService.createRequest(dto, user.getUser()));
    }

    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<Void> accept(@PathVariable Long id) {
        bookingService.acceptRequest(id);
        return ResponseEntity.ok().build();
    }
}
```

---

### P7 — Chain of Responsibility (ValidationChain)
🟡 **Importante** | Requisiti: RNF_R_1, RF_CLI_6, RF_BAR_11

**Problema**: Prima di creare/accettare una prenotazione, devono passare vari controlli in sequenza:
1. Slot nel range degli orari di apertura?
2. Slot non in pausa per quella poltrona?
3. Nessuna prenotazione esistente (`IN_ATTESA`/`ACCETTATA`) si sovrappone?
4. La poltrona è attiva?
5. Il servizio esiste ed è attivo?

Mettere tutti questi `if` in `BookingService` lo appesantisce. La Chain permette di aggiungere/rimuovere controlli senza modificare il Service.

```java
public interface BookingValidator {
    void validate(BookingValidationRequest request) throws BookingValidationException;
}

// Ogni validator è un anello della catena
@Component
@Order(1)
public class ChairActiveValidator implements BookingValidator {
    public void validate(BookingValidationRequest req) {
        if (!req.chair().isActive()) {
            throw new BookingValidationException("La poltrona selezionata non è disponibile.");
        }
    }
}

@Component
@Order(2)
public class SlotWithinScheduleValidator implements BookingValidator {
    public void validate(BookingValidationRequest req) {
        Schedule schedule = req.schedule();
        if (req.startTime().toLocalTime().isBefore(schedule.getOpenTime()) ||
            req.endTime().toLocalTime().isAfter(schedule.getCloseTime())) {
            throw new BookingValidationException("Lo slot richiesto è al di fuori degli orari di apertura.");
        }
    }
}

@Component
@Order(3)
public class SlotNotInBreakValidator implements BookingValidator { ... }

@Component
@Order(4)
public class NoOverlapValidator implements BookingValidator {
    // Verifica a livello applicativo (il DB fa la verifica finale con Exclusion Constraint)
    public void validate(BookingValidationRequest req) {
        boolean hasOverlap = req.existingBookings().stream()
            .anyMatch(b -> b.overlaps(req.startTime(), req.endTime()));
        if (hasOverlap) {
            throw new SlotNotAvailableException("Lo slot selezionato non è più disponibile.");
        }
    }
}

// Il Service esegue la catena
@Service
@RequiredArgsConstructor
public class BookingService {
    private final List<BookingValidator> validators; // Spring inietta tutti i componenti

    private void runValidationChain(BookingValidationRequest request) {
        validators.forEach(v -> v.validate(request)); // ordine garantito da @Order
    }
}
```

**Vantaggio per il testing**: ogni validator è testabile in isolamento con un semplice unit test senza mock del DB.

---

### P8 — Specification Pattern (per Query Complesse)
🟡 **Importante** | Requisiti: RF_CLR_2, RF_CLR_3, RF_BAR_1, RF_BAR_2

**Problema**: Le prenotazioni devono essere filtrate per: stato, data, poltrona, cliente. Combinare questi filtri nel Repository con metodi nomi fissi (`findByClientAndStatusAndDateBetween`) esplode esponenzialmente.

**Soluzione**: JPA Specification (JPA Criteria API) — query componibili a runtime.

```java
public class BookingSpecifications {

    public static Specification<Booking> byClient(Long clientId) {
        return (root, query, cb) ->
            cb.equal(root.get("client").get("id"), clientId);
    }

    public static Specification<Booking> byStatus(BookingStatus status) {
        return (root, query, cb) ->
            cb.equal(root.get("status"), status);
    }

    public static Specification<Booking> byDate(LocalDate date) {
        return (root, query, cb) ->
            cb.between(root.get("startTime"),
                date.atStartOfDay(), date.atTime(LocalTime.MAX));
    }

    public static Specification<Booking> byChair(Long chairId) {
        return (root, query, cb) ->
            cb.equal(root.get("chair").get("id"), chairId);
    }

    public static Specification<Booking> upcoming() {
        return (root, query, cb) ->
            cb.greaterThan(root.get("startTime"), LocalDateTime.now());
    }
}

// Repository abilitato alle Specification
public interface BookingRepository extends JpaRepository<Booking, Long>,
        JpaSpecificationExecutor<Booking> {}

// Uso nel Service
public List<Booking> getClientBookings(Long clientId, BookingStatus status) {
    Specification<Booking> spec = Specification.where(byClient(clientId));
    if (status != null) {
        spec = spec.and(byStatus(status)); // RF_CLR_3: filtro per stato
    }
    return bookingRepository.findAll(spec);
}

// Dashboard giornaliera BAR (RF_BAR_2)
public List<Booking> getDailyDashboard(LocalDate date, Long chairId) {
    return bookingRepository.findAll(
        byDate(date).and(byChair(chairId))  // composizione fluente
    );
}
```

---

## PARTE IV — Pattern Frontend (React)

### P9 — Custom Hook Pattern
🔴 **Critico** (nel contesto Frontend) | Trasversale a tutti i RF

**Problema**: La logica di data-fetching, stato di loading, error handling e invalidazione della cache è ripetitiva e non deve stare nei componenti UI.

**Soluzione**: Custom Hooks che incapsulano `TanStack Query` + `Axios`.

```typescript
// hooks/useAvailableSlots.ts
export function useAvailableSlots(date: string | null, serviceId: number | null) {
    return useQuery({
        queryKey: ['slots', date, serviceId],
        queryFn: () => api.get<TimeSlot[]>(`/availability?date=${date}&serviceId=${serviceId}`),
        enabled: !!date && !!serviceId,   // esegui solo se entrambi i parametri esistono
        staleTime: 30_000,                // dati validi 30 secondi
    });
}

// hooks/useCreateBooking.ts
export function useCreateBooking() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (data: CreateBookingRequest) => api.post('/bookings', data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['slots'] });    // ricarica disponibilità
            queryClient.invalidateQueries({ queryKey: ['bookings'] }); // ricarica storico
            toast.success('Richiesta inviata! Attendi la conferma del barbiere.');
        },
        onError: (error) => {
            toast.error(error.response?.data?.message ?? 'Errore durante la prenotazione.');
        }
    });
}

// hooks/useNotifications.ts — gestione SSE
export function useNotifications(userId: number) {
    const queryClient = useQueryClient();

    useEffect(() => {
        const eventSource = new EventSource(`/api/notifications/stream?userId=${userId}`);
        eventSource.addEventListener('notification', (e) => {
            const notification = JSON.parse(e.data);
            queryClient.setQueryData(['notifications'], (old: Notification[]) =>
                [notification, ...(old ?? [])]
            );
        });
        return () => eventSource.close(); // cleanup alla dismissione del componente
    }, [userId, queryClient]);
}
```

---

### P10 — Container / Presenter Pattern
🟡 **Importante** (nel contesto Frontend)

**Problema**: I componenti che fanno data-fetching E rendering diventano enormi, difficili da testare e non riusabili.

**Soluzione**: separare il componente "intelligente" (Container, fa il fetch) da quello "stupido" (Presenter, solo rendering).

```typescript
// PRESENTER — solo UI, nessuna dipendenza da API o hook di fetch
// Facilmente testabile con React Testing Library
export function BookingListPresenter({ bookings, onCancel, isLoading }: BookingListProps) {
    if (isLoading) return <LoadingSpinner />;
    if (bookings.length === 0) return <EmptyState message="Nessuna prenotazione trovata." />;
    return (
        <ul>
            {bookings.map(b => (
                <BookingCard key={b.id} booking={b} onCancel={onCancel} />
            ))}
        </ul>
    );
}

// CONTAINER — orchestrazione, conosce i custom hook
export function MyBookingsContainer() {
    const { data: bookings, isLoading } = useMyBookings();
    const { mutate: cancelBooking } = useCancelBooking();

    const handleCancel = (id: number, reason: string) => {
        cancelBooking({ bookingId: id, reason });
    };

    return (
        <BookingListPresenter
            bookings={bookings ?? []}
            isLoading={isLoading}
            onCancel={handleCancel}
        />
    );
}
```

---

## Riepilogo: Pattern → Requisito → Dove

| # | Pattern | Livello | Dove | Requisiti coperti |
|---|---------|---------|------|-------------------|
| P0 | Layered Architecture + DTO | Architetturale | Tutti i layer | Tutti i RF |
| P1 | **State** | 🔴 Critico | `BookingStatus` enum | RF_BAR_14, 15, 13, RF_CLR_4, RF_CLR_3 |
| P2 | **Strategy** | 🔴 Critico | `AvailabilityService` | RF_CLI_3, 4, RF_BAR_9, 10 |
| P3 | **Observer** (Spring Events) | 🔴 Critico | `BookingService` → `NotificationService` | RF_GEN_5, RF_BAR_16, RF_CLR_7 |
| P4 | Factory Method | 🟡 Importante | `NotificationFactory` | RF_GEN_5, RF_BAR_16, RF_CLR_7 |
| P5 | Builder (Lombok) | 🟡 Importante | Entity `Booking` | RF_BAR_11, RF_CLI_6, RF_CLG_1 |
| P6 | **Facade** | 🟡 Importante | `BookingService` | RF_CLI_6, RF_BAR_11/12/13/14/15 |
| P7 | Chain of Responsibility | 🟡 Importante | `BookingValidator` chain | RNF_R_1, RF_CLI_6, RF_BAR_11 |
| P8 | Specification (JPA) | 🟡 Importante | `BookingSpecifications` | RF_CLR_2, 3, RF_BAR_1, 2 |
| P9 | Custom Hook | 🔴 Critico (FE) | `hooks/*.ts` | Tutti i RF frontend |
| P10 | Container / Presenter | 🟡 Importante (FE) | Componenti React | Tutti i RF frontend |

---

## Diagramma delle Dipendenze tra Pattern

```
                    REST Request
                         │
                         ▼
              ┌─────────────────────┐
              │  BookingController  │ ← DTO Pattern (input)
              └──────────┬──────────┘
                         │ delega tutto a
                         ▼
              ┌─────────────────────┐
              │   BookingService    │ ← FACADE
              └─────┬───────┬───────┘
                    │       │
          ┌─────────┘       └──────────────┐
          ▼                                ▼
 ┌──────────────────┐         ┌────────────────────────┐
 │ ValidationChain  │         │  BookingStatus.transitionTo │
 │ (Chain of Resp.) │         │  (STATE Pattern)        │
 └──────────────────┘         └────────────┬───────────┘
          │                                │ se valida
          ▼                                ▼
 ┌──────────────────┐         ┌────────────────────────┐
 │ AvailabilityServ │         │  eventPublisher.publish │
 │ (STRATEGY)       │         │  (OBSERVER trigger)    │
 └──────────────────┘         └────────────┬───────────┘
                                           │
                              ┌────────────┴──────────────┐
                              ▼                           ▼
                   ┌──────────────────┐        ┌─────────────────┐
                   │NotificationService│        │  AuditService   │
                   │  @EventListener  │        │  (futuro)       │
                   └──────────┬───────┘        └─────────────────┘
                              │
                   ┌──────────┴───────────┐
                   ▼                      ▼
           ┌──────────────┐    ┌──────────────────┐
           │Notification  │    │  SseEmitterRegistry│
           │Factory       │    │  (push SSE)       │
           └──────────────┘    └──────────────────┘
```

---

## Anti-Pattern da Evitare

| Anti-pattern | Rischio | Prevenzione adottata |
|-------------|---------|----------------------|
| **God Service** | Un Service che fa tutto | Facade divide ruoli chiari |
| **Anemic Domain Model** | Entity senza logica | `BookingStatus` con logica di transizione |
| **Magic Numbers** | `status == 2` invece di enum | Enum `BookingStatus` sempre |
| **Esposizione Entity via API** | Coupling UI/DB | DTO rigorosi + MapStruct |
| **Test accoppiati al DB** | Test lenti e fragili | Strategy pura + Testcontainers solo dove serve |
| **Notifiche hardcoded nel Service** | Coupling service→service | Observer con Spring Events |
| **Repository con metodi nome-esplosione** | `findByClientAndStatusAndDate...` | Specification composibili |

---

*Documento generato: 15/04/2026 — In attesa di approvazione per procedere alla Fase 0*
