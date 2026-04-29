package com.barberbook.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class GuestData {
    @Column(name = "guest_nome")
    private String nome;

    @Column(name = "guest_cognome")
    private String cognome;

    @Column(name = "guest_telefono")
    private String telefono;

    public GuestData() {}

    public GuestData(String nome, String cognome, String telefono) {
        this.nome = nome;
        this.cognome = cognome;
        this.telefono = telefono;
    }

    public String getNome() { return nome; }
    public String getCognome() { return cognome; }
    public String getTelefono() { return telefono; }

    public void setNome(String nome) { this.nome = nome; }
    public void setCognome(String cognome) { this.cognome = cognome; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}
