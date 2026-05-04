package com.barberbook.controller;

import com.barberbook.dto.request.UpdateProfileRequestDto;
import com.barberbook.dto.response.UserResponseDto;
import com.barberbook.security.UserPrincipal;
import com.barberbook.service.ProfileService;
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

    private final ProfileService profileService;

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
}
