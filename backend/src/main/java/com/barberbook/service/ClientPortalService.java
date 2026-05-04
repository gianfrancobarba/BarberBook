package com.barberbook.service;

import com.barberbook.domain.enums.BookingStatus;
import com.barberbook.domain.model.Prenotazione;
import com.barberbook.domain.model.User;
import com.barberbook.dto.response.BookingResponseDto;
import com.barberbook.mapper.BookingMapper;
import com.barberbook.repository.PrenotazioneRepository;
import com.barberbook.repository.specification.BookingSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service per la gestione del portale personale del cliente (CLR).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientPortalService {

    private final PrenotazioneRepository prenotazioneRepository;
    private final BookingMapper bookingMapper;

    /**
     * RF_CLR_1 — Homepage: recupera i prossimi appuntamenti confermati.
     */
    public List<BookingResponseDto> getUpcomingBookings(User client) {
        Specification<Prenotazione> spec = BookingSpecifications.byClient(client.getId())
            .and(BookingSpecifications.upcomingConfirmed());

        return prenotazioneRepository.findAll(spec, Sort.by("startTime").ascending())
            .stream()
            .map(bookingMapper::toDto)
            .collect(Collectors.toList());
    }

    /**
     * RF_CLR_2 — Storico completo delle prenotazioni.
     */
    public List<BookingResponseDto> getBookingHistory(User client) {
        Specification<Prenotazione> spec = BookingSpecifications.byClient(client.getId());

        return prenotazioneRepository.findAll(spec, Sort.by("startTime").descending())
            .stream()
            .map(bookingMapper::toDto)
            .collect(Collectors.toList());
    }

    /**
     * RF_CLR_3 — Storico filtrato per stato.
     */
    public List<BookingResponseDto> getBookingsByStatus(User client, BookingStatus status) {
        Specification<Prenotazione> spec = BookingSpecifications.byClient(client.getId());

        if (status != null) {
            spec = spec.and(BookingSpecifications.byStatus(status));
        }

        return prenotazioneRepository.findAll(spec, Sort.by("startTime").descending())
            .stream()
            .map(bookingMapper::toDto)
            .collect(Collectors.toList());
    }
}
