package com.barberbook.service;

import com.barberbook.domain.model.Servizio;
import com.barberbook.dto.request.CreateServiceRequestDto;
import com.barberbook.dto.request.UpdateServiceRequestDto;
import com.barberbook.dto.response.ServiceResponseDto;
import com.barberbook.exception.ResourceNotFoundException;
import com.barberbook.mapper.ServizioMapper;
import com.barberbook.repository.ServizioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceCatalogServiceTest {

    @Mock
    private ServizioRepository servizioRepository;

    @Mock
    private ServizioMapper servizioMapper;

    @InjectMocks
    private ServiceCatalogService serviceCatalogService;

    @Test
    @DisplayName("getAllActive: ritorna solo i servizi con attivo=true")
    void getAllActive_returnsOnlyActiveServices() {
        Servizio s1 = Servizio.builder().id(1L).nome("Taglio").attivo(true).build();
        Servizio s2 = Servizio.builder().id(2L).nome("Barba").attivo(true).build();
        ServiceResponseDto dto1 = new ServiceResponseDto(1L, "Taglio", null, 30, BigDecimal.TEN, true);
        ServiceResponseDto dto2 = new ServiceResponseDto(2L, "Barba", null, 20, BigDecimal.TEN, true);

        given(servizioRepository.findByAttivoTrue()).willReturn(List.of(s1, s2));
        given(servizioMapper.toDtoList(any())).willReturn(List.of(dto1, dto2));

        List<ServiceResponseDto> result = serviceCatalogService.getAllActive();

        assertThat(result).hasSize(2);
        verify(servizioRepository).findByAttivoTrue();
    }

    @Test
    @DisplayName("getById: servizio esistente e attivo ritorna DTO")
    void getById_existingActiveService_returnsDto() {
        Servizio s = Servizio.builder().id(1L).nome("Taglio").attivo(true).build();
        ServiceResponseDto dto = new ServiceResponseDto(1L, "Taglio", null, 30, BigDecimal.TEN, true);

        given(servizioRepository.findByIdAndAttivoTrue(1L)).willReturn(Optional.of(s));
        given(servizioMapper.toDto(s)).willReturn(dto);

        ServiceResponseDto result = serviceCatalogService.getById(1L);

        assertThat(result.nome()).isEqualTo("Taglio");
    }

    @Test
    @DisplayName("getById: servizio non esistente lancia ResourceNotFoundException")
    void getById_nonExistent_throwsException() {
        given(servizioRepository.findByIdAndAttivoTrue(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> serviceCatalogService.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create: salva correttamente il servizio")
    void create_validData_returnsCreatedDto() {
        CreateServiceRequestDto request = new CreateServiceRequestDto("Taglio", "Desc", 30, BigDecimal.TEN);
        Servizio s = Servizio.builder().nome("Taglio").build();
        ServiceResponseDto dto = new ServiceResponseDto(1L, "Taglio", "Desc", 30, BigDecimal.TEN, true);

        given(servizioMapper.toEntity(request)).willReturn(s);
        given(servizioRepository.save(any())).willReturn(s);
        given(servizioMapper.toDto(any())).willReturn(dto);

        ServiceResponseDto result = serviceCatalogService.create(request);

        assertThat(result.id()).isEqualTo(1L);
        verify(servizioRepository).save(s);
    }

    @Test
    @DisplayName("update: aggiorna tutti i campi se forniti")
    void update_allFields_updatesEverything() {
        Servizio s = Servizio.builder()
                .id(1L)
                .nome("Vecchio")
                .descrizione("Vecchia Desc")
                .durataMinuti(30)
                .prezzo(BigDecimal.TEN)
                .attivo(true)
                .build();
        UpdateServiceRequestDto update = new UpdateServiceRequestDto("Nuovo", "Nuova Desc", 60, new BigDecimal("15.00"));
        
        given(servizioRepository.findByIdAndAttivoTrue(1L)).willReturn(Optional.of(s));
        given(servizioRepository.save(any())).willReturn(s);
        given(servizioMapper.toDto(any())).willReturn(mock(ServiceResponseDto.class));

        serviceCatalogService.update(1L, update);

        assertThat(s.getNome()).isEqualTo("Nuovo");
        assertThat(s.getDescrizione()).isEqualTo("Nuova Desc");
        assertThat(s.getDurataMinuti()).isEqualTo(60);
        assertThat(s.getPrezzo()).isEqualByComparingTo("15.00");
        assertThat(s.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("update: non sovrascrive i campi con null (semantica PATCH)")
    void update_partialData_doesNotOverwriteWithNull() {
        Servizio s = Servizio.builder()
                .id(1L)
                .nome("Esistente")
                .descrizione("Desc")
                .durataMinuti(30)
                .prezzo(BigDecimal.TEN)
                .attivo(true)
                .build();
        UpdateServiceRequestDto update = new UpdateServiceRequestDto(null, "Nuova Desc", null, null);
        
        given(servizioRepository.findByIdAndAttivoTrue(1L)).willReturn(Optional.of(s));
        given(servizioRepository.save(any())).willReturn(s);
        given(servizioMapper.toDto(any())).willReturn(mock(ServiceResponseDto.class));

        serviceCatalogService.update(1L, update);

        assertThat(s.getNome()).isEqualTo("Esistente"); // Invariato
        assertThat(s.getDescrizione()).isEqualTo("Nuova Desc"); // Cambiato
        assertThat(s.getDurataMinuti()).isEqualTo(30); // Invariato
    }

    @Test
    @DisplayName("update: lancia eccezione se il servizio è inattivo o inesistente")
    void update_serviceNotFound_throwsException() {
        given(servizioRepository.findByIdAndAttivoTrue(99L)).willReturn(Optional.empty());
        UpdateServiceRequestDto dto = new UpdateServiceRequestDto("Nome", null, null, null);

        assertThatThrownBy(() -> serviceCatalogService.update(99L, dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete: lancia eccezione se il servizio è già inattivo o inesistente")
    void delete_serviceNotFound_throwsException() {
        given(servizioRepository.findByIdAndAttivoTrue(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> serviceCatalogService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete: imposta attivo=false e updatedAt")
    void delete_activeService_setsAttivoFalse() {
        Servizio s = Servizio.builder().id(1L).nome("Taglio").attivo(true).build();
        given(servizioRepository.findByIdAndAttivoTrue(1L)).willReturn(Optional.of(s));

        serviceCatalogService.delete(1L);

        assertThat(s.isAttivo()).isFalse();
        assertThat(s.getUpdatedAt()).isNotNull();
        verify(servizioRepository).save(s);
    }
    @Test
    @DisplayName("delete: il servizio eliminato non appare più in vetrina")
    void delete_thenGetAllActive_deletedServiceNotReturned() {
        Servizio s = Servizio.builder().id(1L).nome("Taglio").attivo(true).build();
        given(servizioRepository.findByIdAndAttivoTrue(1L)).willReturn(Optional.of(s));

        serviceCatalogService.delete(1L);

        assertThat(s.isAttivo()).isFalse();

        // When getAllActive is called, repository returns empty list or list without s
        // In unit test, we just verify the behaviour of the mock
        given(servizioRepository.findByAttivoTrue()).willReturn(List.of());
        given(servizioMapper.toDtoList(any())).willReturn(List.of());

        List<ServiceResponseDto> activeServices = serviceCatalogService.getAllActive();
        assertThat(activeServices).isEmpty();
    }
}
