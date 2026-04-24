package com.barberbook.controller;

import com.barberbook.dto.request.LoginRequestDto;
import com.barberbook.dto.request.RegisterRequestDto;
import com.barberbook.dto.response.AuthResponseDto;
import com.barberbook.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(
            @Valid @RequestBody RegisterRequestDto dto,
            HttpServletResponse response) {
        AuthResponseDto auth = authService.register(dto);
        addRefreshTokenCookie(response, auth.refreshTokenRaw());
        return ResponseEntity.status(201).body(new AuthResponseDto(
                auth.accessToken(), auth.tokenType(), auth.expiresIn(), null, auth.user()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody LoginRequestDto dto,
            HttpServletResponse response) {
        AuthResponseDto auth = authService.login(dto);
        addRefreshTokenCookie(response, auth.refreshTokenRaw());
        return ResponseEntity.ok(new AuthResponseDto(
                auth.accessToken(), auth.tokenType(), auth.expiresIn(), null, auth.user()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }
        AuthResponseDto auth = authService.refresh(refreshToken);
        addRefreshTokenCookie(response, auth.refreshTokenRaw());
        return ResponseEntity.ok(new AuthResponseDto(
                auth.accessToken(), auth.tokenType(), auth.expiresIn(), null, auth.user()));
    }

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
