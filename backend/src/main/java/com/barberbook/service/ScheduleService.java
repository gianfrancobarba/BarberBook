package com.barberbook.service;

import com.barberbook.domain.model.FasciaOraria;
import com.barberbook.domain.model.Poltrona;
import com.barberbook.dto.request.CreateScheduleRequestDto;
import com.barberbook.dto.response.ScheduleResponseDto;
import com.barberbook.exception.InvalidTimeRangeException;
import com.barberbook.exception.ResourceNotFoundException;
import com.barberbook.mapper.FasciaOrariaMapper;
import com.barberbook.repository.FasciaOrariaRepository;
import com.barberbook.repository.PoltronaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Service per la gestione delle configurazioni orarie del salone.
 * Permette al barbiere di definire orari di apertura e pause.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {

    private final FasciaOrariaRepository fasciaOrariaRepository;
    private final PoltronaRepository poltronaRepository;
    private final FasciaOrariaMapper fasciaOrariaMapper;

    /**
     * RF_BAR_9 — Ottiene tutte le fasce orarie configurate per una specifica poltrona.
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> getScheduleForChair(Long chairId) {
        Poltrona poltrona = poltronaRepository.findByIdAndAttivaTrue(chairId)
            .orElseThrow(() -> new ResourceNotFoundException("Poltrona non trovata: " + chairId));
        return fasciaOrariaMapper.toDtoList(fasciaOrariaRepository.findByPoltrona(poltrona));
    }

    /**
     * RF_BAR_9/10 — Aggiunge una nuova fascia oraria (apertura o pausa).
     */
    public ScheduleResponseDto addSchedule(CreateScheduleRequestDto dto) {
        Poltrona poltrona = poltronaRepository.findByIdAndAttivaTrue(dto.chairId())
            .orElseThrow(() -> new ResourceNotFoundException("Poltrona non trovata: " + dto.chairId()));
        
        validateTimeRange(dto.oraInizio(), dto.oraFine());

        FasciaOraria fascia = FasciaOraria.builder()
            .poltrona(poltrona)
            .giornoSettimana(dto.giornoSettimana())
            .oraInizio(dto.oraInizio())
            .oraFine(dto.oraFine())
            .tipo(dto.tipo())
            .createdAt(LocalDateTime.now())
            .build();

        return fasciaOrariaMapper.toDto(fasciaOrariaRepository.save(fascia));
    }

    /**
     * RF_BAR_9/10 — Rimuove una fascia oraria esistente.
     */
    public void removeSchedule(Long scheduleId) {
        FasciaOraria fascia = fasciaOrariaRepository.findById(scheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Fascia oraria non trovata: " + scheduleId));
        fasciaOrariaRepository.delete(fascia);
    }

    /**
     * Valida che l'orario di inizio sia precedente a quello di fine.
     */
    private void validateTimeRange(LocalTime start, LocalTime end) {
        if (!start.isBefore(end)) {
            throw new InvalidTimeRangeException("L'orario di inizio deve essere prima di quello di fine");
        }
    }
}
