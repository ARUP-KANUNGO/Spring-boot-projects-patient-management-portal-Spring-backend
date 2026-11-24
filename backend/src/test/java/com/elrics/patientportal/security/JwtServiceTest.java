package com.elrics.patientportal.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.elrics.patientportal.entity.User;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-with-more-than-32-chars";

    private static User user(long id) {
        User u = new User();
        u.setId(id);
        u.setEmail("admin@example.com");
        u.setRole(User.ROLE_ADMIN);
        return u;
    }

    @Test
    void generatedTokenIsValidAndCarriesUserId() {
        JwtService service = new JwtService(SECRET, 60_000);
        String token = service.generateToken(user(42));
        assertEquals(42L, service.validateAndGetUserId(token).orElseThrow());
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService service = new JwtService(SECRET, 60_000);
        String token = service.generateToken(user(1));
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("abc") ? "xyz" : "abc");
        assertTrue(service.validateAndGetUserId(tampered).isEmpty());
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = new JwtService("another-secret-key-with-more-than-32-chars!!", 60_000).generateToken(user(1));
        assertTrue(new JwtService(SECRET, 60_000).validateAndGetUserId(token).isEmpty());
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService service = new JwtService(SECRET, -1_000);
        assertTrue(service.validateAndGetUserId(service.generateToken(user(1))).isEmpty());
    }

    @Test
    void garbageTokenIsRejected() {
        assertTrue(new JwtService(SECRET, 60_000).validateAndGetUserId("not-a-jwt").isEmpty());
    }

    @Test
    void shortSecretIsRefused() {
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 60_000));
    }
}
