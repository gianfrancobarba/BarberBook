package com.barberbook.repository;

import com.barberbook.domain.enums.UserRole;
import com.barberbook.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.ruoloDiscriminator = :ruolo")
    List<User> findByRuolo(@org.springframework.data.repository.query.Param("ruolo") UserRole ruolo);
}
