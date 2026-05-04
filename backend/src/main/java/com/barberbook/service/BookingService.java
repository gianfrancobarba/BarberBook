package com.barberbook.service;

import com.barberbook.domain.enums.BookingStatus;
import com.barberbook.domain.enums.ScheduleType;
import com.barberbook.domain.event.*;
import com.barberbook.domain.model.*;
import com.barberbook.dto.request.*;
import com.barberbook.dto.response.BookingResponseDto;
import com.barberbook.exception.ResourceNotFoundException;
import com.barberbook.exception.UnauthorizedOperationException;
import com.barberbook.mapper.BookingMapper;
import com.barberbook.repository.FasciaOrariaRepository;
import com.barberbook.repository.PoltronaRepository;
import com.barberbook.repository.PrenotazioneRepository;
import com.barberbook.repository.ServizioRepository;
import com.barberbook.service.validation.BookingValidationRequest;
import com.barberbook.service.validation.BookingValidator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
public class BookingService {

    private final PrenotazioneRepository prenotazioneRepository;
    private final PoltronaRepository poltronaRepository;
    private final ServizioRepository servizioRepository;
    private final FasciaOrariaRepository fasciaOrariaRepository;
    private final List<BookingValidator> validators;
    private final ApplicationEventPublisher eventPublisher;
    private final BookingMapper bookingMapper;

    public BookingService(PrenotazioneRepository prenotazioneRepository,
                          PoltronaRepository poltronaRepository,
                          ServizioRepository servizioRepository,
                          FasciaOrariaRepository fasciaOrariaRepository,
                          List<BookingValidator> validators,
                          ApplicationEventPublisher eventPublisher,
                          BookingMapper bookingMapper) {
        this.prenotazioneRepository = prenotazioneRepository;
        this.poltronaRepository = poltronaRepository;
        this.servizioRepository = servizioRepository;
        this.fasciaOrariaRepository = fasciaOrariaRepository;
        this.validators = validators;
        this.eventPublisher = eventPublisher;
        this.bookingMapper = bookingMapper;
    }

    /**
     * RF_CLI_6 — CLR invia richiesta di prenotazione.
     */
    public BookingResponseDto createRequest(BookingRequestDto dto, User client) {
        Poltrona chair = getActiveChairOrThrow(dto.chairId());
        Servizio service = getActiveServiceOrThrow(dto.serviceId());

        LocalDateTime start = dto.startTime().atDate(dto.date());
        LocalDateTime end = start.plusMinutes(service.getDurataMinuti());

        runValidationChain(chair, service, start, end, null);

        Prenotazione booking = Prenotazione.builder()
            .poltrona(chair).servizio(service).client(client)
            .startTime(start).endTime(end)
            .status(BookingStatus.IN_ATTESA)
            .createdAt(LocalDateTime.now())
            .build();

        Prenotazione saved = prenotazioneRepository.save(booking);
        eventPublisher.publishEvent(new BookingRequestCreatedEvent(this, saved));
        return bookingMapper.toDto(saved);
    }

    /**
     * RF_CLG_1 — CLG invia richiesta di prenotazione (senza account).
     */
    public BookingResponseDto createGuestRequest(GuestBookingRequestDto dto) {
        Poltrona chair = getActiveChairOrThrow(dto.chairId());
        Servizio service = getActiveServiceOrThrow(dto.serviceId());

        LocalDateTime start = dto.startTime().atDate(dto.date());
        LocalDateTime end = start.plusMinutes(service.getDurataMinuti());

        runValidationChain(chair, service, start, end, null);

        GuestData guestData = new GuestData(dto.guestNome(), dto.guestCognome(), dto.guestTelefono());

        Prenotazione booking = Prenotazione.builder()
            .poltrona(chair).servizio(service).client(null)
            .guestData(guestData)
            .startTime(start).endTime(end)
            .status(BookingStatus.IN_ATTESA)
            .createdAt(LocalDateTime.now())
            .build();

        Prenotazione saved = prenotazioneRepository.save(booking);
        eventPublisher.publishEvent(new BookingRequestCreatedEvent(this, saved));
        return bookingMapper.toDto(saved);
    }

    /**
     * RF_BAR_14 — BAR accetta richiesta.
     */
    public void acceptRequest(Long bookingId) {
        Prenotazione booking = findOrThrow(bookingId);
        booking.setStatus(booking.getStatus().transitionTo(BookingStatus.ACCETTATA));
        booking.setUpdatedAt(LocalDateTime.now());
        prenotazioneRepository.save(booking);
        eventPublisher.publishEvent(new BookingAcceptedEvent(this, booking));
    }

    /**
     * RF_BAR_15 — BAR rifiuta richiesta.
     */
    public void rejectRequest(Long bookingId) {
        Prenotazione booking = findOrThrow(bookingId);
        booking.setStatus(booking.getStatus().transitionTo(BookingStatus.RIFIUTATA));
        booking.setUpdatedAt(LocalDateTime.now());
        prenotazioneRepository.save(booking);
        eventPublisher.publishEvent(new BookingRejectedEvent(this, booking));
    }

    /**
     * RF_CLR_4 — CLR annulla con motivazione.
     */
    public void cancelByClient(Long bookingId, String reason, User requester) {
        Prenotazione booking = findOrThrow(bookingId);

        if (booking.getClient() == null || !booking.getClient().getId().equals(requester.getId())) {
            throw new UnauthorizedOperationException("Non sei autorizzato ad annullare questa prenotazione");
        }

        booking.setStatus(booking.getStatus().transitionTo(BookingStatus.ANNULLATA));
        booking.setCancellationReason(reason);
        booking.setUpdatedAt(LocalDateTime.now());
        prenotazioneRepository.save(booking);
        eventPublisher.publishEvent(new BookingCancelledByClientEvent(this, booking, reason));
    }

