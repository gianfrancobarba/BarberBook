package com.barberbook.repository;

import com.barberbook.domain.model.Servizio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServizioRepository extends JpaRepository<Servizio, Long> {

    // Tutti i servizi attivi (vetrina pubblica)
    List<Servizio> findByAttivoTrue();

    // Verifica unicità del nome tra i servizi attivi
    boolean existsByNomeAndAttivoTrue(String nome);

    // Singolo servizio attivo
    Optional<Servizio> findByIdAndAttivoTrue(Long id);
}
