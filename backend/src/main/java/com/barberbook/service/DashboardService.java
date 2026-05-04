package com.barberbook.service;

import com.barberbook.domain.model.Poltrona;
import com.barberbook.domain.model.Prenotazione;
import com.barberbook.dto.response.*;
import com.barberbook.mapper.BookingMapper;
import com.barberbook.repository.PoltronaRepository;
import com.barberbook.repository.PrenotazioneRepository;
import com.barberbook.repository.specification.BookingSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Service per la gestione delle dashboard amministrative del barbiere.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final PrenotazioneRepository prenotazioneRepository;
    private final PoltronaRepository poltronaRepository;
    private final BookingMapper bookingMapper;

    /**
     * RF_BAR_2 — Dashboard Giornaliera
     * Ritorna tutte le prenotazioni del giorno raggruppate per poltrona.
     */
    public DailyDashboardResponseDto getDailyDashboard(LocalDate date) {
        List<Poltrona> activeChairs = poltronaRepository.findByAttivaTrue();

        List<ChairDayScheduleDto> schedules = activeChairs.stream().map(chair -> {
            Specification<Prenotazione> spec = BookingSpecifications.byDate(date)
                .and(BookingSpecifications.byChair(chair.getId()))
                .and(BookingSpecifications.notCancelledOrRejected());

            List<Prenotazione> bookings = prenotazioneRepository.findAll(spec, Sort.by("startTime").ascending());

            return new ChairDayScheduleDto(
                chair.getId(),
                chair.getNome(),
                date,
                bookingMapper.toDtoList(bookings)
            );
        }).collect(Collectors.toList());

        return new DailyDashboardResponseDto(date, schedules);
    }

    /**
     * RF_BAR_1 — Dashboard Settimanale
     * Ritorna l'agenda per 7 giorni a partire da weekStart.
     * Utilizza un'unica query per caricare tutte le prenotazioni della settimana.
     */
    public WeeklyDashboardResponseDto getWeeklyDashboard(LocalDate weekStart) {
        List<Poltrona> activeChairs = poltronaRepository.findByAttivaTrue();
        LocalDate weekEnd = weekStart.plusDays(6);

        // Ottimizzazione: caricamento bulk della settimana per evitare N+1 query
        Specification<Prenotazione> spec = BookingSpecifications.byWeek(weekStart)
            .and(BookingSpecifications.notCancelledOrRejected());

        List<Prenotazione> weekBookings = prenotazioneRepository.findAll(spec, Sort.by("startTime").ascending());

        List<DayScheduleDto> days = IntStream.rangeClosed(0, 6)
            .mapToObj(weekStart::plusDays)
            .map(day -> {
                String dayName = day.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ITALIAN);
                // Capitalizza la prima lettera del giorno
                dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);

                List<ChairDayScheduleDto> chairSchedules = activeChairs.stream().map(chair -> {
                    List<Prenotazione> dayChairBookings = weekBookings.stream()
                        .filter(b -> b.getStartTime().toLocalDate().equals(day))
                        .filter(b -> b.getPoltrona().getId().equals(chair.getId()))
                        .collect(Collectors.toList());

                    return new ChairDayScheduleDto(
                        chair.getId(),
                        chair.getNome(),
                        day,
                        bookingMapper.toDtoList(dayChairBookings)
                    );
                }).collect(Collectors.toList());

                return new DayScheduleDto(day, dayName, chairSchedules);
            })
            .collect(Collectors.toList());

        return new WeeklyDashboardResponseDto(weekStart, weekEnd, days);
    }
}
