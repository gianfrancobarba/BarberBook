package com.barberbook.service;

import com.barberbook.domain.enums.ScheduleType;
import com.barberbook.domain.model.*;
import com.barberbook.dto.response.AvailabilityResponseDto;
import com.barberbook.dto.response.TimeSlotDto;
import com.barberbook.exception.ResourceNotFoundException;
import com.barberbook.exception.SlotNotAvailableException;
import com.barberbook.repository.*;
import com.barberbook.service.strategy.AvailabilityStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestratore per il calcolo della disponibilità del salone.
 * Coordina il recupero dei dati dai repository e delega il calcolo alla strategy iniettata.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityService {

    private final AvailabilityStrategy availabilityStrategy;
    private final PoltronaRepository poltronaRepository;
    private final FasciaOrariaRepository fasciaOrariaRepository;
    private final ServizioRepository servizioRepository;
    private final PrenotazioneRepository prenotazioneRepository;

    /**
     * RF_CLI_3 + RF_CLI_4 — Calcola gli slot disponibili per tutte le poltrone attive in un dato giorno.
     *
     * @param date      il giorno richiesto dal cliente
     * @param serviceId il servizio selezionato (necessario per la durata)
     * @return lista di oggetti AvailabilityResponseDto, uno per ogni poltrona attiva
     */
    public List<AvailabilityResponseDto> getAvailableSlots(LocalDate date, Long serviceId) {
        // 1. Recupera il servizio (con la durata)
        Servizio servizio = servizioRepository.findByIdAndAttivoTrue(serviceId)
            .orElseThrow(() -> new ResourceNotFoundException("Servizio non trovato: " + serviceId));
        Duration duration = Duration.ofMinutes(servizio.getDurataMinuti());

        // 2. Recupera tutte le poltrone attive
        List<Poltrona> poltrone = poltronaRepository.findByAttivaTrue();

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        // 3. Per ogni poltrona, calcola gli slot disponibili
        return poltrone.stream().map(poltrona -> {
            // Fascia di APERTURA per questa poltrona in questo giorno
            FasciaOraria apertura = fasciaOrariaRepository
                .findByPoltronaAndGiornoSettimanaAndTipo(poltrona, dayOfWeek, ScheduleType.APERTURA)
                .orElse(null);  // null = giorno chiuso

            // Fasce di PAUSA per questa poltrona in questo giorno
            List<FasciaOraria> pause = fasciaOrariaRepository
                .findByPoltronaAndGiornoSettimanaAndTipo(poltrona, dayOfWeek, ScheduleType.PAUSA);

            // Prenotazioni esistenti (IN_ATTESA + ACCETTATA)
            List<Prenotazione> prenotazioni = prenotazioneRepository
                .findActiveBookingsByChairAndDate(poltrona.getId(), date);

            AvailabilityContext context = new AvailabilityContext(apertura, pause, prenotazioni);

            // Delega alla strategy il calcolo algoritmico
            List<TimeSlot> slots = availabilityStrategy.calculateAvailableSlots(
                date, duration, context);

            return new AvailabilityResponseDto(
                poltrona.getId(),
                poltrona.getNome(),
                slots.stream()
                    .map(s -> new TimeSlotDto(
                        s.start().toString(),
                        s.end().toString()
                    ))
                    .collect(Collectors.toList())
            );
        }).collect(Collectors.toList());
    }

    /**
     * Verifica se uno specifico slot è disponibile per una poltrona.
     * Usato per la validazione pre-prenotazione (Sprint 5).
     */
    public void assertSlotIsAvailable(Long chairId, LocalDateTime startTime, LocalDateTime endTime) {
        boolean hasOverlap = prenotazioneRepository
            .existsActiveBookingInSlot(chairId, startTime, endTime);
        if (hasOverlap) {
            throw new SlotNotAvailableException("Lo slot selezionato non è più disponibile");
        }
    }
}
