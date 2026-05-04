package com.barberbook.controller;

import com.barberbook.dto.request.CreateServiceRequestDto;
import com.barberbook.dto.request.UpdateServiceRequestDto;
import com.barberbook.dto.response.ServiceResponseDto;
import com.barberbook.service.ServiceCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: ServiceController")
class ServiceControllerTest {

    @Mock
    private ServiceCatalogService serviceCatalogService;

    @InjectMocks
    private ServiceController serviceController;

    private ServiceResponseDto responseDto;

    @BeforeEach
    void setUp() {
        responseDto = new ServiceResponseDto(1L, "Taglio", "Solo forbici", 30, new BigDecimal("15.00"));
    }

    @Test
    void getAll() {
        when(serviceCatalogService.getAllActive()).thenReturn(List.of(responseDto));
        ResponseEntity<List<ServiceResponseDto>> response = serviceController.getAll();
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getById() {
        when(serviceCatalogService.getById(1L)).thenReturn(responseDto);
        ResponseEntity<ServiceResponseDto> response = serviceController.getById(1L);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(responseDto, response.getBody());
    }

    @Test
    void create() {
        CreateServiceRequestDto dto = new CreateServiceRequestDto("Taglio", "Solo forbici", 30, new BigDecimal("15.00"));
        when(serviceCatalogService.create(dto)).thenReturn(responseDto);
        ResponseEntity<ServiceResponseDto> response = serviceController.create(dto);
        assertEquals(201, response.getStatusCode().value());
        assertEquals(responseDto, response.getBody());
    }

    @Test
    void update() {
        UpdateServiceRequestDto dto = new UpdateServiceRequestDto("Taglio", "Solo forbici", 30, new BigDecimal("15.00"));
        when(serviceCatalogService.update(1L, dto)).thenReturn(responseDto);
        ResponseEntity<ServiceResponseDto> response = serviceController.update(1L, dto);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(responseDto, response.getBody());
    }

    @Test
    void delete() {
        doNothing().when(serviceCatalogService).delete(1L);
        ResponseEntity<Void> response = serviceController.delete(1L);
        assertEquals(204, response.getStatusCode().value());
        verify(serviceCatalogService).delete(1L);
    }
}
