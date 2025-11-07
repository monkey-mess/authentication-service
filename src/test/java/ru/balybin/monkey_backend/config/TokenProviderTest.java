package ru.balybin.monkey_backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;

class TokenProviderTest {

    private TokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new TokenProvider();
    }

    @Test
    void testGenerateToken_Success() {
        // Arrange
        Authentication auth = new UsernamePasswordAuthenticationToken("test@example.com", null);

        // Act
        String token = tokenProvider.generateToken(auth);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testGetEmailFromToken_Success() {
        // Arrange
        String email = "test@example.com";
        Authentication auth = new UsernamePasswordAuthenticationToken(email, null);
        String token = tokenProvider.generateToken(auth);

        // Act
        String extractedEmail = tokenProvider.getEmailFromToken(token);

        // Assert
        assertNotNull(extractedEmail);
        assertEquals(email, extractedEmail);
    }

    @Test
    void testGetEmailFromToken_WithBearerPrefix() {
        // Arrange
        String email = "test@example.com";
        Authentication auth = new UsernamePasswordAuthenticationToken(email, null);
        String token = tokenProvider.generateToken(auth);
        String tokenWithBearer = "Bearer " + token;

        // Act
        String extractedEmail = tokenProvider.getEmailFromToken(tokenWithBearer);

        // Assert
        assertNotNull(extractedEmail);
        assertEquals(email, extractedEmail);
    }

    @Test
    void testTokenContainsEmail() {
        // Arrange
        String email = "user@test.com";
        Authentication auth = new UsernamePasswordAuthenticationToken(email, null);

        // Act
        String token = tokenProvider.generateToken(auth);
        String extractedEmail = tokenProvider.getEmailFromToken(token);

        // Assert
        assertEquals(email, extractedEmail);
    }

    @Test
    void testGenerateToken_DifferentEmails() {
        // Arrange
        String email1 = "user1@test.com";
        String email2 = "user2@test.com";
        Authentication auth1 = new UsernamePasswordAuthenticationToken(email1, null);
        Authentication auth2 = new UsernamePasswordAuthenticationToken(email2, null);

        // Act
        String token1 = tokenProvider.generateToken(auth1);
        String token2 = tokenProvider.generateToken(auth2);

        // Assert
        assertNotEquals(token1, token2);
        assertEquals(email1, tokenProvider.getEmailFromToken(token1));
        assertEquals(email2, tokenProvider.getEmailFromToken(token2));
    }
}

