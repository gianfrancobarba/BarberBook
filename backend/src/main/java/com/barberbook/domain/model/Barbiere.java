package com.barberbook.domain.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "barbers")
@DiscriminatorValue("BARBER")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Barbiere extends User {
}
