package com.barberbook.controller;

import com.barberbook.dto.request.UpdateUserRequestDto;
import com.barberbook.dto.response.UserResponseDto;
import com.barberbook.security.UserPrincipal;
import com.barberbook.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller per la gestione delle informazioni degli utenti.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final AuthService authService;

    /**
     * RF_CLR_6 — Recupera le informazioni del profilo dell'utente autenticato.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDto> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(profileService.getProfile(principal.getUser()));
    }

    /**
     * RF_CLR_6 — Aggiorna i dati del profilo personale (solo per clienti registrati).
     */
    @PatchMapping("/me")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<UserResponseDto> updateMe(
            @Valid @RequestBody UpdateProfileRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(profileService.updateProfile(principal.getUser(), dto));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponseDto> updateMe(
            @Valid @RequestBody UpdateUserRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.updateProfile(principal.getId(), dto));
    }
}
