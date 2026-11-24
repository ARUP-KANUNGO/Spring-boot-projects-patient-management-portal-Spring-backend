package com.elrics.patientportal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elrics.patientportal.dto.AuthResponse;
import com.elrics.patientportal.dto.LoginRequest;
import com.elrics.patientportal.dto.RegisterRequest;
import com.elrics.patientportal.dto.UserResponse;
import com.elrics.patientportal.entity.User;
import com.elrics.patientportal.exception.RegistrationClosedException;
import com.elrics.patientportal.repository.UserRepository;
import com.elrics.patientportal.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final JwtService jwtService = new JwtService("unit-test-secret-key-with-more-than-32-chars", 60_000);
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    private static RegisterRequest registerRequest() {
        return new RegisterRequest("Dr. Admin", "Admin@Example.com", "admin", "Secret123");
    }

    private User storedUser() {
        User u = new User();
        u.setId(5L);
        u.setFullName("Dr. Admin");
        u.setUsername("admin");
        u.setEmail("admin@example.com");
        u.setPassword(passwordEncoder.encode("Secret123"));
        u.setRole(User.ROLE_ADMIN);
        return u;
    }

    @Test
    void firstRegistrationCreatesAdminWithHashedPassword() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse response = authService.register(registerRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("ADMIN", saved.getRole());
        assertEquals("admin@example.com", saved.getEmail());
        assertNotEquals("Secret123", saved.getPassword());
        assertTrue(passwordEncoder.matches("Secret123", saved.getPassword()));
        assertEquals("admin", response.username());
    }

    @Test
    void registrationIsRejectedOnceAnAdministratorExists() {
        when(userRepository.count()).thenReturn(1L);

        assertThrows(RegistrationClosedException.class, () -> authService.register(registerRequest()));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginWithUsernameOrEmailReturnsToken() {
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenReturn(Optional.of(storedUser()));

        AuthResponse response = authService.login(new LoginRequest("admin", "Secret123"));

        assertEquals("Bearer", response.tokenType());
        assertEquals(5L, jwtService.validateAndGetUserId(response.token()).orElseThrow());
        assertEquals("admin@example.com", response.user().email());
    }

    @Test
    void wrongPasswordIsRejected() {
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenReturn(Optional.of(storedUser()));

        assertThrows(BadCredentialsException.class, () -> authService.login(new LoginRequest("admin", "Wrong123")));
    }

    @Test
    void unknownUserIsRejectedWithSameError() {
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("ghost", "ghost"))
                .thenReturn(Optional.empty());

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("ghost", "Secret123")));
        assertEquals("Invalid username/email or password", ex.getMessage());
    }
}
