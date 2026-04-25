package com.barberbook.controller;

import com.barberbook.dto.request.CreateServiceRequestDto;
import com.barberbook.dto.request.UpdateServiceRequestDto;
import com.barberbook.dto.response.ServiceResponseDto;
import com.barberbook.service.ServiceCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceCatalogService serviceCatalogService;

    // -------------------------------------------------------
    // PUBBLICI — accessibili senza autenticazione
    // -------------------------------------------------------

    @GetMapping
    public ResponseEntity<List<ServiceResponseDto>> getAll() {
        return ResponseEntity.ok(serviceCatalogService.getAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceCatalogService.getById(id));
    }

    // -------------------------------------------------------
    // RISERVATI BAR — @PreAuthorize su metodo
    // -------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<ServiceResponseDto> create(
            @Valid @RequestBody CreateServiceRequestDto dto) {
        return ResponseEntity.status(201).body(serviceCatalogService.create(dto));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<ServiceResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceRequestDto dto) {
        return ResponseEntity.ok(serviceCatalogService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceCatalogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
