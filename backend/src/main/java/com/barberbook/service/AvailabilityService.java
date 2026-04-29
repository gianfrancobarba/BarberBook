package com.barberbook.service;

import com.barberbook.domain.enums.ScheduleType;
import com.barberbook.domain.model.*;
import com.barberbook.dto.response.AvailabilityResponseDto;
import com.barberbook.dto.response.TimeSlotDto;
import com.barberbook.exception.ResourceNotFoundException;
import com.barberbook.exception.SlotNotAvailableException;
import com.barberbook.repository.*;
import com.barberbook.service.strategy.AvailabilityStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AvailabilityService {

    private final AvailabilityStrategy availabilityStrategy;
    private final PoltronaRepository poltronaRepository;
    private final FasciaOrariaRepository fasciaOrariaRepository;
    private final ServizioRepository servizioRepository;
    private final PrenotazioneRepository prenotazioneRepository;

    public AvailabilityService(AvailabilityStrategy availabilityStrategy,
                               PoltronaRepository poltronaRepository,
                               FasciaOrariaRepository fasciaOrariaRepository,
                               ServizioRepository servizioRepository,
                               PrenotazioneRepository prenotazioneRepository) {
        this.availabilityStrategy = availabilityStrategy;
        this.poltronaRepository = poltronaRepository;
        this.fasciaOrariaRepository = fasciaOrariaRepository;
        this.servizioRepository = servizioRepository;
        this.prenotazioneRepository = prenotazioneRepository;
    }

    public List<AvailabilityResponseDto> getAvailableSlots(LocalDate date, Long serviceId) {
        Servizio servizio = servizioRepository.findByIdAndAttivoTrue(serviceId)
            .orElseThrow(() -> new ResourceNotFoundException("Servizio non trovato: " + serviceId));
        Duration duration = Duration.ofMinutes(servizio.getDurataMinuti());

        List<Poltrona> poltrone = poltronaRepository.findByAttivaTrue();
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        return poltrone.stream().map(poltrona -> {
            FasciaOraria apertura = fasciaOrariaRepository
                .findByPoltronaAndGiornoSettimanaAndTipo(poltrona, dayOfWeek, ScheduleType.APERTURA)
                .orElse(null);

            List<FasciaOraria> pause = fasciaOrariaRepository
                .findByPoltronaAndGiornoSettimanaAndTipo(poltrona, dayOfWeek, ScheduleType.PAUSA);

            List<Prenotazione> prenotazioni = prenotazioneRepository
                .findActiveBookingsByChairAndDate(poltrona.getId(), date);

            AvailabilityContext context = new AvailabilityContext(apertura, pause, prenotazioni);

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

    public void assertSlotIsAvailable(Long chairId, LocalDateTime startTime, LocalDateTime endTime) {
        boolean hasOverlap = prenotazioneRepository
            .existsActiveBookingInSlot(chairId, startTime, endTime);
        if (hasOverlap) {
            throw new SlotNotAvailableException("Lo slot selezionato non è più disponibile");
        }
    }
}
