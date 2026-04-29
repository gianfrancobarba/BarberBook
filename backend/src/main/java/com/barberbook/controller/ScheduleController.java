package com.barberbook.controller;

import com.barberbook.dto.request.CreateScheduleRequestDto;
import com.barberbook.dto.response.ScheduleResponseDto;
import com.barberbook.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@PreAuthorize("hasRole('BARBER')")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /** RF_BAR_9 — Ottieni tutti gli orari per una poltrona */
    @GetMapping("/chairs/{chairId}")
    public ResponseEntity<List<ScheduleResponseDto>> getByChair(@PathVariable Long chairId) {
        return ResponseEntity.ok(scheduleService.getScheduleForChair(chairId));
    }

    /** RF_BAR_9/10 — Aggiungi una fascia oraria o pausa */
    @PostMapping
    public ResponseEntity<ScheduleResponseDto> add(
            @Valid @RequestBody CreateScheduleRequestDto dto) {
        return ResponseEntity.status(201).body(scheduleService.addSchedule(dto));
    }

    /** RF_BAR_9/10 — Rimuovi una fascia oraria */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        scheduleService.removeSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
