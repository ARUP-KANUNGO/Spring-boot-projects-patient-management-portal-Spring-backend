package com.elrics.patientportal.service;

import com.elrics.patientportal.dto.AuthResponse;
import com.elrics.patientportal.dto.LoginRequest;
import com.elrics.patientportal.dto.RegisterRequest;
import com.elrics.patientportal.dto.UserResponse;
import com.elrics.patientportal.entity.User;
import com.elrics.patientportal.exception.RegistrationClosedException;
import com.elrics.patientportal.exception.ResourceNotFoundException;
import com.elrics.patientportal.repository.UserRepository;
import com.elrics.patientportal.security.JwtService;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    /** Compared against when the user does not exist, so response time does not reveal valid accounts. */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("timing-attack-dummy-password");
    }

    /**
     * Creates the single administrator. Intentionally not @Transactional: save() commits before the lock is released,
     * so concurrent requests in this JVM cannot both pass the check. The UNIQUE admin_guard column enforces the same
     * rule inside MySQL for multi-instance deployments.
     */
    public synchronized UserResponse register(RegisterRequest request) {
        if (userRepository.count() > 0) {
            throw new RegistrationClosedException();
        }
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        user.setUsername(request.username().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(User.ROLE_ADMIN);
        try {
            return UserResponse.from(userRepository.save(user));
        } catch (DataIntegrityViolationException ex) {
            if (userRepository.count() > 0) {
                throw new RegistrationClosedException();
            }
            throw ex;
        }
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.username().trim();
        Optional<User> found = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier);
        String hash = found.map(User::getPassword).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        if (found.isEmpty() || !passwordMatches) {
            throw new BadCredentialsException("Invalid username/email or password");
        }
        User user = found.get();
        return new AuthResponse(jwtService.generateToken(user), "Bearer",
                jwtService.getExpirationMs() / 1000, UserResponse.from(user));
    }

    public UserResponse getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
