package com.barberbook.domain.model;

import com.barberbook.domain.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "clients")
@DiscriminatorValue("CLIENT")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ClienteRegistrato extends User {

    @Column
    private LocalDateTime emailVerifiedAt;

    @Override
    public String getPasswordHash() {
        return passwordHash;
    }

    @Override
    public UserRole getRuolo() {
        return UserRole.CLIENT;
    }
}
