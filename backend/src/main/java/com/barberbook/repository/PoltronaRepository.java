package com.barberbook.repository;

import com.barberbook.domain.model.Poltrona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PoltronaRepository extends JpaRepository<Poltrona, Long> {

    // Tutte le poltrone attive (vetrina pubblica + prenotazione)
    List<Poltrona> findByAttivaTrue();

    // Verifica unicità nome tra tutte le poltrone (attive e non)
    boolean existsByNome(String nome);

    // Poltrona attiva per ID
    Optional<Poltrona> findByIdAndAttivaTrue(Long id);
}
