package com.barberbook.service;

import com.barberbook.domain.model.Poltrona;
import com.barberbook.dto.request.CreateChairRequestDto;
import com.barberbook.dto.request.UpdateChairRequestDto;
import com.barberbook.dto.response.ChairResponseDto;
import com.barberbook.exception.ChairNameAlreadyExistsException;
import com.barberbook.exception.ResourceNotFoundException;
import com.barberbook.mapper.PoltronaMapper;
import com.barberbook.repository.PoltronaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChairServiceTest {

    @Mock
    private PoltronaRepository poltronaRepository;

    @Mock
    private PoltronaMapper poltronaMapper;

    @InjectMocks
    private ChairService chairService;

    // --- getAllActive ---

    @Test
    @DisplayName("getAllActive: ritorna solo le poltrone attive")
    void getAllActive_returnsOnlyActiveChairs() {
        Poltrona p1 = Poltrona.builder().id(1L).nome("P1").attiva(true).build();
        Poltrona p2 = Poltrona.builder().id(2L).nome("P2").attiva(true).build();
        ChairResponseDto dto1 = new ChairResponseDto(1L, "P1");
        ChairResponseDto dto2 = new ChairResponseDto(2L, "P2");

        given(poltronaRepository.findByAttivaTrue()).willReturn(List.of(p1, p2));
        given(poltronaMapper.toDtoList(any())).willReturn(List.of(dto1, dto2));

        List<ChairResponseDto> result = chairService.getAllActive();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).nome()).isEqualTo("P1");
        verify(poltronaRepository).findByAttivaTrue();
    }

    // --- getById ---

    @Test
    @DisplayName("getById: poltrona attiva ritorna DTO")
    void getById_existingActiveChair_returnsDto() {
        Poltrona p = Poltrona.builder().id(1L).nome("P1").attiva(true).build();
        ChairResponseDto dto = new ChairResponseDto(1L, "P1");

        given(poltronaRepository.findByIdAndAttivaTrue(1L)).willReturn(Optional.of(p));
        given(poltronaMapper.toDto(p)).willReturn(dto);

        ChairResponseDto result = chairService.getById(1L);

        assertThat(result.nome()).isEqualTo("P1");
    }

    @Test
    @DisplayName("getById: poltrona non esistente lancia ResourceNotFoundException")
    void getById_nonExistent_throwsException() {
        given(poltronaRepository.findByIdAndAttivaTrue(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chairService.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- create ---

    @Test
    @DisplayName("create: aggiunta con nome univoco ha successo")
    void create_uniqueName_success() {
        CreateChairRequestDto request = new CreateChairRequestDto("P-New");
        Poltrona p = Poltrona.builder().nome("P-New").build();
        ChairResponseDto dto = new ChairResponseDto(3L, "P-New");

        given(poltronaRepository.existsByNome("P-New")).willReturn(false);
        given(poltronaMapper.toEntity(request)).willReturn(p);
        given(poltronaRepository.save(any())).willReturn(p);
        given(poltronaMapper.toDto(any())).willReturn(dto);

        ChairResponseDto result = chairService.create(request);

        assertThat(result.id()).isEqualTo(3L);
        assertThat(p.isAttiva()).isTrue();
        assertThat(p.getCreatedAt()).isNotNull();
        verify(poltronaRepository).save(p);
    }

    @Test
    @DisplayName("create: nome duplicato lancia ChairNameAlreadyExistsException")
    void create_duplicateName_throwsException() {
        CreateChairRequestDto request = new CreateChairRequestDto("P1");
        given(poltronaRepository.existsByNome("P1")).willReturn(true);

        assertThatThrownBy(() -> chairService.create(request))
                .isInstanceOf(ChairNameAlreadyExistsException.class);
    }

    // --- rename ---

    @Test
    @DisplayName("rename: nuovo nome univoco ha successo")
    void rename_newUniqueName_success() {
        Poltrona p = Poltrona.builder().id(1L).nome("Vecchio").attiva(true).build();
        UpdateChairRequestDto request = new UpdateChairRequestDto("Nuovo");

        given(poltronaRepository.findByIdAndAttivaTrue(1L)).willReturn(Optional.of(p));
        given(poltronaRepository.existsByNome("Nuovo")).willReturn(false);
        given(poltronaRepository.save(any())).willReturn(p);
        given(poltronaMapper.toDto(any())).willReturn(new ChairResponseDto(1L, "Nuovo"));

        chairService.rename(1L, request);

        assertThat(p.getNome()).isEqualTo("Nuovo");
        assertThat(p.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("rename: rinominare con lo stesso nome non causa conflitto")
    void rename_sameName_success() {
        Poltrona p = Poltrona.builder().id(1L).nome("P1").attiva(true).build();
        UpdateChairRequestDto request = new UpdateChairRequestDto("P1");

        given(poltronaRepository.findByIdAndAttivaTrue(1L)).willReturn(Optional.of(p));
        given(poltronaRepository.save(any())).willReturn(p);
        given(poltronaMapper.toDto(any())).willReturn(new ChairResponseDto(1L, "P1"));

        chairService.rename(1L, request);

        assertThat(p.getNome()).isEqualTo("P1");
    }

    @Test
    @DisplayName("rename: nome già in uso da un'altra poltrona lancia eccezione")
    void rename_duplicateName_throwsException() {
        Poltrona p1 = Poltrona.builder().id(1L).nome("P1").attiva(true).build();
        UpdateChairRequestDto request = new UpdateChairRequestDto("P2");

        given(poltronaRepository.findByIdAndAttivaTrue(1L)).willReturn(Optional.of(p1));
        given(poltronaRepository.existsByNome("P2")).willReturn(true);

        assertThatThrownBy(() -> chairService.rename(1L, request))
                .isInstanceOf(ChairNameAlreadyExistsException.class);
    }

    // --- deactivate ---

    @Test
    @DisplayName("deactivate: imposta attiva=false")
    void deactivate_success() {
        Poltrona p = Poltrona.builder().id(1L).nome("P1").attiva(true).build();
        given(poltronaRepository.findByIdAndAttivaTrue(1L)).willReturn(Optional.of(p));

        chairService.deactivate(1L);

        assertThat(p.isAttiva()).isFalse();
        assertThat(p.getUpdatedAt()).isNotNull();
        verify(poltronaRepository).save(p);
    }

    @Test
    @DisplayName("deactivate: poltrona già disattiva o inesistente lancia eccezione")
    void deactivate_notFound_throwsException() {
        given(poltronaRepository.findByIdAndAttivaTrue(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chairService.deactivate(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
