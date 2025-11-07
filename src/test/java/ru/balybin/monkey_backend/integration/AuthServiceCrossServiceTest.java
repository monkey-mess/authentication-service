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
    private String testUsername;
    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        testEmail = "test@example.com";
        testPassword = "password123";
        testUsername = "testuser";
        userRepository.deleteAll();

        // Регистрируем пользователя
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(testUsername);
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
        // В реальном сценарии оба сервиса должны использовать один ключ
        
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
    void testUserInfoResponse_MatchesChatMicroserviceFormat() throws Exception {
        // Arrange
        User user = userRepository.findByEmail(testEmail);

        // Act
        String response = mockMvc.perform(get("/api/users/{userId}", user.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert - проверяем структуру ответа
        assertNotNull(response);
        assertTrue(response.contains("\"id\""));
        assertTrue(response.contains("\"username\""));
        assertTrue(response.contains("\"email\""));
        assertTrue(response.contains("\"profilePicture\""));
    }

    @Test
    void testFullFlow_RegisterLoginUseToken() throws Exception {
        // Step 1: Register
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Step 2: Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("newuser@example.com");
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

        // Step 3: Use token to get user info (as chat-microservice would do)
        User user = userRepository.findByEmail("newuser@example.com");
        mockMvc.perform(get("/api/users/{userId}", user.getId())
                        .header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    void testBatchRequest_WithMultipleValidTokens() throws Exception {
        // Arrange - создаем несколько пользователей
        User user1 = userRepository.findByEmail(testEmail);
        
        RegisterRequest registerRequest2 = new RegisterRequest();
        registerRequest2.setUsername("user2");
        registerRequest2.setEmail("user2@example.com");
        registerRequest2.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest2)))
                .andExpect(status().isOk());

        User user2 = userRepository.findByEmail("user2@example.com");

        // Act - симулируем batch запрос от chat-microservice
        String batchRequest = objectMapper.writeValueAsString(
                java.util.Arrays.asList(user1.getId(), user2.getId()));

        mockMvc.perform(post("/api/users/batch")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[1].id").exists());
    }
}

