package com.barberbook.service;

import com.barberbook.domain.model.ClienteRegistrato;
import com.barberbook.dto.request.UpdateProfileRequestDto;
import com.barberbook.exception.EmailAlreadyExistsException;
import com.barberbook.mapper.UserMapper;
import com.barberbook.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @InjectMocks private ProfileService profileService;

    @Test
    @DisplayName("updateProfile: aggiorna correttamente i campi consentiti")
    void updateProfile_success() {
        ClienteRegistrato client = new ClienteRegistrato();
        client.setNome("Mario"); client.setEmail("mario@example.com");
        
        UpdateProfileRequestDto dto = new UpdateProfileRequestDto("Luigi", null, null, "3331234567");

        profileService.updateProfile(client, dto);

        assertEquals("Luigi", client.getNome());
        assertEquals("3331234567", client.getTelefono());
        assertEquals("mario@example.com", client.getEmail()); // Invariato
    }

    @Test
    @DisplayName("updateProfile: lancia eccezione se la nuova email è già occupata")
    void updateProfile_duplicateEmail() {
        ClienteRegistrato client = new ClienteRegistrato();
        client.setEmail("mario@example.com");
        
        UpdateProfileRequestDto dto = new UpdateProfileRequestDto(null, null, "occupied@example.com", null);

        when(userRepository.existsByEmail("occupied@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> 
            profileService.updateProfile(client, dto));
    }
}
