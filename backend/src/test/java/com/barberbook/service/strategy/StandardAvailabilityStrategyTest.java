package com.barberbook.service.strategy;

import com.barberbook.domain.enums.ScheduleType;
import com.barberbook.domain.model.AvailabilityContext;
import com.barberbook.domain.model.FasciaOraria;
import com.barberbook.domain.model.Prenotazione;
import com.barberbook.domain.model.TimeSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StandardAvailabilityStrategyTest {

    private final StandardAvailabilityStrategy strategy = new StandardAvailabilityStrategy();
    private final LocalDate today = LocalDate.now();

    // Helper per creare una fascia di apertura
    private FasciaOraria apertura(LocalTime start, LocalTime end) {
        return FasciaOraria.builder()
                .oraInizio(start)
                .oraFine(end)
                .tipo(ScheduleType.APERTURA)
                .build();
    }

    // Helper per creare una fascia di pausa
    private FasciaOraria pausa(LocalTime start, LocalTime end) {
        return FasciaOraria.builder()
                .oraInizio(start)
                .oraFine(end)
                .tipo(ScheduleType.PAUSA)
                .build();
    }

    // Helper per creare una prenotazione (Stub)
    private Prenotazione booking(LocalTime start, LocalTime end) {
        return Prenotazione.builder()
                .startTime(today.atTime(start))
                .endTime(today.atTime(end))
                .build();
    }

    @Test
    @DisplayName("Giorno chiuso: se non c'è fascia apertura, ritorna lista vuota")
    void calculateAvailableSlots_closedDay_returnsEmpty() {
        AvailabilityContext ctx = new AvailabilityContext(null, Collections.emptyList(), Collections.emptyList());
        List<TimeSlot> result = strategy.calculateAvailableSlots(today, Duration.ofMinutes(30), ctx);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Senza ostacoli: 9:00-11:00, servizio 30min -> 7 slot")
    void calculateAvailableSlots_noObstacles_30min() {
        AvailabilityContext ctx = new AvailabilityContext(apertura(LocalTime.of(9, 0), LocalTime.of(11, 0)), 
                                      Collections.emptyList(), Collections.emptyList());
        
        List<TimeSlot> result = strategy.calculateAvailableSlots(today, Duration.ofMinutes(30), ctx);
        
        assertThat(result).hasSize(7);
        assertThat(result.get(0).start()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.get(6).start()).isEqualTo(LocalTime.of(10, 30));
    }

    @Test
    @DisplayName("Durata servizio > orario apertura -> nessun slot")
    void calculateAvailableSlots_durationExceedsOpening_returnsEmpty() {
        AvailabilityContext ctx = new AvailabilityContext(apertura(LocalTime.of(9, 0), LocalTime.of(10, 0)), 
                                      Collections.emptyList(), Collections.emptyList());
        
        List<TimeSlot> result = strategy.calculateAvailableSlots(today, Duration.ofHours(2), ctx);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Filtro Pause: una pausa di 1 ora al centro rimuove gli slot sovrapposti")
    void calculateAvailableSlots_withPause_filtersCorrectly() {
        AvailabilityContext ctx = new AvailabilityContext(
                apertura(LocalTime.of(9, 0), LocalTime.of(13, 0)),
                List.of(pausa(LocalTime.of(11, 0), LocalTime.of(12, 0))),
                Collections.emptyList()
        );

        List<TimeSlot> result = strategy.calculateAvailableSlots(today, Duration.ofMinutes(30), ctx);
        
        for (TimeSlot slot : result) {
            assertThat(slot.overlapsWith(LocalTime.of(11, 0), LocalTime.of(12, 0))).isFalse();
        }
    }

    @Test
    @DisplayName("Filtro Prenotazioni: rimuove slot che si sovrappongono con prenotazioni")
    void calculateAvailableSlots_withBookings_filtersCorrectly() {
        AvailabilityContext ctx = new AvailabilityContext(
                apertura(LocalTime.of(9, 0), LocalTime.of(11, 0)),
                Collections.emptyList(),
                List.of(booking(LocalTime.of(10, 0), LocalTime.of(10, 30)))
        );

        List<TimeSlot> result = strategy.calculateAvailableSlots(today, Duration.ofMinutes(30), ctx);
        
        assertThat(result).extracting(TimeSlot::start)
                .contains(LocalTime.of(9, 0), LocalTime.of(9, 15), LocalTime.of(9, 30), LocalTime.of(10, 30))
                .doesNotContain(LocalTime.of(9, 45), LocalTime.of(10, 0), LocalTime.of(10, 15));
    }

    @Test
    @DisplayName("Pausa copre tutto il giorno -> nessun slot")
    void calculateAvailableSlots_pauseCoversAllDay_returnsEmpty() {
        AvailabilityContext ctx = new AvailabilityContext(
                apertura(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                List.of(pausa(LocalTime.of(8, 0), LocalTime.of(13, 0))),
                Collections.emptyList()
        );
        List<TimeSlot> result = strategy.calculateAvailableSlots(today, Duration.ofMinutes(30), ctx);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Slot adiacente a prenotazione o pausa: non deve essere rimosso")
    void calculateAvailableSlots_adjacentToObstacle_isFree() {
        AvailabilityContext ctx = new AvailabilityContext(
                apertura(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                Collections.emptyList(),
                List.of(booking(LocalTime.of(10, 0), LocalTime.of(10, 30)))
        );

        List<TimeSlot> result = strategy.calculateAvailableSlots(today, Duration.ofMinutes(30), ctx);
        assertThat(result).extracting(TimeSlot::start).contains(LocalTime.of(10, 30));
    }

    @Test
    @DisplayName("Combinazione: Pausa + Prenotazione simultanea")
    void calculateAvailableSlots_pauseAndBooking_combined() {
        AvailabilityContext ctx = new AvailabilityContext(
                apertura(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                List.of(pausa(LocalTime.of(10, 0), LocalTime.of(10, 30))),
                List.of(booking(LocalTime.of(11, 0), LocalTime.of(11, 30)))
        );

        List<TimeSlot> result = strategy.calculateAvailableSlots(today, Duration.ofMinutes(30), ctx);
        
        assertThat(result).extracting(TimeSlot::start)
                .doesNotContain(LocalTime.of(9, 45), LocalTime.of(10, 0), LocalTime.of(10, 15))
                .doesNotContain(LocalTime.of(10, 45), LocalTime.of(11, 0), LocalTime.of(11, 15))
                .contains(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 30), LocalTime.of(11, 30));
    }

    @Test
    @DisplayName("Granularità: slot generati ogni 15 minuti")
    void calculateAvailableSlots_granularityCheck() {
        AvailabilityContext ctx = new AvailabilityContext(apertura(LocalTime.of(9, 0), LocalTime.of(10, 0)), 
                                      Collections.emptyList(), Collections.emptyList());
        
        List<TimeSlot> result = strategy.calculateAvailableSlots(today, Duration.ofMinutes(30), ctx);
        
        assertThat(result).extracting(TimeSlot::start)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(9, 15), LocalTime.of(9, 30));
    }

    @Test
    @DisplayName("Servizio di 1 ora in 1 ora di apertura -> esatto 1 slot")
    void calculateAvailableSlots_exactMatch() {
        AvailabilityContext ctx = new AvailabilityContext(apertura(LocalTime.of(9, 0), LocalTime.of(10, 0)), 
                                      Collections.emptyList(), Collections.emptyList());
        
        List<TimeSlot> result = strategy.calculateAvailableSlots(today, Duration.ofHours(1), ctx);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).start()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    @DisplayName("Prenotazione che copre tutto lo spazio rimanente -> nessun slot")
    void calculateAvailableSlots_bookingFillsRemainingSpace() {
        AvailabilityContext ctx = new AvailabilityContext(
                apertura(LocalTime.of(9, 0), LocalTime.of(10, 0)),
                Collections.emptyList(),
                List.of(booking(LocalTime.of(9, 15), LocalTime.of(9, 45)))
        );
        List<TimeSlot> result = strategy.calculateAvailableSlots(today, Duration.ofMinutes(30), ctx);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Robustezza: liste null o vuote gestite correttamente")
    void calculateAvailableSlots_robustness() {
        AvailabilityContext ctx = new AvailabilityContext(apertura(LocalTime.of(9, 0), LocalTime.of(10, 0)), 
                                      null, null);
        List<TimeSlot> result = strategy.calculateAvailableSlots(today, Duration.ofMinutes(30), ctx);
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("Orario di chiusura: slot non deve terminare dopo la chiusura")
    void calculateAvailableSlots_notAfterClosing() {
        AvailabilityContext ctx = new AvailabilityContext(apertura(LocalTime.of(9, 0), LocalTime.of(10, 0)), 
                                      Collections.emptyList(), Collections.emptyList());
        List<TimeSlot> result = strategy.calculateAvailableSlots(today, Duration.ofMinutes(45), ctx);
        assertThat(result).extracting(TimeSlot::start).containsExactly(LocalTime.of(9, 0), LocalTime.of(9, 15));
    }

    @Test
    @DisplayName("Scenario Reale: Configurazione standard Tony (9-19, pausa 13-15)")
    void calculateAvailableSlots_realScenario() {
        AvailabilityContext ctx = new AvailabilityContext(
                apertura(LocalTime.of(9, 0), LocalTime.of(19, 0)),
                List.of(pausa(LocalTime.of(13, 0), LocalTime.of(15, 0))),
                Collections.emptyList()
        );
        List<TimeSlot> result = strategy.calculateAvailableSlots(today, Duration.ofMinutes(30), ctx);
        assertThat(result).hasSize(30);
    }

    @Test
    @DisplayName("Slot all'inizio della giornata: generato correttamente")
    void calculateAvailableSlots_atStartOfOpening() {
        AvailabilityContext ctx = new AvailabilityContext(apertura(LocalTime.of(9, 0), LocalTime.of(10, 0)), 
                                      Collections.emptyList(), Collections.emptyList());
        List<TimeSlot> result = strategy.calculateAvailableSlots(today, Duration.ofMinutes(15), ctx);
        assertThat(result.get(0).start()).isEqualTo(LocalTime.of(9, 0));
    }
}
