# Sprint 1 — Autenticazione & Gestione Utenti
> **Stato**: ⬜ Non iniziato  
> **Dipende da**: Sprint 0 ✅  
> **Obiettivo**: Implementare il sistema di identità completo. Dopo questo sprint ogni attore ha la propria identità verificata e può autenticarsi in sicurezza con JWT + Refresh Token Rotation.

---

## Requisiti Funzionali Coperti

| RF | Nome | Attore | Priorità |
|----|------|--------|----------|
| RF_GEN_1 | Registrazione Account | CLR | Alta |
| RF_GEN_2 | Login | BAR, CLR | Alta |
| RF_GEN_3 | Logout | BAR, CLR | Alta |

---

## Indice Fasi

1. [Fase 1.1 — Modello di Dominio Utenti](#fase-11--modello-di-dominio-utenti)
2. [Fase 1.2 — Migrazioni Flyway](#fase-12--migrazioni-flyway)
3. [Fase 1.3 — Spring Security & JWT](#fase-13--spring-security--jwt)
4. [Fase 1.4 — AuthService (logica di business)](#fase-14--authservice-logica-di-business)
5. [Fase 1.5 — REST Controllers Auth](#fase-15--rest-controllers-auth)
6. [Fase 1.6 — Unit Test](#fase-16--unit-test)
7. [Fase 1.7 — Integration Test](#fase-17--integration-test)
8. [Fase 1.8 — Verifica Quality Gate](#fase-18--verifica-quality-gate)

---

## Fase 1.1 — Modello di Dominio Utenti

**Obiettivo**: Definire le entità JPA che rappresentano gli attori del sistema con la corretta gerarchia di ereditarietà.

### Entità da creare

#### `UserRole.java` (enum)
```java
public enum UserRole {
    BARBER,
    CLIENT
}
```

#### `User.java` (superclasse astratta — JPA JOINED strategy)
```java
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "ruolo", discriminatorType = DiscriminatorType.STRING)
public abstract class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String cognome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, insertable = false, updatable = false)
    private UserRole ruolo;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    // Getter, Setter via Lombok @Data/@Getter/@Setter
}
```

#### `Barbiere.java` (specializzazione)
```java
@Entity
@Table(name = "barbers")
@DiscriminatorValue("BARBER")
public class Barbiere extends User {
    // Nessun campo aggiuntivo per ora
    // L'account è pre-configurato via migrazione Flyway
}
```

#### `ClienteRegistrato.java` (specializzazione)
```java
@Entity
@Table(name = "clients")
@DiscriminatorValue("CLIENT")
public class ClienteRegistrato extends User {
    @Column(nullable = false)
    private String passwordHash;   // BCrypt hash

    @Column
    private LocalDateTime emailVerifiedAt;  // futuro: verifica email
}
```

#### `RefreshToken.java`
```java
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tokenHash;      // SHA-256 del token originale

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
```

### DTO da creare

```java
// Request
public record RegisterRequestDto(
    @NotBlank String nome,
    @NotBlank String cognome,
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8) String password,
    String telefono
) {}

public record LoginRequestDto(
    @Email @NotBlank String email,
    @NotBlank String password
) {}

public record RefreshTokenRequestDto(
    // Il Refresh Token arriva dal cookie HttpOnly — non nel body
) {}

// Response
public record AuthResponseDto(
    String accessToken,
    String tokenType,  // "Bearer"
    long expiresIn     // secondi
    // Il Refresh Token è nel cookie HttpOnly — non nel body
) {}

public record UserResponseDto(
    Long id,
    String nome,
    String cognome,
    String email,
    String telefono,
    UserRole ruolo
) {}
```

### Mapper
```java
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponseDto toDto(User user);
    ClienteRegistrato toEntity(RegisterRequestDto dto);
}
```

### Attività
- [ ] Creare enum `UserRole`
- [ ] Creare classe astratta `User` con JPA JOINED inheritance
- [ ] Creare `Barbiere extends User`
- [ ] Creare `ClienteRegistrato extends User` con `passwordHash`
- [ ] Creare `RefreshToken` entity
- [ ] Creare tutti i DTO (Request e Response)
- [ ] Creare `UserMapper` con MapStruct
- [ ] Creare `UserRepository`, `RefreshTokenRepository`

---

## Fase 1.2 — Migrazioni Flyway

**Obiettivo**: Schema DB completo per la gestione utenti e token, con seed dell'account BAR.

### `V2__auth_schema.sql`
```sql
-- Tabella barbers (specializzazione di users)
CREATE TABLE barbers (
    id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE
);

-- Tabella clients (specializzazione di users)
CREATE TABLE clients (
    id             BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    password_hash  VARCHAR(255) NOT NULL,
    email_verified_at TIMESTAMP
);

-- Tabella refresh_tokens
CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at  TIMESTAMP NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);
```

### `V3__seed_barber.sql`
```sql
-- Account BAR pre-configurato (non si registra autonomamente)
-- Password: 'admin1234' hashata con BCrypt strength 12
-- IMPORTANTE: cambiare in produzione
INSERT INTO users (nome, cognome, email, telefono, ruolo, created_at)
VALUES ('Tony', 'Hairman', 'tony@hairmanbarber.it', '3331234567', 'BARBER', NOW());

INSERT INTO barbers (id)
SELECT id FROM users WHERE email = 'tony@hairmanbarber.it';
```

> **Nota**: L'hash BCrypt dell'account BAR verrà generato e inserito nella migrazione. La password iniziale sarà documentata in `.env.example` e **deve essere cambiata prima del deploy in produzione**.

### Attività
- [ ] Creare `V2__auth_schema.sql` — tabelle barbers, clients, refresh_tokens
- [ ] Creare `V3__seed_barber.sql` — account BAR pre-inserito
- [ ] Applicare migrazioni: `mvn flyway:migrate` o avvio Spring Boot
- [ ] Verificare in psql: tabelle create, indici creati, seed inserito

---

## Fase 1.3 — Spring Security & JWT

**Obiettivo**: Configurare Spring Security con filtro JWT e gestione ruoli RBAC.

### File da creare in `security/`

#### `JwtUtil.java`
```java
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private static final long ACCESS_TOKEN_EXPIRY = 15 * 60 * 1000; // 15 min in ms

    public String generateAccessToken(User user) {
        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("role", user.getRuolo().name())
            .claim("email", user.getEmail())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY))
            .signWith(getSigningKey())
            .compact();
    }

    public Claims validateAndExtract(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
```

#### `JwtAuthFilter.java`
```java
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String token = header.substring(7);
            Claims claims = jwtUtil.validateAndExtract(token);
            Long userId = Long.parseLong(claims.getSubject());

            UserDetails userDetails = userDetailsService.loadUserById(userId);
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token non valido o scaduto");
            return;
        }

        chain.doFilter(request, response);
    }
}
```

#### `SecurityConfig.java`
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/services", "/api/chairs").permitAll()
                .requestMatchers("/api/health", "/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
```

#### `UserDetailsServiceImpl.java`
```java
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + id));
        return new UserPrincipal(user);
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + email));
        return new UserPrincipal(user);
    }
}
```

### Attività
- [ ] Creare `JwtUtil` con generazione e validazione token
- [ ] Creare `JwtAuthFilter` (OncePerRequestFilter)
- [ ] Creare `SecurityConfig` con regole di autorizzazione
- [ ] Creare `UserDetailsServiceImpl`
- [ ] Creare `UserPrincipal` (wrapper Spring Security)
- [ ] Configurare `jwt.secret` in `application.yml` (da env var)

---

## Fase 1.4 — AuthService (logica di business)

**Obiettivo**: Implementare la logica di registrazione, login, refresh e logout con Refresh Token Rotation.

### `AuthService.java`
```java
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    private static final Duration REFRESH_TOKEN_EXPIRY = Duration.ofDays(7);

    /** RF_GEN_1 — Registrazione CLR */
    public AuthResponseDto register(RegisterRequestDto dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new EmailAlreadyExistsException("Email già registrata: " + dto.email());
        }
        ClienteRegistrato client = userMapper.toEntity(dto);
        client.setPasswordHash(passwordEncoder.encode(dto.password()));
        client.setCreatedAt(LocalDateTime.now());
        ClienteRegistrato saved = userRepository.save(client);
        return generateTokenPair(saved);
    }

    /** RF_GEN_2 — Login BAR e CLR */
    public AuthResponseDto login(LoginRequestDto dto) {
        User user = userRepository.findByEmail(dto.email())
            .orElseThrow(() -> new InvalidCredentialsException("Credenziali non valide"));

        String storedHash = extractPasswordHash(user);
        if (!passwordEncoder.matches(dto.password(), storedHash)) {
            throw new InvalidCredentialsException("Credenziali non valide");
        }

        return generateTokenPair(user);
    }

    /** Rinnovo coppia token con Refresh Token Rotation */
    public AuthResponseDto refresh(String refreshTokenRaw) {
        String tokenHash = hashToken(refreshTokenRaw);
        RefreshToken rt = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new InvalidTokenException("Refresh token non valido"));

        if (rt.isRevoked()) {
            // Segnale di riutilizzo token → invalida tutta la sessione utente
            refreshTokenRepository.revokeAllByUserId(rt.getUser().getId());
            throw new TokenReuseDetectedException("Sessione invalidata per sicurezza");
        }

        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Refresh token scaduto");
        }

        // Rotazione: invalida il vecchio e genera nuova coppia
        rt.setRevoked(true);
        refreshTokenRepository.save(rt);
        return generateTokenPair(rt.getUser());
    }

    /** RF_GEN_3 — Logout */
    public void logout(String refreshTokenRaw) {
        String tokenHash = hashToken(refreshTokenRaw);
        refreshTokenRepository.findByTokenHash(tokenHash)
            .ifPresent(rt -> {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
            });
    }

    // --- Metodi privati ---

    private AuthResponseDto generateTokenPair(User user) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshTokenRaw = generateSecureToken();
        String refreshTokenHash = hashToken(refreshTokenRaw);

        RefreshToken rt = RefreshToken.builder()
            .tokenHash(refreshTokenHash)
            .user(user)
            .expiresAt(LocalDateTime.now().plus(REFRESH_TOKEN_EXPIRY))
            .revoked(false)
            .createdAt(LocalDateTime.now())
            .build();
        refreshTokenRepository.save(rt);

        // Il refreshTokenRaw viene messo nel cookie HttpOnly dal controller
        return new AuthResponseDto(accessToken, "Bearer", 900, refreshTokenRaw);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String raw) {
        // SHA-256 del token — mai salvato in chiaro
        return DigestUtils.sha256Hex(raw);
    }

    private String extractPasswordHash(User user) {
        if (user instanceof ClienteRegistrato client) return client.getPasswordHash();
        if (user instanceof Barbiere) {
            // Il BAR ha la password nel seed — logica separata
            throw new UnsupportedOperationException("BAR usa auth separata");
        }
        throw new IllegalStateException("Tipo utente sconosciuto");
    }
}
```

> **Nota**: La gestione della password del BAR è leggermente diversa perché il suo account è pre-configurato. Verrà chiarita nell'implementazione effettiva in base alla scelta di ereditarietà.

### Eccezioni custom da creare
```
exception/
├── EmailAlreadyExistsException.java        (400)
├── InvalidCredentialsException.java        (401)
├── InvalidTokenException.java              (401)
├── TokenReuseDetectedException.java        (401)
└── GlobalExceptionHandler.java             (@RestControllerAdvice)
```

### Attività
- [ ] Creare `AuthService` con tutti i metodi
- [ ] Creare tutte le eccezioni custom
- [ ] Creare `GlobalExceptionHandler` con `@RestControllerAdvice`
- [ ] Creare `RefreshTokenRepository` con query custom (`findByTokenHash`, `revokeAllByUserId`)

---

## Fase 1.5 — REST Controllers Auth

**Obiettivo**: Esporre gli endpoint di autenticazione. Il Refresh Token viene gestito via cookie HttpOnly.

### `AuthController.java`
```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** RF_GEN_1 */
    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(
            @Valid @RequestBody RegisterRequestDto dto,
            HttpServletResponse response) {
        AuthResponseDto auth = authService.register(dto);
        addRefreshTokenCookie(response, auth.refreshTokenRaw());
        return ResponseEntity.status(201).body(/* user data */);
    }

    /** RF_GEN_2 */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody LoginRequestDto dto,
            HttpServletResponse response) {
        AuthResponseDto auth = authService.login(dto);
        addRefreshTokenCookie(response, auth.refreshTokenRaw());
        // Non restituire il refreshToken nel body — solo l'accessToken
        return ResponseEntity.ok(new AuthResponseDto(auth.accessToken(), auth.tokenType(), auth.expiresIn(), null));
    }

    /** Refresh Token Rotation */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }
        AuthResponseDto auth = authService.refresh(refreshToken);
        addRefreshTokenCookie(response, auth.refreshTokenRaw());
        return ResponseEntity.ok(new AuthResponseDto(auth.accessToken(), auth.tokenType(), auth.expiresIn(), null));
    }

    /** RF_GEN_3 */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok().build();
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", token)
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .maxAge(Duration.ofDays(7))
            .path("/api/auth")
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(true)
            .maxAge(0)
            .path("/api/auth")
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
```

### API Endpoints Riepilogo
| Metodo | Path | Auth | Body Request | Response |
|--------|------|------|-------------|----------|
| POST | `/api/auth/register` | Pubblico | `RegisterRequestDto` | `201 UserResponseDto` |
| POST | `/api/auth/login` | Pubblico | `LoginRequestDto` | `200 AuthResponseDto` (AT solo) + cookie RT |
| POST | `/api/auth/refresh` | Cookie RT | — | `200 AuthResponseDto` (AT solo) + nuovo cookie RT |
| POST | `/api/auth/logout` | Cookie RT | — | `200` + cookie RT svuotato |
| GET | `/api/users/me` | Bearer AT | — | `200 UserResponseDto` |

### Attività
- [ ] Creare `AuthController` con gestione cookie HttpOnly
- [ ] Creare `UserController` con `GET /api/users/me`
- [ ] Verificare che il cookie venga impostato correttamente (HttpOnly, Secure, SameSite=Strict)

---

## Fase 1.6 — Unit Test

**Obiettivo**: Coprire con test unitari tutta la logica di `AuthService` in isolamento (Mockito).

### Test da implementare — `AuthServiceTest.java`
```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock UserMapper userMapper;
    @InjectMocks AuthService authService;

    // --- Registrazione ---
    @Test void register_validData_returnsTokenPair()
    @Test void register_duplicateEmail_throwsEmailAlreadyExistsException()
    @Test void register_emptyName_throwsValidationException()  // validato dal DTO

    // --- Login ---
    @Test void login_validCredentials_returnsTokenPair()
    @Test void login_wrongPassword_throwsInvalidCredentialsException()
    @Test void login_unknownEmail_throwsInvalidCredentialsException()
    @Test void login_doesNotRevealWhetherEmailExists()  // stesso messaggio errore

    // --- Refresh Token ---
    @Test void refresh_validToken_returnsNewPairAndRevokesOld()
    @Test void refresh_expiredToken_throwsInvalidTokenException()
    @Test void refresh_revokedToken_revokesAllSessionAndThrows()  // segnale di furto
    @Test void refresh_unknownToken_throwsInvalidTokenException()

    // --- Logout ---
    @Test void logout_validToken_revokesToken()
    @Test void logout_unknownToken_noException()  // idempotente
}
```

### Test da implementare — `JwtUtilTest.java`
```java
class JwtUtilTest {
    // Setup: JwtUtil con secret di test

