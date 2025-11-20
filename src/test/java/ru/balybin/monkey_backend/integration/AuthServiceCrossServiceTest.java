package ru.balybin.monkey_backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.balybin.monkey_backend.DTO.request.LoginRequest;
import ru.balybin.monkey_backend.DTO.request.RegisterRequest;
import ru.balybin.monkey_backend.DTO.response.AuthResponse;
import ru.balybin.monkey_backend.config.TokenProvider;
import ru.balybin.monkey_backend.model.User;
import ru.balybin.monkey_backend.repository.UserRepository;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для проверки совместимости JWT токенов
 * между authentication service и chat-microservice
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthServiceCrossServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String testEmail;
    private String testPassword;
    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        testEmail = "test@example.com";
        testPassword = "password123";
        userRepository.deleteAll();

        // Регистрируем пользователя
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(testEmail);
        registerRequest.setPassword(testPassword);

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(response, AuthResponse.class);
        jwtToken = authResponse.getToken();
    }

    @Test
    void testJwtTokenStructure_CompatibleWithChatMicroservice() {
        // Arrange & Act
        assertNotNull(jwtToken);

        // Assert - проверяем, что токен содержит email (как ожидает chat-microservice)
        String emailFromToken = tokenProvider.getEmailFromToken(jwtToken);
        assertNotNull(emailFromToken);
        assertEquals(testEmail, emailFromToken);
    }

    @Test
    void testTokenCanBeValidated_ByChatMicroservice() {
        // Arrange
        // Chat-microservice использует тот же SECRET_KEY для валидации

        // Act - извлекаем email из токена (как это делает chat-microservice)
        String email = tokenProvider.getEmailFromToken(jwtToken);

        // Assert
        assertNotNull(email);
        assertEquals(testEmail, email);
    }

    @Test
    void testTokenWithBearerPrefix_IsHandledCorrectly() {
        // Arrange
        String tokenWithBearer = "Bearer " + jwtToken;

        // Act
        String email = tokenProvider.getEmailFromToken(tokenWithBearer);

        // Assert
        assertEquals(testEmail, email);
    }

    @Test
    void testUserInfoResponse_MatchesActualModel() throws Exception {
        // Arrange
        User user = userRepository.findByEmail(testEmail);
        assertNotNull(user, "User should be found in repository");

        // Act & Assert - проверяем структуру ответа согласно фактической модели User
        mockMvc.perform(get("/api/users/{userId}", user.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value(testEmail));
        // Note: password исключен из JSON благодаря @JsonIgnore
    }

    @Test
    void testFullFlow_RegisterLoginUseToken() throws Exception {
        // Step 1: Register
        String newUserEmail = "newuser@example.com";
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(newUserEmail);
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Step 2: Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(newUserEmail);
        loginRequest.setPassword("password123");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse loginAuthResponse = objectMapper.readValue(loginResponse, AuthResponse.class);
        String loginToken = loginAuthResponse.getToken();

        // Step 3: Use token to get user info
        User user = userRepository.findByEmail(newUserEmail);
        assertNotNull(user, "User should be found after registration");

        mockMvc.perform(get("/api/users/{userId}", user.getId())
                        .header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value(newUserEmail))
                .andExpect(jsonPath("$.id").value(user.getId().toString()));
    }

    @Test
    void testBatchRequest_WithMultipleValidTokens() throws Exception {
        // Arrange - создаем несколько пользователей
        User user1 = userRepository.findByEmail(testEmail);
        assertNotNull(user1, "First user should exist");

        // Create second user
        String secondUserEmail = "user2@example.com";
        RegisterRequest registerRequest2 = new RegisterRequest();
        registerRequest2.setEmail(secondUserEmail);
        registerRequest2.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest2)))
                .andExpect(status().isOk());

        User user2 = userRepository.findByEmail(secondUserEmail);
        assertNotNull(user2, "Second user should exist");

        // Act & Assert - симулируем batch запрос от chat-microservice
        List<String> userIds = Arrays.asList(
                user1.getId().toString(),
                user2.getId().toString()
        );

        String batchRequest = objectMapper.writeValueAsString(userIds);

        mockMvc.perform(post("/api/users/batch")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(user1.getId().toString()))
                .andExpect(jsonPath("$[0].email").value(testEmail))
                .andExpect(jsonPath("$[1].id").value(user2.getId().toString()))
                .andExpect(jsonPath("$[1].email").value(secondUserEmail));
    }

    // Additional test to verify token contains necessary claims
    @Test
    void testJwtTokenContainsRequiredClaims() {
        // Act
        String email = tokenProvider.getEmailFromToken(jwtToken);

        // Assert
        assertNotNull(email);
        assertEquals(testEmail, email);

        // You might want to add more claim validations here if needed
        // For example, if your chat-microservice needs specific claims
    }

    @Test
    void testInvalidToken_ReturnsUnauthorized() throws Exception {
        // Arrange
        String invalidToken = "invalid.token.here";
        User user = userRepository.findByEmail(testEmail);

        // Act & Assert
        mockMvc.perform(get("/api/users/{userId}", user.getId())
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }
}