package com.barberbook.controller;

import com.barberbook.dto.response.AvailabilityResponseDto;
import com.barberbook.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    /** RF_CLI_3 + RF_CLI_4 — Slot disponibili per un giorno e servizio */
    @GetMapping
    public ResponseEntity<List<AvailabilityResponseDto>> getAvailableSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Long serviceId) {
        return ResponseEntity.ok(availabilityService.getAvailableSlots(date, serviceId));
    }
}
