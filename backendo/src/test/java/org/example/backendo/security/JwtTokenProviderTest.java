package org.example.backendo.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "superSecretKeyForUnitTestingWithMoreThan256BitsLength1234567890";
    private final long expirationMs = 3600000; // 1 ora

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, expirationMs);
    }

    @Test
    void generateAndValidateToken_success() {
        String email = "ospite@example.com";
        Long userId = 42L;
        String role = "ROLE_GUEST";

        String token = jwtTokenProvider.generateToken(email, userId, role);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtTokenProvider.validateToken(token));

        assertEquals(email, jwtTokenProvider.getEmailFromToken(token));
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token));
        assertEquals(role, jwtTokenProvider.getRoleFromToken(token));
    }

    @Test
    void validateToken_withTamperedToken_returnsFalse() {
        String token = jwtTokenProvider.generateToken("user@example.com", 1L, "ROLE_GUEST");
        String tamperedToken = token + "corrupted";

        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
    }

    @Test
    void validateToken_withExpiredToken_returnsFalse() throws InterruptedException {
        // Provider con scadenza di 1 millisecondo
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(secret, 1);
        String token = shortLivedProvider.generateToken("user@example.com", 1L, "ROLE_GUEST");

        Thread.sleep(10); // Aspetta che scada

        assertFalse(shortLivedProvider.validateToken(token));
    }
}
