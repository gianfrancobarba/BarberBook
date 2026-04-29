package com.barberbook.repository;

import com.barberbook.domain.enums.ScheduleType;
import com.barberbook.domain.model.FasciaOraria;
import com.barberbook.domain.model.Poltrona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface FasciaOrariaRepository extends JpaRepository<FasciaOraria, Long> {

    // Fascia di APERTURA per poltrona+giorno (al massimo 1)
    Optional<FasciaOraria> findByPoltronaAndGiornoSettimanaAndTipo(
        Poltrona poltrona, DayOfWeek giorno, ScheduleType tipo);

    // Tutte le fasce (APERTURA + PAUSA) di una poltrona in un giorno
    List<FasciaOraria> findByPoltronaAndGiornoSettimana(
        Poltrona poltrona, DayOfWeek giorno);

    // Solo le PAUSE di una poltrona in un giorno
    List<FasciaOraria> findByPoltronaAndGiornoSettimanaAndTipo(
        Poltrona poltrona, DayOfWeek giorno, ScheduleType tipo);

    // Tutte le fasce di una poltrona (per configurazione BAR)
    List<FasciaOraria> findByPoltrona(Poltrona poltrona);
}
