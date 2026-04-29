package com.barberbook.scheduler;

import com.barberbook.domain.enums.BookingStatus;
import com.barberbook.domain.model.Prenotazione;
import com.barberbook.repository.PrenotazioneRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Task pianificato per la gestione automatica degli stati delle prenotazioni.
 */
@Component
public class BookingStatusScheduler {

    private final PrenotazioneRepository prenotazioneRepository;

    public BookingStatusScheduler(PrenotazioneRepository prenotazioneRepository) {
        this.prenotazioneRepository = prenotazioneRepository;
    }

    /**
     * Ogni ora, le prenotazioni ACCETTATE il cui orario di fine è trascorso 
     * vengono marcate come PASSATE.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void markExpiredBookingsAsPassed() {
        LocalDateTime now = LocalDateTime.now();
        List<Prenotazione> expired = prenotazioneRepository
            .findByStatusAndEndTimeBefore(BookingStatus.ACCETTATA, now);

        if (!expired.isEmpty()) {
            expired.forEach(b -> {
                b.setStatus(BookingStatus.PASSATA);
                b.setUpdatedAt(now);
            });
            prenotazioneRepository.saveAll(expired);
        }
    }
}
