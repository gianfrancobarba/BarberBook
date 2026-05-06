package com.barberbook.controller;

import com.barberbook.dto.request.UpdateUserRequestDto;
import com.barberbook.dto.response.UserResponseDto;
import com.barberbook.mapper.UserMapper;
import com.barberbook.security.UserPrincipal;
import com.barberbook.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userMapper.toDto(principal.getUser()));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponseDto> updateMe(
            @Valid @RequestBody UpdateUserRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.updateProfile(principal.getId(), dto));
    }
}
