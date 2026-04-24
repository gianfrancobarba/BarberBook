package com.barberbook.service;

import com.barberbook.domain.model.Servizio;
import com.barberbook.dto.request.CreateServiceRequestDto;
import com.barberbook.dto.request.UpdateServiceRequestDto;
import com.barberbook.dto.response.ServiceResponseDto;
import com.barberbook.exception.ResourceNotFoundException;
import com.barberbook.mapper.ServizioMapper;
import com.barberbook.repository.ServizioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceCatalogService {

    private final ServizioRepository servizioRepository;
    private final ServizioMapper servizioMapper;

    // -------------------------------------------------------
    // RF_CLI_1 — Vetrina pubblica (lettura, nessun auth)
    // -------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ServiceResponseDto> getAllActive() {
        return servizioMapper.toDtoList(servizioRepository.findByAttivoTrue());
    }

    @Transactional(readOnly = true)
    public ServiceResponseDto getById(Long id) {
        Servizio s = servizioRepository.findByIdAndAttivoTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Servizio non trovato: " + id));
        return servizioMapper.toDto(s);
    }

    // -------------------------------------------------------
    // RF_BAR_6 — Creazione servizio
    // -------------------------------------------------------

    public ServiceResponseDto create(CreateServiceRequestDto dto) {
        Servizio s = servizioMapper.toEntity(dto);
        s.setAttivo(true);
        s.setCreatedAt(LocalDateTime.now());
        return servizioMapper.toDto(servizioRepository.save(s));
    }

    // -------------------------------------------------------
    // RF_BAR_7 — Modifica servizio
    // -------------------------------------------------------

    public ServiceResponseDto update(Long id, UpdateServiceRequestDto dto) {
        Servizio s = servizioRepository.findByIdAndAttivoTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Servizio non trovato: " + id));

        if (dto.nome() != null)          s.setNome(dto.nome());
        if (dto.descrizione() != null)   s.setDescrizione(dto.descrizione());
        if (dto.durataMinuti() != null)  s.setDurataMinuti(dto.durataMinuti());
        if (dto.prezzo() != null)        s.setPrezzo(dto.prezzo());
        s.setUpdatedAt(LocalDateTime.now());

        return servizioMapper.toDto(servizioRepository.save(s));
    }

    // -------------------------------------------------------
    // RF_BAR_8 — Eliminazione servizio (soft-delete)
    // -------------------------------------------------------

    public void delete(Long id) {
        Servizio s = servizioRepository.findByIdAndAttivoTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Servizio non trovato: " + id));
        s.setAttivo(false);
        s.setUpdatedAt(LocalDateTime.now());
        servizioRepository.save(s);
    }
}
