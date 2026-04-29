package com.barberbook.service;

import com.barberbook.domain.enums.ScheduleType;
import com.barberbook.domain.model.FasciaOraria;
import com.barberbook.domain.model.Poltrona;
import com.barberbook.domain.model.Servizio;
import com.barberbook.dto.response.AvailabilityResponseDto;
import com.barberbook.exception.ResourceNotFoundException;
import com.barberbook.exception.SlotNotAvailableException;
import com.barberbook.repository.FasciaOrariaRepository;
import com.barberbook.repository.PoltronaRepository;
import com.barberbook.repository.PrenotazioneRepository;
import com.barberbook.repository.ServizioRepository;
import com.barberbook.service.strategy.AvailabilityStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock AvailabilityStrategy availabilityStrategy;
    @Mock PoltronaRepository poltronaRepository;
    @Mock FasciaOrariaRepository fasciaOrariaRepository;
    @Mock ServizioRepository servizioRepository;
    @Mock PrenotazioneRepository prenotazioneRepository;
    
    @InjectMocks AvailabilityService availabilityService;

    @Test
    @DisplayName("getAvailableSlots: recupera correttamente i dati e chiama la strategy")
    void getAvailableSlots_success() {
        Long serviceId = 1L;
        LocalDate date = LocalDate.now();
        
        Servizio servizio = Servizio.builder().id(serviceId).durataMinuti(30).attivo(true).build();
        Poltrona poltrona = Poltrona.builder().id(1L).nome("Poltrona 1").attiva(true).build();
        
        when(servizioRepository.findByIdAndAttivoTrue(serviceId)).thenReturn(Optional.of(servizio));
        when(poltronaRepository.findByAttivaTrue()).thenReturn(List.of(poltrona));
        when(fasciaOrariaRepository.findByPoltronaAndGiornoSettimanaAndTipo(any(), any(), eq(ScheduleType.APERTURA)))
                .thenReturn(Optional.of(FasciaOraria.builder().build()));
        when(availabilityStrategy.calculateAvailableSlots(any(), any(), any())).thenReturn(Collections.emptyList());

        List<AvailabilityResponseDto> result = availabilityService.getAvailableSlots(date, serviceId);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).chairName()).isEqualTo("Poltrona 1");
    }

    @Test
    @DisplayName("getAvailableSlots: lancia eccezione se servizio non trovato")
    void getAvailableSlots_serviceNotFound_throwsException() {
        when(servizioRepository.findByIdAndAttivoTrue(anyLong())).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> availabilityService.getAvailableSlots(LocalDate.now(), 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("assertSlotIsAvailable: lancia eccezione se lo slot è occupato")
    void assertSlotIsAvailable_occupied_throwsException() {
        when(prenotazioneRepository.existsActiveBookingInSlot(anyLong(), any(), any())).thenReturn(true);
        
        assertThatThrownBy(() -> availabilityService.assertSlotIsAvailable(1L, LocalDateTime.now(), LocalDateTime.now().plusHours(1)))
                .isInstanceOf(SlotNotAvailableException.class);
    }

    @Test
    @DisplayName("assertSlotIsAvailable: non lancia nulla se lo slot è libero")
    void assertSlotIsAvailable_free_noException() {
        when(prenotazioneRepository.existsActiveBookingInSlot(anyLong(), any(), any())).thenReturn(false);
        
        availabilityService.assertSlotIsAvailable(1L, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
    }
}