    @Test void generateToken_validUser_returnsValidJwt()
    @Test void validateToken_validJwt_extractsCorrectUserId()
    @Test void validateToken_expiredJwt_throwsJwtException()
    @Test void validateToken_tamperedJwt_throwsJwtException()
    @Test void validateToken_wrongSecret_throwsJwtException()
}
```

### Test da implementare — `BookingStatusTransition_Sprint1` (preparatorio)
> Non applicabile a Sprint 1, i test della state machine iniziano in Sprint 5.

### Attività
- [ ] Implementare tutti i test `AuthServiceTest`
- [ ] Implementare tutti i test `JwtUtilTest`
- [ ] Verificare coverage `AuthService` ≥ 80%

---

## Fase 1.7 — Integration Test

**Obiettivo**: Testare i flussi completi attraverso il layer HTTP con database reale (Testcontainers).

### `AuthIntegrationTest.java`
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // --- Scenari di test end-to-end ---

    @Test void registerThenLogin_fullFlow_success()
    @Test void login_barberAccount_returnsBarberRole()
    @Test void accessProtectedEndpoint_withValidToken_returns200()
    @Test void accessProtectedEndpoint_withoutToken_returns401()
    @Test void accessBarberEndpoint_withClientToken_returns403()
    @Test void refreshFlow_fullRotation_oldTokenInvalidated()
    @Test void logout_tokenInvalidated_refreshReturns401()
    @Test void register_duplicateEmail_returns400()
}
```

