package com.barberbook.controller;

import com.barberbook.dto.request.CreateChairRequestDto;
import com.barberbook.dto.request.UpdateChairRequestDto;
import com.barberbook.dto.response.ChairResponseDto;
import com.barberbook.service.ChairService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chairs")
@RequiredArgsConstructor
public class ChairController {

    private final ChairService chairService;

    // -------------------------------------------------------
    // PUBBLICI
    // -------------------------------------------------------

    /** RF_CLI_2 — Lista poltrone attive */
    @GetMapping
    public ResponseEntity<List<ChairResponseDto>> getAll() {
        return ResponseEntity.ok(chairService.getAllActive());
    }

    /** RF_CLI_2 — Dettaglio singola poltrona */
    @GetMapping("/{id}")
    public ResponseEntity<ChairResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(chairService.getById(id));
    }

    // -------------------------------------------------------
    // RISERVATI BAR
    // -------------------------------------------------------

    /** RF_BAR_3 — Aggiunge nuova poltrona */
    @PostMapping
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<ChairResponseDto> create(
            @Valid @RequestBody CreateChairRequestDto dto) {
        return ResponseEntity.status(201).body(chairService.create(dto));
    }

    /** RF_BAR_5 — Rinomina poltrona esistente */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<ChairResponseDto> rename(
            @PathVariable Long id,
            @Valid @RequestBody UpdateChairRequestDto dto) {
        return ResponseEntity.ok(chairService.rename(id, dto));
    }

    /** RF_BAR_4 — Disattiva poltrona (soft-delete) */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        chairService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
