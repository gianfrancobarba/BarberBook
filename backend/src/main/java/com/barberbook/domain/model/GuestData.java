package com.barberbook.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuestData {
    @Column(name = "guest_nome")
    private String nome;

    @Column(name = "guest_cognome")
    private String cognome;

    @Column(name = "guest_telefono")
    private String telefono;
}
