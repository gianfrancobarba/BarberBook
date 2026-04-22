# Sprint 6 — Notifiche In-App (SSE)
> **Stato**: ⬜ Non iniziato  
> **Dipende da**: Sprint 5 ✅ (domain events pubblicati da BookingService)  
> **Obiettivo**: Il sistema reagisce agli eventi di business e avvisa proattivamente BAR e CLR in real-time tramite Server-Sent Events, senza polling.

---

## Requisiti Funzionali Coperti

| RF | Nome | Attore | Priorità |
|----|------|--------|----------|
| RF_GEN_5 | Notifiche In-App in tempo reale | BAR, CLR | Alta |
| RF_BAR_16 | Area Notifiche BAR | BAR | Alta |
| RF_CLR_7 | Area Notifiche CLR | CLR | Alta |

---

## Indice Fasi

1. [Fase 6.1 — Modello di Dominio Notifiche](#fase-61--modello-di-dominio-notifiche)
2. [Fase 6.2 — Migrazione Flyway](#fase-62--migrazione-flyway)
3. [Fase 6.3 — NotificationFactory (Factory Method)](#fase-63--notificationfactory-factory-method)
4. [Fase 6.4 — SseEmitterRegistry](#fase-64--sseemitterregistry)
5. [Fase 6.5 — NotificationService (Observer)](#fase-65--notificationservice-observer)
6. [Fase 6.6 — REST Controller SSE](#fase-66--rest-controller-sse)
7. [Fase 6.7 — Unit Test](#fase-67--unit-test)
8. [Fase 6.8 — Integration Test](#fase-68--integration-test)
9. [Fase 6.9 — Verifica Quality Gate](#fase-69--verifica-quality-gate)

---

## Fase 6.1 — Modello di Dominio Notifiche

**Obiettivo**: Definire l'entità `Notifica` e l'enum `NotificationType` con tutti i tipi di evento supportati.

### Enum — `NotificationType.java`
```java
public enum NotificationType {
    NUOVA_RICHIESTA,           // → BAR: cliente ha inviato richiesta
    PRENOTAZIONE_ACCETTATA,    // → CLR: BAR ha accettato
    PRENOTAZIONE_RIFIUTATA,    // → CLR: BAR ha rifiutato
    ANNULLAMENTO_DA_CLIENTE,   // → BAR: CLR ha annullato con motivazione
    ANNULLAMENTO_DA_BARBIERE   // → CLR: BAR ha cancellato la prenotazione
}
```

### Entità JPA — `Notifica.java`
```java
@Entity
@Table(name = "notifications",
       indexes = {
           @Index(name = "idx_notification_recipient", columnList = "recipient_id"),
           @Index(name = "idx_notification_read", columnList = "letta"),
           @Index(name = "idx_notification_created", columnList = "created_at")
       })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Notifica {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User destinatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType tipo;

    @Column(nullable = false, length = 200)
    private String titolo;

    @Column(nullable = false, length = 1000)
    private String messaggio;

    @Column(nullable = false)
    @Builder.Default
    private boolean letta = false;

    @Column(name = "booking_id")
    private Long bookingId;          // riferimento alla prenotazione correlata

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
```

### DTO

```java
// Response: notifica per il client (frontend)
public record NotificationResponseDto(
    Long id,
    NotificationType tipo,
    String titolo,
    String messaggio,
    boolean letta,
    Long bookingId,
    LocalDateTime createdAt
) {}

// DTO minimo per push SSE (non necessita di tutti i campi)
public record NotificationPushDto(
    Long id,
    NotificationType tipo,
    String titolo,
    String messaggio,
    Long bookingId,
    LocalDateTime createdAt
) {}
```

### Attività
- [ ] Creare enum `NotificationType`
- [ ] Creare entità `Notifica.java`
- [ ] Creare `NotificationResponseDto`, `NotificationPushDto`
- [ ] Creare `NotificaRepository extends JpaRepository<Notifica, Long>`

---

## Fase 6.2 — Migrazione Flyway

### `V12__notifications_schema.sql`
```sql
CREATE TABLE notifications (
    id            BIGSERIAL PRIMARY KEY,
    recipient_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tipo          VARCHAR(30) NOT NULL,
    titolo        VARCHAR(200) NOT NULL,
    messaggio     VARCHAR(1000) NOT NULL,
    letta         BOOLEAN NOT NULL DEFAULT FALSE,
    booking_id    BIGINT REFERENCES bookings(id) ON DELETE SET NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_notification_tipo CHECK (tipo IN (
        'NUOVA_RICHIESTA',
        'PRENOTAZIONE_ACCETTATA',
        'PRENOTAZIONE_RIFIUTATA',
        'ANNULLAMENTO_DA_CLIENTE',
        'ANNULLAMENTO_DA_BARBIERE'
    ))
);

CREATE INDEX idx_notification_recipient ON notifications(recipient_id);
CREATE INDEX idx_notification_read      ON notifications(letta);
CREATE INDEX idx_notification_created   ON notifications(created_at DESC);
```

### Attività
- [ ] Creare `V12__notifications_schema.sql`
- [ ] Verificare migrazione applicata correttamente
- [ ] Verificare CHECK constraint su `tipo`

---

## Fase 6.3 — NotificationFactory (Factory Method)

**Obiettivo**: Centralizzare la costruzione delle notifiche. Un solo punto nel codice dove vengono definiti titoli e messaggi per ogni tipo di evento.

```java
/**
 * Factory Method Pattern: ogni metodo statico crea una Notifica per uno specifico evento.
 * Classe non istanziabile — solo metodi factory statici.
 */
public final class NotificationFactory {

    private NotificationFactory() {}

    /** BAR: nuova richiesta di prenotazione ricevuta */
    public static Notifica createNewRequestNotification(Prenotazione booking, User barber) {
        return Notifica.builder()
            .destinatario(barber)
            .tipo(NotificationType.NUOVA_RICHIESTA)
            .titolo("Nuova richiesta di prenotazione")
            .messaggio(String.format(
                "%s ha richiesto '%s' per il %s alle %s sulla %s.",
                booking.getCustomerDisplayName(),
                booking.getServizio().getNome(),
                formatDate(booking.getStartTime()),
                formatTime(booking.getStartTime()),
                booking.getPoltrona().getNome()
            ))
            .bookingId(booking.getId())
            .build();
    }

    /** CLR: la propria prenotazione è stata accettata */
    public static Notifica createAcceptedNotification(Prenotazione booking) {
        return Notifica.builder()
            .destinatario(booking.getClient())
            .tipo(NotificationType.PRENOTAZIONE_ACCETTATA)
            .titolo("Prenotazione confermata!")
            .messaggio(String.format(
                "La tua prenotazione per '%s' il %s alle %s è stata confermata.",
                booking.getServizio().getNome(),
                formatDate(booking.getStartTime()),
                formatTime(booking.getStartTime())
            ))
            .bookingId(booking.getId())
            .build();
    }

    /** CLR: la propria prenotazione è stata rifiutata */
    public static Notifica createRejectedNotification(Prenotazione booking) {
        return Notifica.builder()
            .destinatario(booking.getClient())
            .tipo(NotificationType.PRENOTAZIONE_RIFIUTATA)
            .titolo("Prenotazione non confermata")
            .messaggio(String.format(
                "Purtroppo la tua richiesta per '%s' il %s alle %s non è stata confermata. " +
                "Puoi richiedere un altro orario.",
                booking.getServizio().getNome(),
                formatDate(booking.getStartTime()),
                formatTime(booking.getStartTime())
            ))
            .bookingId(booking.getId())
            .build();
    }

    /** BAR: un CLR ha annullato la propria prenotazione */
    public static Notifica createClientCancellationNotification(Prenotazione booking,
                                                                  String reason,
                                                                  User barber) {
        return Notifica.builder()
            .destinatario(barber)
            .tipo(NotificationType.ANNULLAMENTO_DA_CLIENTE)
            .titolo("Prenotazione annullata dal cliente")
            .messaggio(String.format(
                "%s ha annullato la prenotazione del %s alle %s. Motivo: \"%s\"",
                booking.getCustomerDisplayName(),
                formatDate(booking.getStartTime()),
                formatTime(booking.getStartTime()),
                reason
            ))
            .bookingId(booking.getId())
            .build();
    }

    /** CLR: il BAR ha cancellato una prenotazione confermata */
    public static Notifica createBarberCancellationNotification(Prenotazione booking) {
        return Notifica.builder()
            .destinatario(booking.getClient())
            .tipo(NotificationType.ANNULLAMENTO_DA_BARBIERE)
            .titolo("Prenotazione cancellata dal salone")
            .messaggio(String.format(
                "La tua prenotazione per '%s' del %s alle %s è stata cancellata dal salone. " +
                "Ci scusiamo per il disagio.",
                booking.getServizio().getNome(),
                formatDate(booking.getStartTime()),
                formatTime(booking.getStartTime())
            ))
            .bookingId(booking.getId())
            .build();
    }

    // --- Utility ---
    private static String formatDate(LocalDateTime dt) {
        return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static String formatTime(LocalDateTime dt) {
        return dt.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
```

### Attività
- [ ] Creare `NotificationFactory` con tutti i metodi factory
- [ ] Verificare che i messaggi siano in italiano (RNF_X_1)
- [ ] Verificare che il formato data/ora sia italiano (`dd/MM/yyyy`, `HH:mm`)

---

## Fase 6.4 — SseEmitterRegistry

**Obiettivo**: Gestire le connessioni SSE attive con una map thread-safe. Ogni utente autenticato può aprire una connessione SSE persistente.

```java
/**
 * Registro centralizzato delle connessioni SSE attive.
 * Usa ConcurrentHashMap per thread-safety sotto carico concorrente.
 */
@Component
@RequiredArgsConstructor
public class SseEmitterRegistry {

    private final UserRepository userRepository;

    // userId → SseEmitter attivo (un emitter per utente)
    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Registra un nuovo emitter SSE per l'utente.
     * Se esisteva già una connessione precedente, viene sostituita.
     */
    public SseEmitter register(Long userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);  // nessun timeout

        emitters.put(userId, emitter);

        // Cleanup automatico alla chiusura della connessione
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        // Invia evento di connessione (heartbeat iniziale)
        try {
            emitter.send(SseEmitter.event().name("connected").data("Connessione SSE attiva"));
        } catch (IOException e) {
            emitters.remove(userId);
        }

        return emitter;
    }

    /**
     * Invia una notifica a un utente specifico tramite SSE.
     * Se l'utente non ha una connessione attiva, non succede nulla (notifica persiste in DB).
     */
    public void pushToUser(Long userId, NotificationPushDto notification) {
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

    /**
     * Invia una notifica a tutti gli utenti con ruolo BARBER connessi.
     * (Nel sistema attuale c'è un solo BAR, ma il codice è scalabile.)
     */
    public void pushToAllBarbers(NotificationPushDto notification) {
        emitters.entrySet().stream()
            .filter(entry -> isBarber(entry.getKey()))
            .forEach(entry -> {
                try {
                    entry.getValue().send(SseEmitter.event()
                        .name("notification")
                        .data(notification));
                } catch (IOException e) {
                    emitters.remove(entry.getKey());
                }
            });
    }

    private boolean isBarber(Long userId) {
        return userRepository.findById(userId)
            .map(u -> u.getRuolo() == UserRole.BARBER)
            .orElse(false);
    }

    /** Per i test: verifica se un utente ha una connessione attiva */
    public boolean hasActiveConnection(Long userId) {
        return emitters.containsKey(userId);
    }

    /** Per i test: numero connessioni attive */
    public int getActiveConnectionCount() {
        return emitters.size();
    }
}
```

### Attività
- [ ] Creare `SseEmitterRegistry` con `ConcurrentHashMap`
- [ ] Verificare cleanup automatico alla disconnessione
- [ ] Creare `NotificationMapper` per la conversione `Notifica → NotificationPushDto`

---

## Fase 6.5 — NotificationService (Observer)

**Obiettivo**: Ascoltare i domain events pubblicati da `BookingService` e generare le notifiche corrispondenti.

```java
/**
 * Observer Pattern: ascolta gli Spring ApplicationEvent pubblicati da BookingService
 * e genera/persiste/pusha le notifiche in modo completamente disaccoppiato.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificaRepository notificaRepository;
    private final SseEmitterRegistry sseRegistry;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;

    // -------------------------------------------------------
    // OBSERVER: nuova richiesta → notifica al BAR
    // -------------------------------------------------------
    @EventListener
    @Async
    public void onBookingRequestCreated(BookingRequestCreatedEvent event) {
        Prenotazione booking = event.getBooking();
        User barber = getBarber();  // nel sistema: unico account BARBER

        Notifica n = NotificationFactory.createNewRequestNotification(booking, barber);
        n.setCreatedAt(LocalDateTime.now());
        notificaRepository.save(n);

        sseRegistry.pushToAllBarbers(notificationMapper.toPushDto(n));
    }

    // -------------------------------------------------------
    // OBSERVER: accettazione → notifica al CLR (non al CLG)
    // -------------------------------------------------------
    @EventListener
    @Async
    public void onBookingAccepted(BookingAcceptedEvent event) {
        Prenotazione booking = event.getBooking();

        if (booking.getClient() == null) {
            // CLG: nessuna notifica in-app — Tony contatta per telefono
            return;
        }

        Notifica n = NotificationFactory.createAcceptedNotification(booking);
        n.setCreatedAt(LocalDateTime.now());
        notificaRepository.save(n);

        sseRegistry.pushToUser(booking.getClient().getId(), notificationMapper.toPushDto(n));
    }

    // -------------------------------------------------------
    // OBSERVER: rifiuto → notifica al CLR
    // -------------------------------------------------------
    @EventListener
    @Async
    public void onBookingRejected(BookingRejectedEvent event) {
        Prenotazione booking = event.getBooking();

        if (booking.getClient() == null) return;  // CLG: nessuna notifica

        Notifica n = NotificationFactory.createRejectedNotification(booking);
        n.setCreatedAt(LocalDateTime.now());
        notificaRepository.save(n);

        sseRegistry.pushToUser(booking.getClient().getId(), notificationMapper.toPushDto(n));
    }

    // -------------------------------------------------------
    // OBSERVER: annullamento da CLR → notifica al BAR
    // -------------------------------------------------------
    @EventListener
    @Async
    public void onBookingCancelledByClient(BookingCancelledByClientEvent event) {
        Prenotazione booking = event.getBooking();
        User barber = getBarber();
        String reason = event.getCancellationReason();

        Notifica n = NotificationFactory.createClientCancellationNotification(booking, reason, barber);
        n.setCreatedAt(LocalDateTime.now());
        notificaRepository.save(n);

        sseRegistry.pushToAllBarbers(notificationMapper.toPushDto(n));
    }

    // -------------------------------------------------------
    // OBSERVER: cancellazione da BAR → notifica al CLR
    // -------------------------------------------------------
    @EventListener
    @Async
    public void onBookingCancelledByBarber(BookingCancelledByBarberEvent event) {
        Prenotazione booking = event.getBooking();

        if (booking.getClient() == null) return;  // CLG: nessuna notifica

        Notifica n = NotificationFactory.createBarberCancellationNotification(booking);
        n.setCreatedAt(LocalDateTime.now());
        notificaRepository.save(n);

        sseRegistry.pushToUser(booking.getClient().getId(), notificationMapper.toPushDto(n));
    }

    // -------------------------------------------------------
    // Recupero notifiche persistite
    // -------------------------------------------------------

    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotificationsForUser(Long userId) {
        return notificaRepository.findByDestinatarioIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(notificationMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notifica n = notificaRepository.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notifica non trovata: " + notificationId));
        if (!n.getDestinatario().getId().equals(userId)) {
            throw new UnauthorizedOperationException("Non autorizzato");
        }
        n.setLetta(true);
        notificaRepository.save(n);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificaRepository.markAllAsReadForUser(userId);
    }

    private User getBarber() {
        return userRepository.findByRuolo(UserRole.BARBER)
            .stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("Account BAR non trovato"));
    }
}
```

### Repository aggiuntivo — `NotificaRepository.java`
```java
public interface NotificaRepository extends JpaRepository<Notifica, Long> {

    List<Notifica> findByDestinatarioIdOrderByCreatedAtDesc(Long userId);

    long countByDestinatarioIdAndLettaFalse(Long userId);  // per badge non lette

    @Modifying
    @Query("UPDATE Notifica n SET n.letta = true WHERE n.destinatario.id = :userId")
    void markAllAsReadForUser(@Param("userId") Long userId);
}
```

### Configurazione `@Async`
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setThreadNamePrefix("notification-");
        return executor;
    }
}
```

### Attività
- [ ] Creare `NotificationService` con tutti i `@EventListener`
- [ ] Creare `NotificaRepository` con query custom
- [ ] Creare `AsyncConfig` con `@EnableAsync`
- [ ] Creare `NotificationMapper`

---

## Fase 6.6 — REST Controller SSE

### `NotificationController.java`
```java
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SseEmitterRegistry sseRegistry;
    private final NotificationService notificationService;

    /**
     * RF_GEN_5 — Apre una connessione SSE per ricevere push in real-time.
     * Il tipo MediaType.TEXT_EVENT_STREAM_VALUE è fondamentale.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(
            @AuthenticationPrincipal UserPrincipal principal) {
        return sseRegistry.register(principal.getId());
    }

    /**
     * RF_BAR_16 / RF_CLR_7 — Lista notifiche persistite (storico)
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
            notificationService.getNotificationsForUser(principal.getId()));
    }

    /** Segna una notifica come letta */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAsRead(id, principal.getId());
        return ResponseEntity.ok().build();
    }

    /** Segna tutte le notifiche come lette */
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.ok().build();
    }
}
```

### Configurazione CORS per SSE
Aggiungere in `SecurityConfig` o `CorsConfig`:
```java
// SSE richiede che CORS sia configurato esplicitamente per il frontend
configuration.setAllowedOrigins(List.of("http://localhost:3000", "https://yourdomain.com"));
configuration.setAllowCredentials(true);  // necessario per cookie HttpOnly
```

### API Endpoints Riepilogo
| Metodo | Path | Auth | Produce | RF |
|--------|------|------|---------|-----|
| GET | `/api/notifications/stream` | Autenticato | `text/event-stream` | RF_GEN_5 |
| GET | `/api/notifications` | Autenticato | `application/json` | RF_BAR_16, RF_CLR_7 |
| PATCH | `/api/notifications/{id}/read` | Autenticato | `application/json` | — |
| PATCH | `/api/notifications/read-all` | Autenticato | `application/json` | — |

### Attività
- [ ] Creare `NotificationController`
- [ ] Configurare CORS per SSE
- [ ] Verificare che l'evento SSE abbia il corretto `name` e `data`
- [ ] Aggiornare `SecurityConfig`: `/api/notifications/stream` richiede autenticazione

---

## Fase 6.7 — Unit Test

### `NotificationServiceTest.java`
```java
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificaRepository notificaRepository;
    @Mock SseEmitterRegistry sseRegistry;
    @Mock UserRepository userRepository;
    @Mock NotificationMapper notificationMapper;
    @InjectMocks NotificationService notificationService;

    // --- onBookingRequestCreated ---
    @Test void onBookingRequestCreated_clientBooking_createsNotificationForBarber()
    @Test void onBookingRequestCreated_savedToRepository()
    @Test void onBookingRequestCreated_pushedToBarberSSE()

    // --- onBookingAccepted ---
    @Test void onBookingAccepted_registeredClient_createsNotificationForClient()
    @Test void onBookingAccepted_guestBooking_noNotificationCreated()
    @Test void onBookingAccepted_pushedToClientSSE()

    // --- onBookingRejected ---
    @Test void onBookingRejected_registeredClient_createsRejectedNotification()
    @Test void onBookingRejected_guestBooking_noNotificationCreated()

    // --- onBookingCancelledByClient ---
    @Test void onBookingCancelledByClient_createsNotificationForBarber()
    @Test void onBookingCancelledByClient_cancellationReasonIncludedInMessage()

    // --- onBookingCancelledByBarber ---
    @Test void onBookingCancelledByBarber_registeredClient_createsNotificationForClient()
    @Test void onBookingCancelledByBarber_guestBooking_noNotificationCreated()

    // --- markAsRead ---
    @Test void markAsRead_ownNotification_setsLettaTrue()
    @Test void markAsRead_notOwner_throwsUnauthorizedException()
    @Test void markAsRead_notFound_throwsResourceNotFoundException()
}
```

### `NotificationFactoryTest.java`
```java
class NotificationFactoryTest {

    @Test void createNewRequestNotification_containsCorrectClientName()
    @Test void createNewRequestNotification_containsServiceName()
    @Test void createNewRequestNotification_containsFormattedDate()
    @Test void createNewRequestNotification_recipientIsBarber()

    @Test void createAcceptedNotification_recipientIsClient()
    @Test void createAcceptedNotification_containsServiceAndDate()

    @Test void createRejectedNotification_recipientIsClient()

    @Test void createClientCancellationNotification_containsReason()
    @Test void createClientCancellationNotification_recipientIsBarber()

    @Test void createBarberCancellationNotification_recipientIsClient()

    // Test lingua italiana
    @Test void allNotifications_titlesAreInItalian()
}
```

### `SseEmitterRegistryTest.java`
```java
class SseEmitterRegistryTest {

    @Test void register_newUser_returnsEmitter()
    @Test void register_replacesPreviousEmitter()
    @Test void pushToUser_activeConnection_sends()
    @Test void pushToUser_noActiveConnection_silentNoOp()
    @Test void hasActiveConnection_registeredUser_returnsTrue()
    @Test void hasActiveConnection_unregisteredUser_returnsFalse()
}
```

### Attività
- [ ] Implementare `NotificationServiceTest`
- [ ] Implementare `NotificationFactoryTest`
- [ ] Implementare `SseEmitterRegistryTest`
- [ ] Coverage `NotificationService` ≥ 80%

---

## Fase 6.8 — Integration Test

### `NotificationIntegrationTest.java`
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class NotificationIntegrationTest {

    // --- Persistenza notifiche ---
    @Test void bookingAccepted_notificationPersistedInDb()
    @Test void bookingRejected_notificationPersistedInDb()
    @Test void clientCancellation_barberNotificationPersistedInDb()
    @Test void guestBookingAccepted_noNotificationInDb()

    // --- API ---
    @Test void getNotifications_asClient_returnsOwnNotifications()
    @Test void getNotifications_asBarber_returnsOwnNotifications()
    @Test void markAsRead_ownNotification_returns200()
    @Test void markAsRead_notOwner_returns403()
    @Test void markAllAsRead_updatesAllUserNotifications()

    // --- Flow Observer completo ---
    @Test void fullFlow_clientRequestThenAccept_twoNotificationsCreated()
    // 1. CLR invia richiesta → notifica per BAR (NUOVA_RICHIESTA)
    // 2. BAR accetta → notifica per CLR (PRENOTAZIONE_ACCETTATA)
    // Totale: 2 notifiche in DB
}
```

> **Nota**: I test SSE di push real-time richiedono un client SSE nello stesso test — complesso in JUnit. Si testa indirettamente verificando che le notifiche siano **persistite in DB** e che `sseRegistry.pushToUser` sia chiamato (via mock o spy).

### Attività
- [ ] Implementare `NotificationIntegrationTest`
- [ ] Verificare che `@Async` non crei race condition nei test (usare `@DirtiesContext` o await)

---

## Fase 6.9 — Verifica Quality Gate

### Checklist finale Sprint 6
- [ ] CLR riceve notifica in-app per accettazione prenotazione
- [ ] CLR riceve notifica in-app per rifiuto prenotazione
- [ ] CLR riceve notifica in-app per cancellazione da BAR
- [ ] BAR riceve notifica in-app per nuova richiesta
- [ ] BAR riceve notifica in-app per annullamento da CLR (con motivazione nel testo)
- [ ] CLG **non** riceve notifiche in-app (verificato con test)
- [ ] Notifiche persistite in DB (storico)
- [ ] SSE endpoint configurato e funzionante (test con curl)
- [ ] `@Async` non blocca il thread di `BookingService`
- [ ] Coverage `NotificationService` ≥ 80%
- [ ] CI pipeline verde

---

## Definition of Done — Sprint 6

| Criterio | Verifica |
|----------|----------|
| ✅ RF_GEN_5 Notifiche in-app | Push SSE su ogni evento di prenotazione |
| ✅ RF_BAR_16 Area notifiche BAR | BAR riceve notifiche per nuove richieste e annullamenti |
| ✅ RF_CLR_7 Area notifiche CLR | CLR riceve notifiche per accettazioni, rifiuti, cancellazioni |
| ✅ CLG escluso | Nessuna notifica in-app per prenotazioni ospite |
| ✅ Observer disaccoppiato | BookingService non dipende da NotificationService |
| ✅ Factory Method centralizzato | Tutti i messaggi costruiti in NotificationFactory |
| ✅ ConcurrentHashMap thread-safe | SseEmitterRegistry sicuro sotto carico |
| ✅ Persistenza notifiche | Ogni notifica salvata in DB per storico |
| ✅ Messaggio in italiano | Tutti i titoli e testi rispettano RNF_X_1 |
| ✅ Unit test ≥ 80% coverage | NotificationService e Factory coperti |
| ✅ CI pipeline verde | GitHub Actions passa |

---

## Note Operative

- `@Async` su `@EventListener` è fondamentale: senza di esso, la notifica verrebbe inviata nel thread della transazione di `BookingService`, causando un potenziale timeout se il push SSE è lento.
- In presenza di un `@Transactional` nel publisher, gli eventi Spring vengono pubblicati **dopo il commit della transazione**. Questo è esattamente il comportamento che vogliamo: la notifica viene inviata solo se la prenotazione è stata salvata con successo.
- Se l'utente non ha una connessione SSE attiva, la notifica è comunque **persistita in DB** e sarà visibile al prossimo `GET /api/notifications`.
- In **Sprint 9** (Frontend), il hook `useNotifications()` aprirà la connessione SSE automaticamente per ogni utente autenticato.

---

*Sprint 6 — Ultima modifica: 22/04/2026*