    /**
     * RF_BAR_13 — BAR cancella prenotazione.
     */
    public void cancelByBarber(Long bookingId) {
        Prenotazione booking = findOrThrow(bookingId);
        booking.setStatus(booking.getStatus().transitionTo(BookingStatus.ANNULLATA));
        booking.setUpdatedAt(LocalDateTime.now());
        prenotazioneRepository.save(booking);
        eventPublisher.publishEvent(new BookingCancelledByBarberEvent(this, booking));
    }

    /**
     * RF_BAR_11 — BAR crea prenotazione diretta (già ACCETTATA).
     */
    public BookingResponseDto createDirect(DirectBookingRequestDto dto) {
        Poltrona chair = getActiveChairOrThrow(dto.chairId());
        Servizio service = getActiveServiceOrThrow(dto.serviceId());

        LocalDateTime start = dto.startTime().atDate(dto.date());
        LocalDateTime end = start.plusMinutes(service.getDurataMinuti());

        runValidationChain(chair, service, start, end, null);

        GuestData guestData = new GuestData(dto.customerName(), dto.customerSurname(), dto.customerPhone());

        Prenotazione booking = Prenotazione.builder()
            .poltrona(chair).servizio(service).client(null)
            .guestData(guestData)
            .startTime(start).endTime(end)
            .status(BookingStatus.ACCETTATA)
            .createdAt(LocalDateTime.now())
            .build();

        return bookingMapper.toDto(prenotazioneRepository.save(booking));
    }

    /**
     * RF_BAR_12 — BAR modifica prenotazione.
     */
    public BookingResponseDto update(Long bookingId, UpdateBookingRequestDto dto) {
        Prenotazione booking = findOrThrow(bookingId);

        Poltrona chair = dto.chairId() != null ? getActiveChairOrThrow(dto.chairId()) : booking.getPoltrona();
        Servizio service = dto.serviceId() != null ? getActiveServiceOrThrow(dto.serviceId()) : booking.getServizio();

        LocalDate date = dto.date() != null ? dto.date() : booking.getStartTime().toLocalDate();
        LocalTime time = dto.startTime() != null ? dto.startTime() : booking.getStartTime().toLocalTime();

        LocalDateTime newStart = time.atDate(date);
        LocalDateTime newEnd = newStart.plusMinutes(service.getDurataMinuti());

        runValidationChain(chair, service, newStart, newEnd, bookingId);

        booking.setPoltrona(chair);
        booking.setServizio(service);
        booking.setStartTime(newStart);
        booking.setEndTime(newEnd);
        booking.setUpdatedAt(LocalDateTime.now());

        return bookingMapper.toDto(prenotazioneRepository.save(booking));
    }

    /**
     * RF_CLR_5 — Riprenotazione rapida.
     * Preleva servizio e poltrona da una prenotazione passata
     * e avvia una nuova richiesta per il nuovo slot.
     */
    public BookingResponseDto rebook(Long pastBookingId, LocalDate newDate,
                                      LocalTime newStartTime, User client) {
        Prenotazione past = findOrThrow(pastBookingId);

        // Verifica ownership
        if (past.getClient() == null || !past.getClient().getId().equals(client.getId())) {
            throw new UnauthorizedOperationException("Non sei autorizzato a riprenotare basandoti su questa prenotazione");
        }

        // Crea nuova richiesta con stessa poltrona e servizio
        BookingRequestDto dto = new BookingRequestDto(
            past.getPoltrona().getId(),
            past.getServizio().getId(),
            newDate,
            newStartTime
        );

        return createRequest(dto, client);  // riutilizza il flusso standard di creazione
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDto> getPendingRequests() {
        return bookingMapper.toDtoList(
            prenotazioneRepository.findByStatusOrderByCreatedAtAsc(BookingStatus.IN_ATTESA));
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDto> getClientBookings(User client) {
        return bookingMapper.toDtoList(
            prenotazioneRepository.findByClientOrderByStartTimeDesc(client));
    }

    private void runValidationChain(Poltrona chair, Servizio service,
                                     LocalDateTime start, LocalDateTime end, Long excludeId) {
        DayOfWeek dayOfWeek = start.getDayOfWeek();
        FasciaOraria schedule = fasciaOrariaRepository
            .findByPoltronaAndGiornoSettimanaAndTipo(chair, dayOfWeek, ScheduleType.APERTURA)
            .stream().findFirst().orElse(null);
        List<FasciaOraria> breaks = fasciaOrariaRepository
            .findByPoltronaAndGiornoSettimanaAndTipo(chair, dayOfWeek, ScheduleType.PAUSA);
        List<Prenotazione> existing = prenotazioneRepository
            .findActiveBookingsByChairAndDate(chair.getId(), start.toLocalDate());

        BookingValidationRequest req = new BookingValidationRequest(
            chair, service, schedule, breaks, start, end, existing, excludeId);

        validators.forEach(v -> v.validate(req));
    }

    private Prenotazione findOrThrow(Long id) {
        return prenotazioneRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Prenotazione non trovata: " + id));
    }

    private Poltrona getActiveChairOrThrow(Long id) {
        return poltronaRepository.findByIdAndAttivaTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Poltrona non trovata: " + id));
    }

    private Servizio getActiveServiceOrThrow(Long id) {
        return servizioRepository.findByIdAndAttivoTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Servizio non trovato: " + id));
    }
}
