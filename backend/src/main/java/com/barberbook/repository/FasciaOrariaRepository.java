package com.barberbook.repository;

import com.barberbook.domain.enums.ScheduleType;
import com.barberbook.domain.model.FasciaOraria;
import com.barberbook.domain.model.Poltrona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface FasciaOrariaRepository extends JpaRepository<FasciaOraria, Long> {

    // Fasce (APERTURA o PAUSA) di una poltrona in un giorno
    List<FasciaOraria> findByPoltronaAndGiornoSettimanaAndTipo(
        Poltrona poltrona, DayOfWeek giorno, ScheduleType tipo);

    // Tutte le fasce di una poltrona (per configurazione BAR)
    List<FasciaOraria> findByPoltrona(Poltrona poltrona);
}
