package com.barberbook.repository;

import com.barberbook.domain.model.Notifica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository per la gestione delle notifiche persistite.
 */
@Repository
public interface NotificaRepository extends JpaRepository<Notifica, Long> {
}
