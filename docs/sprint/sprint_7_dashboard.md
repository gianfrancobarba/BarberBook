# Sprint 7 — Dashboard & Storico Prenotazioni
> **Stato**: ⬜ Non iniziato  
> **Dipende da**: Sprint 5 ✅ + Sprint 6 ✅  
> **Obiettivo**: Fornire al BAR le viste operative dell'agenda (giornaliera e settimanale) e al CLR il proprio portale personale con homepage, storico filtrato e prossimi appuntamenti.

---

## Requisiti Funzionali Coperti

| RF | Nome | Attore | Priorità |
|----|------|--------|----------|
| RF_BAR_1 | Dashboard Settimanale | BAR | Alta |
| RF_BAR_2 | Dashboard Giornaliera | BAR | Alta |
| RF_CLR_1 | Homepage Personale CLR | CLR | Media |
| RF_CLR_2 | Visualizzazione Storico Prenotazioni | CLR | Media |
| RF_CLR_3 | Filtro Prenotazioni per Stato | CLR | Media |

---

## Indice Fasi

1. [Fase 7.1 — Specification Pattern (Query Composte)](#fase-71--specification-pattern-query-composte)
2. [Fase 7.2 — DashboardService (BAR)](#fase-72--dashboardservice-bar)
3. [Fase 7.3 — ClientPortalService (CLR)](#fase-73--clientportalservice-clr)
4. [Fase 7.4 — DTO per Dashboard e Storico](#fase-74--dto-per-dashboard-e-storico)
5. [Fase 7.5 — REST Controllers](#fase-75--rest-controllers)
6. [Fase 7.6 — Unit Test](#fase-76--unit-test)
7. [Fase 7.7 — Integration Test](#fase-77--integration-test)
8. [Fase 7.8 — Verifica Quality Gate](#fase-78--verifica-quality-gate)

---

## Fase 7.1 — Specification Pattern (Query Composte)

**Obiettivo**: Implementare query JPA composabili a runtime per filtrare le prenotazioni per stato, data, cliente e poltrona. Evita l'esplosione di metodi nel repository.

### `BookingSpecifications.java`
```java
/**
 * Specification Pattern (JPA Criteria API):
 * ogni metodo statico ritorna una Specification componibile.
 * Le specifiche vengono combinate con .and() e .or() a runtime nel Service.
 */
public class BookingSpecifications {

    private BookingSpecifications() {}

    /** Filtra per cliente (storico CLR) */
    public static Specification<Prenotazione> byClient(Long clientId) {
        return (root, query, cb) ->
            cb.equal(root.get("client").get("id"), clientId);
    }

    /** Filtra per stato (RF_CLR_3) */
    public static Specification<Prenotazione> byStatus(BookingStatus status) {
        return (root, query, cb) ->
            cb.equal(root.get("status"), status);
    }

    /** Filtra per giorno specifico (dashboard BAR) */
    public static Specification<Prenotazione> byDate(LocalDate date) {
        return (root, query, cb) ->
            cb.between(
                root.get("startTime"),
                date.atStartOfDay(),
                date.atTime(LocalTime.MAX)
            );
    }

    /** Filtra per settimana (dashboard settimanale BAR) */
    public static Specification<Prenotazione> byWeek(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        return (root, query, cb) ->
            cb.between(
                root.get("startTime"),
                weekStart.atStartOfDay(),
                weekEnd.atTime(LocalTime.MAX)
            );
    }

    /** Filtra per poltrona (dashboard per poltrona) */
    public static Specification<Prenotazione> byChair(Long chairId) {
        return (root, query, cb) ->
            cb.equal(root.get("poltrona").get("id"), chairId);
    }

    /** Solo prenotazioni future (per homepage CLR) */
    public static Specification<Prenotazione> upcoming() {
        return (root, query, cb) ->
            cb.greaterThan(root.get("startTime"), LocalDateTime.now());
    }

    /** Esclude stati non rilevanti nella dashboard BAR */
    public static Specification<Prenotazione> notCancelledOrRejected() {
        return (root, query, cb) ->
            root.get("status").in(BookingStatus.IN_ATTESA, BookingStatus.ACCETTATA, BookingStatus.PASSATA);
    }

    /** Solo prenotazioni future confermate (homepage CLR) */
    public static Specification<Prenotazione> upcomingConfirmed() {
        return upcoming().and(byStatus(BookingStatus.ACCETTATA));
    }
}
```

### Abilitare Specification sul repository
```java
// PrenotazioneRepository già estende JpaSpecificationExecutor<Prenotazione> da Sprint 5
// Nessuna modifica necessaria — già pronto
```

### Attività
- [ ] Creare `BookingSpecifications` con tutti i metodi statici
- [ ] Verificare che `PrenotazioneRepository` estenda `JpaSpecificationExecutor<Prenotazione>`

---

## Fase 7.2 — DashboardService (BAR)

**Obiettivo**: Fornire i dati strutturati per le viste agenda del BAR — giornaliera e settimanale.

### `DashboardService.java`
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final PrenotazioneRepository prenotazioneRepository;
    private final PoltronaRepository poltronaRepository;
    private final BookingMapper bookingMapper;

    // -------------------------------------------------------
    // RF_BAR_2 — Dashboard Giornaliera
    // -------------------------------------------------------

    /**
     * Ritorna tutte le prenotazioni del giorno per ogni poltrona attiva,
     * raggruppate per poltrona e ordinate cronologicamente.
     * Esclude prenotazioni RIFIUTATE e ANNULLATE.
     */
    public DailyDashboardResponseDto getDailyDashboard(LocalDate date) {
        List<Poltrona> activeChairs = poltronaRepository.findByAttivaTrue();

        List<ChairDayScheduleDto> schedules = activeChairs.stream().map(chair -> {
            Specification<Prenotazione> spec = BookingSpecifications.byDate(date)
                .and(BookingSpecifications.byChair(chair.getId()))
                .and(BookingSpecifications.notCancelledOrRejected());

            List<Prenotazione> bookings = prenotazioneRepository.findAll(spec,
                Sort.by("startTime").ascending());

            return new ChairDayScheduleDto(
                chair.getId(),
                chair.getNome(),
                date,
                bookingMapper.toDtoList(bookings)
            );
        }).collect(Collectors.toList());

        return new DailyDashboardResponseDto(date, schedules);
    }

    // -------------------------------------------------------
    // RF_BAR_1 — Dashboard Settimanale
    // -------------------------------------------------------

    /**
     * Ritorna l'agenda della settimana (7 giorni da weekStart),
     * per ogni giorno e ogni poltrona.
     * Struttura: Map<giorno, Map<poltrona, prenotazioni>>
     */
    public WeeklyDashboardResponseDto getWeeklyDashboard(LocalDate weekStart) {
        List<Poltrona> activeChairs = poltronaRepository.findByAttivaTrue();
        LocalDate weekEnd = weekStart.plusDays(6);

        // Carica tutte le prenotazioni della settimana in un'unica query
        Specification<Prenotazione> spec = BookingSpecifications.byWeek(weekStart)
            .and(BookingSpecifications.notCancelledOrRejected());

        List<Prenotazione> weekBookings = prenotazioneRepository.findAll(spec,
            Sort.by("startTime").ascending());

        // Raggruppa per giorno, poi per poltrona
        List<DayScheduleDto> days = IntStream.rangeClosed(0, 6)
            .mapToObj(weekStart::plusDays)
            .map(day -> {
                List<ChairDayScheduleDto> chairSchedules = activeChairs.stream().map(chair -> {
                    List<Prenotazione> dayChairBookings = weekBookings.stream()
                        .filter(b -> b.getStartTime().toLocalDate().equals(day))
                        .filter(b -> b.getPoltrona().getId().equals(chair.getId()))
                        .collect(Collectors.toList());

                    return new ChairDayScheduleDto(
                        chair.getId(),
                        chair.getNome(),
                        day,
                        bookingMapper.toDtoList(dayChairBookings)
                    );
                }).collect(Collectors.toList());

                return new DayScheduleDto(day, chairSchedules);
            })
            .collect(Collectors.toList());

        return new WeeklyDashboardResponseDto(weekStart, weekEnd, days);
    }
}
```

### Attività
- [ ] Creare `DashboardService` con `getDailyDashboard` e `getWeeklyDashboard`
- [ ] Verificare che la query settimanale carichi i dati in una sola query (no N+1)

---

## Fase 7.3 — ClientPortalService (CLR)

**Obiettivo**: Fornire al CLR il proprio portale personale — homepage, storico filtrato, prenotazioni prossime.

### `ClientPortalService.java`
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientPortalService {

    private final PrenotazioneRepository prenotazioneRepository;
    private final BookingMapper bookingMapper;

    // -------------------------------------------------------
    // RF_CLR_1 — Homepage: prossimi appuntamenti confermati
    // -------------------------------------------------------

    public List<BookingResponseDto> getUpcomingBookings(User client) {
        return prenotazioneRepository.findAll(
            BookingSpecifications.byClient(client.getId())
                .and(BookingSpecifications.upcomingConfirmed()),
            Sort.by("startTime").ascending()
        ).stream()
            .map(bookingMapper::toDto)
            .collect(Collectors.toList());
    }

    // -------------------------------------------------------
    // RF_CLR_2 — Storico completo prenotazioni
    // -------------------------------------------------------

    public List<BookingResponseDto> getBookingHistory(User client) {
        return prenotazioneRepository.findAll(
            BookingSpecifications.byClient(client.getId()),
            Sort.by("startTime").descending()
        ).stream()
            .map(bookingMapper::toDto)
            .collect(Collectors.toList());
    }

    // -------------------------------------------------------
    // RF_CLR_3 — Storico filtrato per stato
    // -------------------------------------------------------

    public List<BookingResponseDto> getBookingsByStatus(User client, BookingStatus status) {
        Specification<Prenotazione> spec = BookingSpecifications.byClient(client.getId());

        if (status != null) {
            spec = spec.and(BookingSpecifications.byStatus(status));
        }

        return prenotazioneRepository.findAll(spec, Sort.by("startTime").descending())
            .stream()
            .map(bookingMapper::toDto)
            .collect(Collectors.toList());
    }
}
```

### Attività
- [ ] Creare `ClientPortalService` con i 3 metodi
- [ ] Verificare che `byClient` non esponga prenotazioni di altri utenti (isolamento dati)

---

## Fase 7.4 — DTO per Dashboard e Storico

**Obiettivo**: Strutturare i DTO di risposta per le viste dashboard e storico.

### DTO Dashboard
```java
// Singola poltrona in un giorno
public record ChairDayScheduleDto(
    Long chairId,
    String chairName,
    LocalDate date,
    List<BookingResponseDto> bookings
) {}

// Dashboard giornaliera completa
public record DailyDashboardResponseDto(
    LocalDate date,
    List<ChairDayScheduleDto> chairs
) {}

// Un giorno della settimana (con tutte le poltrone)
public record DayScheduleDto(
    LocalDate date,
    String dayName,     // "Lunedì", "Martedì" ecc.
    List<ChairDayScheduleDto> chairs
) {}

// Dashboard settimanale completa
public record WeeklyDashboardResponseDto(
    LocalDate weekStart,
    LocalDate weekEnd,
    List<DayScheduleDto> days   // 7 giorni sempre presenti (anche se vuoti)
) {}

// Homepage CLR
public record ClientHomepageDto(
    String clientName,
    List<BookingResponseDto> upcomingBookings,  // prossimi appuntamenti confermati
    int totalBookings,                           // contatore statistico
    long unreadNotifications                     // badge notifiche non lette
) {}
```

### Attività
- [ ] Creare tutti i DTO di dashboard e storico
- [ ] Aggiungere `dayName` in italiano in `DayScheduleDto` (es. via `date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ITALIAN)`)

---

## Fase 7.5 — REST Controllers

### `DashboardController.java` (BAR-only)
```java
@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('BARBER')")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** RF_BAR_2 — Agenda giornaliera */
    @GetMapping("/daily")
    public ResponseEntity<DailyDashboardResponseDto> getDailyDashboard(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(dashboardService.getDailyDashboard(targetDate));
    }

    /** RF_BAR_1 — Agenda settimanale */
    @GetMapping("/weekly")
    public ResponseEntity<WeeklyDashboardResponseDto> getWeeklyDashboard(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        // Default: lunedì della settimana corrente
        LocalDate start = weekStart != null
            ? weekStart
            : LocalDate.now().with(DayOfWeek.MONDAY);
        return ResponseEntity.ok(dashboardService.getWeeklyDashboard(start));
    }
}
```

### `ClientPortalController.java` (CLIENT-only)
```java
@RestController
@RequestMapping("/api/client")
@PreAuthorize("hasRole('CLIENT')")
@RequiredArgsConstructor
public class ClientPortalController {

    private final ClientPortalService clientPortalService;
    private final NotificationService notificationService;

    /** RF_CLR_1 — Homepage con prossimi appuntamenti */
    @GetMapping("/homepage")
    public ResponseEntity<ClientHomepageDto> getHomepage(
            @AuthenticationPrincipal UserPrincipal principal) {
        User client = principal.getUser();
        List<BookingResponseDto> upcoming = clientPortalService.getUpcomingBookings(client);
        long unread = notificationService.countUnreadForUser(principal.getId());

        return ResponseEntity.ok(new ClientHomepageDto(
            client.getNome() + " " + client.getCognome(),
            upcoming,
            upcoming.size(),
            unread
        ));
    }

    /** RF_CLR_2 — Storico completo */
    @GetMapping("/bookings")
    public ResponseEntity<List<BookingResponseDto>> getHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
            clientPortalService.getBookingHistory(principal.getUser()));
    }

    /** RF_CLR_3 — Storico filtrato per stato */
    @GetMapping("/bookings/filter")
    public ResponseEntity<List<BookingResponseDto>> getByStatus(
            @RequestParam(required = false) BookingStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
            clientPortalService.getBookingsByStatus(principal.getUser(), status));
    }

    /** RF_CLR_2 — Prenotazioni future (prossimi appuntamenti) */
    @GetMapping("/bookings/upcoming")
    public ResponseEntity<List<BookingResponseDto>> getUpcoming(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
            clientPortalService.getUpcomingBookings(principal.getUser()));
    }
}
```

### API Endpoints Riepilogo
| Metodo | Path | Auth | RF |
|--------|------|------|----|
| GET | `/api/dashboard/daily?date={date}` | BARBER | RF_BAR_2 |
| GET | `/api/dashboard/weekly?weekStart={date}` | BARBER | RF_BAR_1 |
| GET | `/api/client/homepage` | CLIENT | RF_CLR_1 |
| GET | `/api/client/bookings` | CLIENT | RF_CLR_2 |
| GET | `/api/client/bookings/filter?status={status}` | CLIENT | RF_CLR_3 |
| GET | `/api/client/bookings/upcoming` | CLIENT | RF_CLR_1 |

### Attività
- [ ] Creare `DashboardController`
- [ ] Creare `ClientPortalController`
- [ ] Aggiungere metodo `countUnreadForUser` al `NotificationService`

---

## Fase 7.6 — Unit Test

### `BookingSpecificationsTest.java`
```java
@ExtendWith(MockitoExtension.class)
class BookingSpecificationsTest {
    // Test con H2/Testcontainers (richiedono un contesto JPA)
    // Più efficiente testarli nell'Integration Test

    // Qui testiamo la logica pura di composizione
    @Test void upcomingConfirmed_isCompositionOfTwoPredicate()
    @Test void specifications_canBeComposedWithAnd()
}
```

### `DashboardServiceTest.java`
```java
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock PrenotazioneRepository prenotazioneRepository;
    @Mock PoltronaRepository poltronaRepository;
    @Mock BookingMapper bookingMapper;
    @InjectMocks DashboardService dashboardService;

    @Test void getDailyDashboard_2chairs_returnsBothChairsInResponse()
    @Test void getDailyDashboard_noBookings_returnsEmptyListPerChair()
    @Test void getDailyDashboard_bookingsOrderedChronologically()
    @Test void getDailyDashboard_cancelledBookings_notIncluded()
    @Test void getWeeklyDashboard_always7Days_evenIfEmpty()
    @Test void getWeeklyDashboard_bookingsCorrectlyGroupedByDayAndChair()
}
```

### `ClientPortalServiceTest.java`
```java
@ExtendWith(MockitoExtension.class)
class ClientPortalServiceTest {

    @Mock PrenotazioneRepository prenotazioneRepository;
    @Mock BookingMapper bookingMapper;
    @InjectMocks ClientPortalService clientPortalService;

    @Test void getUpcomingBookings_onlyFutureAccepted()
    @Test void getUpcomingBookings_pastAccepted_notReturned()
    @Test void getBookingHistory_allStatuses_returned()
    @Test void getBookingHistory_orderedNewestFirst()
    @Test void getBookingsByStatus_nullStatus_returnsAll()
    @Test void getBookingsByStatus_withStatus_filtersCorrectly()
    @Test
    @DisplayName("Isolamento dati: getBookingHistory ritorna solo le prenotazioni del cliente")
    void getBookingHistory_onlyOwnBookings()
}
```

### Attività
- [ ] Implementare `DashboardServiceTest`
- [ ] Implementare `ClientPortalServiceTest`
- [ ] Coverage `DashboardService` e `ClientPortalService` ≥ 80%

---

## Fase 7.7 — Integration Test

### `DashboardIntegrationTest.java`
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class DashboardIntegrationTest {

    // --- Dashboard BAR ---
    @Test void getDailyDashboard_asBarber_returns200()
    @Test void getDailyDashboard_asClient_returns403()
    @Test void getDailyDashboard_noDate_defaultsToToday()
    @Test void getDailyDashboard_hasCorrectChairs()
    @Test void getDailyDashboard_with3Bookings_returns3InResponse()
    @Test void getWeeklyDashboard_always7DaysInResponse()
    @Test void getWeeklyDashboard_cancelledBookings_notInResponse()

    // --- Portal CLR ---
    @Test void getHomepage_asClient_returns200WithOwnData()
    @Test void getHomepage_asBarber_returns403()
    @Test void getBookingHistory_returns_onlyOwnBookings()
    @Test void getBookingsByFilter_status_ACCETTATA_returnsOnlyAccepted()
    @Test void getBookingsByFilter_noStatus_returnsAll()

    // --- Isolamento ---
    @Test
    @DisplayName("CRITICO: CLR1 non vede le prenotazioni di CLR2")
    void clientDataIsolation_cannotSeeOtherClientBookings()
}
```

### Attività
- [ ] Implementare `DashboardIntegrationTest`
- [ ] Verificare il test di isolamento dati (scenari con 2 client diversi)
- [ ] Verificare che la dashboard settimanale abbia sempre esattamente 7 elementi

---

## Fase 7.8 — Verifica Quality Gate

### Checklist finale Sprint 7
- [ ] Dashboard giornaliera raggruppata per poltrona, ordinata cronologicamente
- [ ] Dashboard settimanale con 7 giorni (anche vuoti) per ogni poltrona
- [ ] Homepage CLR con prossimi appuntamenti confermati
- [ ] Storico filtrato per stato funzionante
- [ ] Isolamento dati: CLR vede solo le proprie prenotazioni
- [ ] Prenotazioni RIFIUTATE e ANNULLATE non appaiono nella dashboard BAR
- [ ] Coverage Service layer ≥ 80%
- [ ] CI pipeline verde

---

## Definition of Done — Sprint 7

| Criterio | Verifica |
|----------|----------|
| ✅ RF_BAR_2 Dashboard Giornaliera | `GET /api/dashboard/daily` raggruppata per poltrona |
| ✅ RF_BAR_1 Dashboard Settimanale | `GET /api/dashboard/weekly` con 7 giorni per poltrona |
| ✅ RF_CLR_1 Homepage CLR | Prossimi appuntamenti confermati ordinati cronologicamente |
| ✅ RF_CLR_2 Storico | Lista completa proprie prenotazioni, ordine decrescente |
| ✅ RF_CLR_3 Filtro per stato | `?status=ACCETTATA` filtra correttamente |
| ✅ Specification Pattern | Query composte, nessun metodo nome-esplosione nel repository |
| ✅ Isolamento dati | CLR non accede alle prenotazioni di altri clienti |
| ✅ Unit test ≥ 80% | DashboardService e ClientPortalService coperti |
| ✅ Integration test | Tutti i flussi testati con DB reale |
| ✅ CI pipeline verde | GitHub Actions passa |

---

## Note Operative

- La **Dashboard Settimanale** ha sempre 7 giorni nella risposta, anche se alcuni giorni sono vuoti (domenica chiuso → lista vuota). Questo semplifica enormemente il rendering nel frontend.
- Il **default** per `getDailyDashboard` senza parametro `date` è `LocalDate.now()`. Per la dashboard settimanale il default è il lunedì della settimana corrente.
- In **Sprint 9** (Frontend), `DashboardGiornalieraUI` e `DashboardSettimanaleUI` consumeranno questi endpoint tramite `useQuery` di TanStack Query con `staleTime` di 30 secondi.
- Il test di **isolamento dati** è classificato come critico: un bug in questo punto esporrebbe le prenotazioni private di un cliente a un altro cliente.

---

*Sprint 7 — Ultima modifica: 22/04/2026*
