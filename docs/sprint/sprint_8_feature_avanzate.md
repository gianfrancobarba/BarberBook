# Sprint 8 — Feature Avanzate
> **Stato**: ⬜ Non iniziato  
> **Dipende da**: Sprint 5 ✅ + Sprint 7 ✅  
> **Obiettivo**: Completare le funzionalità secondarie che arricchiscono l'esperienza utente — password recovery, modifica profilo, riprenotazione rapida e conversione ospite in cliente registrato.

---

## Requisiti Funzionali Coperti

| RF | Nome | Attore | Priorità |
|----|------|--------|----------|
| RF_GEN_4 | Recupero Password | CLR | Media |
| RF_CLR_6 | Modifica Profilo Personale | CLR | Alta |
| RF_CLR_5 | Riprenotazione Rapida | CLR | Bassa |
| RF_CLG_2 | Registrazione Veloce Post-Prenotazione | CLG | Bassa |

---

## Indice Fasi

1. [Fase 8.1 — Migrazione Flyway (Password Reset Token)](#fase-81--migrazione-flyway-password-reset-token)
2. [Fase 8.2 — Password Recovery (RF_GEN_4)](#fase-82--password-recovery-rf_gen_4)
3. [Fase 8.3 — Modifica Profilo (RF_CLR_6)](#fase-83--modifica-profilo-rf_clr_6)
4. [Fase 8.4 — Riprenotazione Rapida (RF_CLR_5)](#fase-84--riprenotazione-rapida-rf_clr_5)
5. [Fase 8.5 — Registrazione Veloce Post-Prenotazione (RF_CLG_2)](#fase-85--registrazione-veloce-post-prenotazione-rf_clg_2)
6. [Fase 8.6 — Unit Test](#fase-86--unit-test)
7. [Fase 8.7 — Integration Test](#fase-87--integration-test)
8. [Fase 8.8 — Verifica Quality Gate](#fase-88--verifica-quality-gate)

---

## Fase 8.1 — Migrazione Flyway (Password Reset Token)

**Obiettivo**: Schema per i token di recupero password — monouso, con scadenza, sicuri.

### `V13__password_reset_tokens.sql`
```sql
CREATE TABLE password_reset_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,   -- SHA-256 del token — mai in chiaro
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at  TIMESTAMP NOT NULL,             -- scadenza 1 ora
    used        BOOLEAN NOT NULL DEFAULT FALSE, -- token monouso
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prt_token  ON password_reset_tokens(token_hash);
CREATE INDEX idx_prt_user   ON password_reset_tokens(user_id);
CREATE INDEX idx_prt_expiry ON password_reset_tokens(expires_at);
```

### Entità JPA — `PasswordResetToken.java`
```java
@Entity
@Table(name = "password_reset_tokens")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PasswordResetToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
```

### Attività
- [ ] Creare `V13__password_reset_tokens.sql`
- [ ] Creare `PasswordResetToken.java`
- [ ] Creare `PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long>`

---

## Fase 8.2 — Password Recovery (RF_GEN_4)

**Obiettivo**: CLR può reimpostare la password tramite un link inviato all'email. Il token è monouso e scade dopo 1 ora.

### `PasswordResetService.java`
```java
@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;  // astrazione per invio email

    private static final Duration TOKEN_EXPIRY = Duration.ofHours(1);

    // -------------------------------------------------------
    // RF_GEN_4 — Richiesta reset password
    // -------------------------------------------------------

    /**
     * Genera un token di reset e lo invia all'email dell'utente.
     *
     * SECURITY: Non rivela se l'email è registrata o meno.
     * La risposta è sempre identica (200 OK) indipendentemente dall'esito.
     */
    public void requestPasswordReset(String email) {
        userRepository.findByEmailAndType(email, ClienteRegistrato.class).ifPresent(user -> {
            // Invalida eventuali token precedenti non usati
            tokenRepository.invalidatePreviousTokensForUser(user.getId());

            // Genera token casuale sicuro
            String tokenRaw = generateSecureToken();
            String tokenHash = hashToken(tokenRaw);

            PasswordResetToken token = PasswordResetToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(LocalDateTime.now().plus(TOKEN_EXPIRY))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
            tokenRepository.save(token);

            // Invia email con link contenente tokenRaw (mai tokenHash)
            emailService.sendPasswordResetEmail(user.getEmail(), user.getNome(), tokenRaw);
        });
        // Se l'email non è registrata: nessuna azione, nessuna risposta diversa
    }

    // -------------------------------------------------------
    // RF_GEN_4 — Conferma reset con nuovo password
    // -------------------------------------------------------

    public void resetPassword(String tokenRaw, String newPassword) {
        String tokenHash = hashToken(tokenRaw);

        PasswordResetToken token = tokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new InvalidTokenException("Token non valido o già utilizzato"));

        if (token.isUsed()) {
            throw new InvalidTokenException("Token già utilizzato");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Token scaduto. Richiedere un nuovo link.");
        }

        // Aggiorna la password
        User user = token.getUser();
        if (user instanceof ClienteRegistrato client) {
            client.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(client);
        }

        // Marca il token come usato (monouso)
        token.setUsed(true);
        tokenRepository.save(token);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String raw) {
        return DigestUtils.sha256Hex(raw);
    }
}
```

### `EmailService.java` (astrazione)
```java
@Service
public class EmailService {

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    /**
     * In sviluppo: logga il link nel console (Mailhog opzionale).
     * In produzione: usa JavaMailSender con SMTP.
     */
    public void sendPasswordResetEmail(String to, String nome, String token) {
        String resetLink = baseUrl + "/reset-password?token=" + token;
        // TODO Sprint 10: sostituire con JavaMailSender per produzione
        log.info("📧 PASSWORD RESET EMAIL → {} : {}", to, resetLink);
    }
}
```

### DTO
```java
public record ForgotPasswordRequestDto(
    @Email @NotBlank String email
) {}

public record ResetPasswordRequestDto(
    @NotBlank String token,
    @NotBlank @Size(min = 8) String newPassword
) {}
```

### Endpoint
```java
@PostMapping("/auth/forgot-password")
public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto dto) {
    passwordResetService.requestPasswordReset(dto.email());
    return ResponseEntity.ok().build();  // sempre 200, non rivela se email esiste
}

@PostMapping("/auth/reset-password")
public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDto dto) {
    passwordResetService.resetPassword(dto.token(), dto.newPassword());
    return ResponseEntity.ok().build();
}
```

### Attività
- [ ] Creare `PasswordResetService`
- [ ] Creare `EmailService` con implementazione dev (log only)
- [ ] Creare DTO e aggiungere endpoint ad `AuthController`
- [ ] Creare `PasswordResetTokenRepository` con query `findByTokenHash`, `invalidatePreviousTokensForUser`

---

## Fase 8.3 — Modifica Profilo (RF_CLR_6)

**Obiettivo**: CLR visualizza e modifica i propri dati personali con validazione dei formati.

### `ProfileService.java`
```java
@Service
@RequiredArgsConstructor
@Transactional
public class ProfileService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /** RF_CLR_6 — Visualizzazione profilo */
    @Transactional(readOnly = true)
    public UserResponseDto getProfile(User user) {
        return userMapper.toDto(user);
    }

    /** RF_CLR_6 — Aggiornamento profilo */
    public UserResponseDto updateProfile(User user, UpdateProfileRequestDto dto) {
        if (user instanceof ClienteRegistrato client) {
            // Verifica unicità nuova email (se cambia)
            if (dto.email() != null && !dto.email().equals(client.getEmail())) {
                if (userRepository.existsByEmail(dto.email())) {
                    throw new EmailAlreadyExistsException("Email già registrata: " + dto.email());
                }
                client.setEmail(dto.email());
            }

            if (dto.nome() != null)     client.setNome(dto.nome());
            if (dto.cognome() != null)  client.setCognome(dto.cognome());
            if (dto.telefono() != null) client.setTelefono(sanitizeTelefono(dto.telefono()));

            client.setUpdatedAt(LocalDateTime.now());
            return userMapper.toDto(userRepository.save(client));
        }
        throw new UnsupportedOperationException("Solo i clienti registrati possono modificare il profilo");
    }

    private String sanitizeTelefono(String telefono) {
        // Rimuove spazi e trattini, mantiene solo cifre e '+'
        return telefono.replaceAll("[\\s\\-]", "");
    }
}
```

### DTO
```java
public record UpdateProfileRequestDto(
    @Size(max = 100) String nome,
    @Size(max = 100) String cognome,
    @Email @Size(max = 255) String email,
    @Pattern(regexp = "\\+?[0-9\\s\\-]{8,20}",
             message = "Numero di telefono non valido") String telefono
) {}
```

### Endpoint
```java
@GetMapping("/api/users/me")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<UserResponseDto> getMe(@AuthenticationPrincipal UserPrincipal principal) {
    return ResponseEntity.ok(profileService.getProfile(principal.getUser()));
}

@PatchMapping("/api/users/me")
@PreAuthorize("hasRole('CLIENT')")
public ResponseEntity<UserResponseDto> updateMe(
        @Valid @RequestBody UpdateProfileRequestDto dto,
        @AuthenticationPrincipal UserPrincipal principal) {
    return ResponseEntity.ok(profileService.updateProfile(principal.getUser(), dto));
}
```

### Attività
- [ ] Creare `ProfileService`
- [ ] Creare `UpdateProfileRequestDto` con validazioni
- [ ] Aggiungere endpoint a `UserController`
- [ ] Verificare che la modifica email non crei conflitti con altri account

---

## Fase 8.4 — Riprenotazione Rapida (RF_CLR_5)

**Obiettivo**: CLR avvia un nuovo flusso di prenotazione pre-compilato con il servizio e la poltrona di un appuntamento passato.

### `BookingService.java` — aggiungere metodo:
```java
/**
 * RF_CLR_5 — Riprenotazione rapida.
 * Preleva servizio e poltrona da una prenotazione passata
 * e avvia una nuova richiesta per il nuovo slot.
 */
public BookingResponseDto rebook(Long pastBookingId, LocalDate newDate,
                                  LocalTime newStartTime, User client) {
    Prenotazione past = findOrThrow(pastBookingId);

    // Verifica ownership
    if (past.getClient() == null || !past.getClient().getId().equals(client.getId())) {
        throw new UnauthorizedOperationException("Non autorizzato alla riprenotazione");
    }

    // Crea nuova richiesta con stessa poltrona e servizio
    BookingRequestDto dto = new BookingRequestDto(
        past.getPoltrona().getId(),
        past.getServizio().getId(),
        newDate,
        newStartTime
    );

    return createRequest(dto, client);  // riutilizza il flusso standard
}
```

### Endpoint
```java
@PostMapping("/{id}/rebook")
@PreAuthorize("hasRole('CLIENT')")
public ResponseEntity<BookingResponseDto> rebook(
        @PathVariable Long id,
        @Valid @RequestBody RebookRequestDto dto,
        @AuthenticationPrincipal UserPrincipal principal) {
    return ResponseEntity.status(201).body(
        bookingService.rebook(id, dto.date(), dto.startTime(), principal.getUser()));
}
```

### DTO
```java
public record RebookRequestDto(
    @NotNull LocalDate date,
    @NotNull LocalTime startTime
) {}
```

### Attività
- [ ] Aggiungere metodo `rebook` al `BookingService`
- [ ] Creare endpoint nel `BookingController`
- [ ] Creare `RebookRequestDto`

---

## Fase 8.5 — Registrazione Veloce Post-Prenotazione (RF_CLG_2)

**Obiettivo**: Dopo aver inviato una richiesta come CLG, il sistema offre la possibilità di creare un account riutilizzando i dati già inseriti.

### `AuthService.java` — aggiungere metodo:
```java
/**
 * RF_CLG_2 — Conversione CLG in CLR.
 * Parte dalla prenotazione ospite e crea un account CLR
 * associando retroattivamente la prenotazione al nuovo utente.
 */
public AuthResponseDto registerFromGuest(Long guestBookingId, String password) {
    Prenotazione booking = prenotazioneRepository.findById(guestBookingId)
        .orElseThrow(() -> new ResourceNotFoundException("Prenotazione non trovata"));

    if (booking.getClient() != null) {
        throw new IllegalStateException("La prenotazione è già associata a un cliente registrato");
    }
    if (booking.getGuestData() == null) {
        throw new IllegalStateException("Nessun dato ospite da cui creare account");
    }

    GuestData guest = booking.getGuestData();

    if (userRepository.existsByEmail(guest.getEmail())) {
        // L'email è richiesta — il CLG potrebbe non averla inserita
        // Questo metodo è opzionale e best-effort
        throw new EmailAlreadyExistsException("Email già registrata");
    }

    // Crea il nuovo ClienteRegistrato con i dati del form ospite
    ClienteRegistrato client = ClienteRegistrato.builder()
        .nome(guest.getNome())
        .cognome(guest.getCognome())
        .telefono(guest.getTelefono())
        .passwordHash(passwordEncoder.encode(password))
        .ruolo(UserRole.CLIENT)
        .createdAt(LocalDateTime.now())
        .build();
    userRepository.save(client);

    // Associa retroattivamente la prenotazione al nuovo account
    booking.setClient(client);
    booking.setGuestData(null);   // rimuove i dati ospite
    prenotazioneRepository.save(booking);

    return generateTokenPair(client);
}
```

> **Nota**: RF_CLG_2 è priorità **Bassa**. Se l'implementazione presenta complessità impreviste (es. il CLG non ha inserito email), può essere semplificata o posticipata senza impatto sul core del sistema.

### DTO e Endpoint
```java
public record GuestRegisterRequestDto(
    @NotBlank Long bookingId,
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8) String password
) {}

// Endpoint nel AuthController:
@PostMapping("/auth/guest-register")
public ResponseEntity<AuthResponseDto> guestRegister(
        @Valid @RequestBody GuestRegisterRequestDto dto,
        HttpServletResponse response) {
    AuthResponseDto auth = authService.registerFromGuest(dto.bookingId(), dto.email(), dto.password());
    addRefreshTokenCookie(response, auth.refreshTokenRaw());
    return ResponseEntity.status(201).body(/* auth senza RT raw */);
}
```

### Attività
- [ ] Aggiungere metodo `registerFromGuest` ad `AuthService`
- [ ] Creare endpoint in `AuthController`
- [ ] Verificare l'associazione retroattiva della prenotazione al nuovo account

---

## Fase 8.6 — Unit Test

### `PasswordResetServiceTest.java`
```java
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordResetTokenRepository tokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EmailService emailService;
    @InjectMocks PasswordResetService passwordResetService;

    // --- requestPasswordReset ---
    @Test void requestReset_knownEmail_generatesTokenAndSendsEmail()
    @Test void requestReset_unknownEmail_silentNoOp()
    // SECURITY: stessa risposta per email nota e non nota
    @Test void requestReset_unknownEmail_doesNotCallEmailService()
    @Test void requestReset_invalidatesPreviousTokens()

    // --- resetPassword ---
    @Test void resetPassword_validToken_updatesPassword()
    @Test void resetPassword_expiredToken_throwsInvalidTokenException()
    @Test void resetPassword_usedToken_throwsInvalidTokenException()
    @Test void resetPassword_unknownToken_throwsInvalidTokenException()
    @Test void resetPassword_marksTokenAsUsed()
    @Test void resetPassword_tokenIsMonouso_secondUseFails()
}
```

### `ProfileServiceTest.java`
```java
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @InjectMocks ProfileService profileService;

    @Test void updateProfile_validData_updatesName()
    @Test void updateProfile_newEmail_updatesEmail()
    @Test void updateProfile_duplicateEmail_throwsEmailAlreadyExistsException()
    @Test void updateProfile_nullFields_doesNotOverwrite()
    @Test void updateProfile_invalidPhone_validationFails()  // DTO level
    @Test void updateProfile_invalidEmail_validationFails()  // DTO level
}
```

### `BookingServiceTest.java` — test aggiuntivi per rebook
```java
@Test void rebook_ownPastBooking_createsNewRequestWithSameServiceAndChair()
@Test void rebook_notOwner_throwsUnauthorizedException()
@Test void rebook_bookingNotFound_throwsResourceNotFoundException()
```

### Attività
- [ ] Implementare `PasswordResetServiceTest`
- [ ] Implementare `ProfileServiceTest`
- [ ] Aggiungere test `rebook` a `BookingServiceTest`
- [ ] Coverage `PasswordResetService` e `ProfileService` ≥ 80%

---

## Fase 8.7 — Integration Test

### `AdvancedFeaturesIntegrationTest.java`
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class AdvancedFeaturesIntegrationTest {

    // --- Password Reset ---
    @Test void forgotPassword_knownEmail_returns200()
    @Test void forgotPassword_unknownEmail_returns200()  // identico! security
    @Test void resetPassword_validToken_changesPassword()
    @Test void resetPassword_expiredToken_returns400()
    @Test void resetPassword_reuse_returns400()
    @Test void resetPassword_canLoginWithNewPassword()

    // --- Modifica Profilo ---
    @Test void updateProfile_asClient_returns200()
    @Test void updateProfile_asBarber_returns403()
    @Test void updateProfile_duplicateEmail_returns400()
    @Test void updateProfile_invalidEmail_returns400()
    @Test void updateProfile_changesSavedCorrectly()

    // --- Riprenotazione Rapida ---
    @Test void rebook_ownPastBooking_returns201()
    @Test void rebook_notOwner_returns403()
    @Test void rebook_slotNotAvailable_returns409()

    // --- Guest Register ---
    @Test void guestRegister_validBooking_returns201()
    @Test void guestRegister_bookingAlreadyLinked_returns400()
}
```

### Attività
- [ ] Implementare `AdvancedFeaturesIntegrationTest`
- [ ] Verificare che il token di reset sia correttamente hashato in DB (mai in chiaro)

---

## Fase 8.8 — Verifica Quality Gate

### Checklist finale Sprint 8
- [ ] Password recovery funzionante (token generato, link loggato in dev)
- [ ] Token monouso e con scadenza verificati
- [ ] `forgotPassword` non rivela se email è registrata (risposta identica per email nota e non)
- [ ] Modifica profilo con tutti i campi validati
- [ ] Riprenotazione rapida: stessa poltrona e servizio, nuovo slot
- [ ] Coverage Service layer ≥ 80%
- [ ] CI pipeline verde

---

## Definition of Done — Sprint 8

| Criterio | Verifica |
|----------|----------|
| ✅ RF_GEN_4 Password recovery | Token generato, link inviato (log in dev), reset funzionante |
| ✅ Token monouso | Secondo utilizzo → 400 |
| ✅ Token scadenza 1h | Token scaduto → 400 |
| ✅ Security: email non rivelata | `forgotPassword` sempre 200, indipendentemente da email |
| ✅ RF_CLR_6 Modifica profilo | Nome, cognome, email, telefono aggiornabili con validazione |
| ✅ RF_CLR_5 Riprenotazione rapida | Pre-compila servizio e poltrona dal booking passato |
| ✅ RF_CLG_2 Guest register | Crea account CLR da dati ospite, associa prenotazione |
| ✅ Unit test ≥ 80% | PasswordResetService, ProfileService coperti |
| ✅ CI pipeline verde | GitHub Actions passa |

---

## Note Operative

- In questo sprint **non implementiamo JavaMailSender** per produzione — l'`EmailService` logga il link sulla console. La configurazione SMTP completa è pianificata per Sprint 10 (Security Hardening) se richiesta.
- **RF_CLG_2 (Registrazione Veloce Post-Prenotazione)** ha priorità **Bassa**: se le scadenze di progetto sono strette, può essere demandato a un secondo ciclo di sviluppo senza impatto sul core.
- La **Riprenotazione Rapida** riusa completamente il flusso `createRequest` — questo è il vantaggio del Facade: un singolo punto di ingresso per la creazione di prenotazioni, indipendentemente dall'origine.

---

*Sprint 8 — Ultima modifica: 22/04/2026*
