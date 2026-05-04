package com.barberbook.service;

import com.barberbook.domain.model.Poltrona;
import com.barberbook.domain.model.Prenotazione;
import com.barberbook.dto.response.DailyDashboardResponseDto;
import com.barberbook.dto.response.WeeklyDashboardResponseDto;
import com.barberbook.mapper.BookingMapper;
import com.barberbook.repository.PoltronaRepository;
import com.barberbook.repository.PrenotazioneRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private PrenotazioneRepository prenotazioneRepository;
    @Mock private PoltronaRepository poltronaRepository;
    @Mock private BookingMapper bookingMapper;
    @InjectMocks private DashboardService dashboardService;

    @Test
    @DisplayName("getDailyDashboard: ritorna tutte le poltrone attive")
    void getDailyDashboard_returnsAllActiveChairs() {
        LocalDate date = LocalDate.now();
        Poltrona p1 = new Poltrona(); p1.setId(1L); p1.setNome("P1");
        Poltrona p2 = new Poltrona(); p2.setId(2L); p2.setNome("P2");
        
        when(poltronaRepository.findByAttivaTrue()).thenReturn(List.of(p1, p2));
        when(prenotazioneRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(Collections.emptyList());

        DailyDashboardResponseDto response = dashboardService.getDailyDashboard(date);

        assertEquals(2, response.chairs().size());
        assertEquals("P1", response.chairs().get(0).chairName());
    }

    @Test
    @DisplayName("getWeeklyDashboard: ritorna esattamente 7 giorni, anche se vuoti")
    void getWeeklyDashboard_returns7Days() {
        LocalDate start = LocalDate.now();
        when(poltronaRepository.findByAttivaTrue()).thenReturn(Collections.emptyList());
        when(prenotazioneRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(Collections.emptyList());

        WeeklyDashboardResponseDto response = dashboardService.getWeeklyDashboard(start);

        assertEquals(7, response.days().size());
        assertEquals(start, response.weekStart());
        assertEquals(start.plusDays(6), response.weekEnd());
    }
}
