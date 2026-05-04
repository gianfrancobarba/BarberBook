package com.barberbook.service;

import com.barberbook.domain.model.ClienteRegistrato;
import com.barberbook.domain.model.User;
import com.barberbook.dto.request.UpdateProfileRequestDto;
import com.barberbook.dto.response.UserResponseDto;
import com.barberbook.exception.EmailAlreadyExistsException;
import com.barberbook.mapper.UserMapper;
import com.barberbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service per la gestione del profilo utente.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProfileService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * RF_CLR_6 — Visualizzazione profilo.
     */
    @Transactional(readOnly = true)
    public UserResponseDto getProfile(User user) {
        return userMapper.toDto(user);
    }

    /**
     * RF_CLR_6 — Aggiornamento profilo (solo per Clienti Registrati).
     */
    public UserResponseDto updateProfile(User user, UpdateProfileRequestDto dto) {
        if (user instanceof ClienteRegistrato client) {
            // Verifica unicità nuova email (se è stata fornita e se è diversa dall'attuale)
            if (dto.email() != null && !dto.email().equals(client.getEmail())) {
                if (userRepository.existsByEmail(dto.email())) {
                    throw new EmailAlreadyExistsException("Email già registrata: " + dto.email());
                }
                client.setEmail(dto.email());
            }

            if (dto.nome() != null)     client.setNome(dto.nome());
            if (dto.cognome() != null)  client.setCognome(dto.cognome());
            if (dto.telefono() != null) client.setTelefono(sanitizeTelefono(dto.telefono()));

            client.setUpdatedAt(LocalDateTime.now());
            return userMapper.toDto(userRepository.save(client));
        }
        throw new UnsupportedOperationException("Solo i clienti registrati possono modificare il profilo");
    }

    private String sanitizeTelefono(String telefono) {
        // Rimuove spazi e trattini, mantiene solo cifre e '+'
        return telefono.replaceAll("[\\s\\-]", "");
    }
}
