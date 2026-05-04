package com.barberbook.controller;

import com.barberbook.dto.response.DailyDashboardResponseDto;
import com.barberbook.dto.response.WeeklyDashboardResponseDto;
import com.barberbook.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Controller per le dashboard operative del barbiere (BAR).
 */
@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('BARBER')")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * RF_BAR_2 — Agenda giornaliera raggruppata per poltrona.
     */
    @GetMapping("/daily")
    public ResponseEntity<DailyDashboardResponseDto> getDailyDashboard(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(dashboardService.getDailyDashboard(targetDate));
    }

    /**
     * RF_BAR_1 — Agenda settimanale con vista 7 giorni.
     */
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
