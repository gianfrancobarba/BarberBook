package com.barberbook.service;

import com.barberbook.domain.model.Poltrona;
import com.barberbook.dto.request.CreateChairRequestDto;
import com.barberbook.dto.request.UpdateChairRequestDto;
import com.barberbook.dto.response.ChairResponseDto;
import com.barberbook.exception.ChairNameAlreadyExistsException;
import com.barberbook.exception.ResourceNotFoundException;
import com.barberbook.mapper.PoltronaMapper;
import com.barberbook.repository.PoltronaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChairService {

    private final PoltronaRepository poltronaRepository;
    private final PoltronaMapper poltronaMapper;

    // -------------------------------------------------------
    // RF_CLI_2 — Lista poltrone attive (pubblica)
    // -------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ChairResponseDto> getAllActive() {
        return poltronaMapper.toDtoList(poltronaRepository.findByAttivaTrue());
    }

    @Transactional(readOnly = true)
    public ChairResponseDto getById(Long id) {
        Poltrona p = poltronaRepository.findByIdAndAttivaTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Poltrona non trovata: " + id));
        return poltronaMapper.toDto(p);
    }

    // -------------------------------------------------------
    // RF_BAR_3 — Aggiunta poltrona
    // -------------------------------------------------------

    public ChairResponseDto create(CreateChairRequestDto dto) {
        // Verifica unicità del nome (anche tra poltrone disattivate)
        if (poltronaRepository.existsByNome(dto.nome())) {
            throw new ChairNameAlreadyExistsException(
                "Esiste già una poltrona con il nome: " + dto.nome());
        }
        Poltrona p = poltronaMapper.toEntity(dto);
        p.setAttiva(true);
        p.setCreatedAt(LocalDateTime.now());
        return poltronaMapper.toDto(poltronaRepository.save(p));
    }

    // -------------------------------------------------------
    // RF_BAR_5 — Rinomina poltrona
    // -------------------------------------------------------

    public ChairResponseDto rename(Long id, UpdateChairRequestDto dto) {
        Poltrona p = poltronaRepository.findByIdAndAttivaTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Poltrona non trovata: " + id));

        // Verifica unicità del nuovo nome (solo se cambia effettivamente)
        if (!p.getNome().equals(dto.nome()) && poltronaRepository.existsByNome(dto.nome())) {
            throw new ChairNameAlreadyExistsException(
                "Esiste già una poltrona con il nome: " + dto.nome());
        }

        p.setNome(dto.nome());
        p.setUpdatedAt(LocalDateTime.now());
        return poltronaMapper.toDto(poltronaRepository.save(p));
    }

    // -------------------------------------------------------
    // RF_BAR_4 — Rimozione poltrona (soft-delete)
    // -------------------------------------------------------

    public void deactivate(Long id) {
        Poltrona p = poltronaRepository.findByIdAndAttivaTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Poltrona non trovata: " + id));

        p.setAttiva(false);
        p.setUpdatedAt(LocalDateTime.now());
        poltronaRepository.save(p);
    }
}
