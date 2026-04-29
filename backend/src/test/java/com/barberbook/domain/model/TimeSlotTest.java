package com.barberbook.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeSlotTest {

    @Test
    @DisplayName("Constructor: crea correttamente lo slot con orari validi")
    void constructor_validTimes_success() {
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(10, 0);
        TimeSlot slot = new TimeSlot(start, end);
        
        assertThat(slot.start()).isEqualTo(start);
        assertThat(slot.end()).isEqualTo(end);
    }

    @Test
    @DisplayName("Constructor: lancia eccezione se inizio >= fine")
    void constructor_invalidTimes_throwsException() {
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(9, 0);
        
        assertThatThrownBy(() -> new TimeSlot(start, end))
                .isInstanceOf(IllegalArgumentException.class);
                
        assertThatThrownBy(() -> new TimeSlot(start, start))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("overlapsWith: rileva correttamente le sovrapposizioni")
    void overlapsWith_tests() {
        TimeSlot base = new TimeSlot(LocalTime.of(10, 0), LocalTime.of(11, 0));
        
        // Sovrapposizione parziale (inizio prima, fine durante)
        assertThat(base.overlapsWith(LocalTime.of(9, 30), LocalTime.of(10, 30))).isTrue();
        
        // Sovrapposizione parziale (inizio durante, fine dopo)
        assertThat(base.overlapsWith(LocalTime.of(10, 30), LocalTime.of(11, 30))).isTrue();
        
        // Sovrapposizione totale (interno)
        assertThat(base.overlapsWith(LocalTime.of(10, 15), LocalTime.of(10, 45))).isTrue();
        
        // Sovrapposizione totale (esterno)
        assertThat(base.overlapsWith(LocalTime.of(9, 0), LocalTime.of(12, 0))).isTrue();
        
        // Adiacente (fine = inizio) -> No sovrapposizione
        assertThat(base.overlapsWith(LocalTime.of(11, 0), LocalTime.of(12, 0))).isFalse();
        
        // Adiacente (inizio = fine) -> No sovrapposizione
        assertThat(base.overlapsWith(LocalTime.of(9, 0), LocalTime.of(10, 0))).isFalse();
    }

    @Test
    @DisplayName("fitsWithin: verifica se lo slot è contenuto in un intervallo")
    void fitsWithin_tests() {
        TimeSlot slot = new TimeSlot(LocalTime.of(10, 0), LocalTime.of(11, 0));
        
        assertThat(slot.fitsWithin(LocalTime.of(9, 0), LocalTime.of(12, 0))).isTrue();
        assertThat(slot.fitsWithin(LocalTime.of(10, 0), LocalTime.of(11, 0))).isTrue();
        
        // Sfora inizio
        assertThat(slot.fitsWithin(LocalTime.of(10, 30), LocalTime.of(12, 0))).isFalse();
        
        // Sfora fine
        assertThat(slot.fitsWithin(LocalTime.of(9, 0), LocalTime.of(10, 30))).isFalse();
    }
}
