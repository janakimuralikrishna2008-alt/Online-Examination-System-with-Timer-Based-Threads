package com.onlineexam.web.security;

import com.onlineexam.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
                "SuperSecretKeyForTestingJwtAuthenticationMustBeAtLeast256BitsLong!",
                3600000
        );
    }

    @Test
    @DisplayName("JWT is generated and correctly decodes username, user ID, and role")
    void testGenerateAndValidateToken() {
        User user = new User(101L, "alice", "hash", "Alice Student", "alice@test.com", User.Role.STUDENT, null, null);
        String token = tokenProvider.generateToken(user);

        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals("alice", tokenProvider.getUsernameFromToken(token));
        assertEquals(101L, tokenProvider.getUserIdFromToken(token));
        assertEquals("STUDENT", tokenProvider.getRoleFromToken(token));
    }

    @Test
    @DisplayName("Invalid or tampered token returns false")
    void testInvalidToken() {
        assertFalse(tokenProvider.validateToken("invalid.token.here"));
        assertFalse(tokenProvider.validateToken(null));
        assertFalse(tokenProvider.validateToken(""));
    }
}
