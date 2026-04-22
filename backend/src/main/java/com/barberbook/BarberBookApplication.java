package com.barberbook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point dell'applicazione BarberBook.
 *
 * @EnableScheduling — abilita il task scheduler per la transizione
 * automatica delle prenotazioni scadute a stato PASSATA (Sprint 5).
 */
@SpringBootApplication
@EnableScheduling
public class BarberBookApplication {

    public static void main(String[] args) {
        SpringApplication.run(BarberBookApplication.class, args);
    }
}