### Attività
- [ ] Implementare `AuthIntegrationTest` con Testcontainers
- [ ] Verificare che il seed BAR (da Flyway V3) sia presente nel DB di test
- [ ] Verificare che pipeline GitHub Actions esegua test di integrazione

---

## Fase 1.8 — Verifica Quality Gate

**Obiettivo**: Tutti i quality gate sono soddisfatti prima di chiudere Sprint 1.

### Checklist
- [x] `mvn verify` → BUILD SUCCESS, tutti i test passano
- [x] JaCoCo: `AuthService` coverage LINE ≥ 80%, BRANCH ≥ 75%
- [ ] SonarCloud: 0 Bug Critical/Major, 0 vulnerabilità
- [ ] Snyk: 0 dipendenze con vulnerabilità HIGH/CRITICAL
- [ ] Push su `develop` → GitHub Actions verde
- [ ] PR review: nessuna Entity JPA esposta via API (solo DTO)

---

## Definition of Done — Sprint 1

| Criterio | Verifica |
|----------|----------|
| ✅ RF_GEN_1 Registrazione CLR | `POST /api/auth/register` crea account e ritorna token |
| ✅ RF_GEN_2 Login BAR+CLR | `POST /api/auth/login` con credenziali valide ritorna AT + cookie RT |
| ✅ RF_GEN_3 Logout | `POST /api/auth/logout` invalida RT, svuota cookie |
| ✅ Refresh Token Rotation | Vecchio RT invalidato, nuova coppia generata |
| ✅ Rilevamento furto token | RT già revocato → invalida tutta la sessione |
| ✅ RT in cookie HttpOnly | Mai esposto nel response body dopo il primo invio |
| ✅ BCrypt strength 12 | Configurato in `SecurityConfig`, ~300ms/hash |
| ✅ RBAC funzionante | BARBER-only endpoint inaccessibile con token CLIENT |
| ✅ Account BAR pre-configurato | Creato via migrazione Flyway, non via registrazione |
| ✅ Unit test coverage ≥ 80% | AuthService, JwtUtil coperti |
| ✅ Integration test verdi | Flusso completo con DB reale (Testcontainers) |
| ✅ CI pipeline verde | GitHub Actions passa su develop |

---

## Note Operative

- Il **Refresh Token** è una stringa casuale di 64 byte (Base64 URL-encoded). Solo il suo hash SHA-256 è salvato in DB.
- Il **BAR non si registra**: il suo account è nel seed Flyway. La password iniziale deve essere hashata offline e inserita in `V3__seed_barber.sql`.
- Il **GlobalExceptionHandler** deve essere implementato e testato per garantire che le eccezioni restituiscano sempre JSON strutturato (non stack trace raw).
- Il campo `emailVerifiedAt` nei clienti è previsto per Sprint 8 (recupero password). Per ora è nullable e ignorato.

---

*Sprint 1 — Ultima modifica: 22/04/2026*
