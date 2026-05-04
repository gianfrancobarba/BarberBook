package com.barberbook.service;

import com.barberbook.domain.enums.ScheduleType;
import com.barberbook.domain.model.FasciaOraria;
import com.barberbook.domain.model.Poltrona;
import com.barberbook.dto.request.CreateScheduleRequestDto;
import com.barberbook.dto.response.ScheduleResponseDto;
import com.barberbook.exception.InvalidTimeRangeException;
import com.barberbook.exception.ResourceNotFoundException;
import com.barberbook.mapper.FasciaOrariaMapper;
import com.barberbook.repository.FasciaOrariaRepository;
import com.barberbook.repository.PoltronaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: ScheduleService")
class ScheduleServiceTest {

    @Mock
    private FasciaOrariaRepository fasciaOrariaRepository;
    @Mock
    private PoltronaRepository poltronaRepository;
    @Mock
    private FasciaOrariaMapper fasciaOrariaMapper;

    @InjectMocks
    private ScheduleService scheduleService;

    private Poltrona poltrona;
    private FasciaOraria fasciaOraria;
    private CreateScheduleRequestDto createRequest;
    private ScheduleResponseDto responseDto;

    @BeforeEach
    void setUp() {
        poltrona = Poltrona.builder().id(1L).nome("P1").attiva(true).build();
        fasciaOraria = FasciaOraria.builder()
                .poltrona(poltrona)
                .giornoSettimana(DayOfWeek.MONDAY)
                .oraInizio(LocalTime.of(9, 0))
                .oraFine(LocalTime.of(13, 0))
                .tipo(ScheduleType.APERTURA)
                .build();
        createRequest = new CreateScheduleRequestDto(
                1L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0), ScheduleType.APERTURA
        );
        responseDto = new ScheduleResponseDto(
                1L, 1L, "P1", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0), ScheduleType.APERTURA
        );
    }

    @Test
    @DisplayName("getScheduleForChair - successo")
    void getScheduleForChair_success() {
        when(poltronaRepository.findByIdAndAttivaTrue(1L)).thenReturn(Optional.of(poltrona));
        when(fasciaOrariaRepository.findByPoltrona(poltrona)).thenReturn(List.of(fasciaOraria));
        when(fasciaOrariaMapper.toDtoList(List.of(fasciaOraria))).thenReturn(List.of(responseDto));

        List<ScheduleResponseDto> result = scheduleService.getScheduleForChair(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        verify(poltronaRepository).findByIdAndAttivaTrue(1L);
        verify(fasciaOrariaRepository).findByPoltrona(poltrona);
    }

    @Test
    @DisplayName("getScheduleForChair - poltrona non trovata")
    void getScheduleForChair_chairNotFound() {
        when(poltronaRepository.findByIdAndAttivaTrue(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> scheduleService.getScheduleForChair(1L));
        verify(fasciaOrariaRepository, never()).findByPoltrona(any());
    }

    @Test
    @DisplayName("addSchedule - successo")
    void addSchedule_success() {
        when(poltronaRepository.findByIdAndAttivaTrue(1L)).thenReturn(Optional.of(poltrona));
        when(fasciaOrariaRepository.save(any(FasciaOraria.class))).thenReturn(fasciaOraria);
        when(fasciaOrariaMapper.toDto(fasciaOraria)).thenReturn(responseDto);

        ScheduleResponseDto result = scheduleService.addSchedule(createRequest);

        assertNotNull(result);
        assertEquals(1L, result.id());
        verify(fasciaOrariaRepository).save(any(FasciaOraria.class));
    }

    @Test
    @DisplayName("addSchedule - poltrona non trovata")
    void addSchedule_chairNotFound() {
        when(poltronaRepository.findByIdAndAttivaTrue(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> scheduleService.addSchedule(createRequest));
        verify(fasciaOrariaRepository, never()).save(any());
    }

    @Test
    @DisplayName("addSchedule - range orario non valido")
    void addSchedule_invalidTimeRange() {
        when(poltronaRepository.findByIdAndAttivaTrue(1L)).thenReturn(Optional.of(poltrona));
        CreateScheduleRequestDto invalidDto = new CreateScheduleRequestDto(
                1L, DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(9, 0), ScheduleType.APERTURA
        );

        assertThrows(InvalidTimeRangeException.class, () -> scheduleService.addSchedule(invalidDto));
        verify(fasciaOrariaRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("addSchedule - orario inizio e fine uguali non valido")
    void addSchedule_equalTimeRange() {
        when(poltronaRepository.findByIdAndAttivaTrue(1L)).thenReturn(Optional.of(poltrona));
        CreateScheduleRequestDto invalidDto = new CreateScheduleRequestDto(
                1L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(9, 0), ScheduleType.APERTURA
        );

        assertThrows(InvalidTimeRangeException.class, () -> scheduleService.addSchedule(invalidDto));
        verify(fasciaOrariaRepository, never()).save(any());
    }

    @Test
    @DisplayName("removeSchedule - successo")
    void removeSchedule_success() {
        when(fasciaOrariaRepository.findById(1L)).thenReturn(Optional.of(fasciaOraria));

        scheduleService.removeSchedule(1L);

        verify(fasciaOrariaRepository).delete(fasciaOraria);
    }

    @Test
    @DisplayName("removeSchedule - non trovata")
    void removeSchedule_notFound() {
        when(fasciaOrariaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> scheduleService.removeSchedule(1L));
        verify(fasciaOrariaRepository, never()).delete(any());
    }
}
