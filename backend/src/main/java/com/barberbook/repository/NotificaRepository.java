package com.barberbook.repository;

import com.barberbook.domain.model.Notifica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository per la gestione delle notifiche persistite.
 */
@Repository
public interface NotificaRepository extends JpaRepository<Notifica, Long> {

    List<Notifica> findByDestinatarioIdOrderByCreatedAtDesc(Long userId);

    long countByDestinatarioIdAndLettaFalse(Long userId);  // per badge non lette

    @Modifying
    @Query("UPDATE Notifica n SET n.letta = true WHERE n.destinatario.id = :userId")
    void markAllAsReadForUser(@Param("userId") Long userId);
}
