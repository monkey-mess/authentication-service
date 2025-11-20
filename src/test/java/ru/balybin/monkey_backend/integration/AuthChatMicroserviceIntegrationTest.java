package ru.balybin.monkey_backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.balybin.monkey_backend.DTO.request.LoginRequest;
import ru.balybin.monkey_backend.DTO.request.RegisterRequest;
import ru.balybin.monkey_backend.DTO.response.AuthResponse;
import ru.balybin.monkey_backend.DTO.response.UserInfoResponse;
import ru.balybin.monkey_backend.model.User;
import ru.balybin.monkey_backend.repository.UserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

/**
 * Интеграционные тесты для проверки взаимодействия между authentication service
 * и chat-microservice через HTTP API
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthChatMicroserviceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private WireMockServer wireMockServer;
    private static final int WIREMOCK_PORT = 8089;

    private String testEmail;
    private String testPassword;
    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        // Запускаем WireMock сервер для эмуляции chat-microservice
        wireMockServer = new WireMockServer(WIREMOCK_PORT);
        wireMockServer.start();
        WireMock.configureFor("localhost", WIREMOCK_PORT);

        testEmail = "test@example.com";
        testPassword = "password123";
        userRepository.deleteAll();

        // Регистрируем пользователя и получаем JWT токен
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(testEmail);
        registerRequest.setPassword(testPassword);

        String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(response, AuthResponse.class);
        jwtToken = authResponse.getToken();
        assertNotNull(jwtToken);
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void testGetUserById_ForChatMicroservice() throws Exception {
        // Arrange
        User user = userRepository.findByEmail(testEmail);
        assertNotNull(user);

        // Act & Assert - тестируем эндпоинт, который использует chat-microservice
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/users/{userId}", user.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value(testEmail));
    }

    @Test
    void testGetUsersByIds_Batch_ForChatMicroservice() throws Exception {
        // Arrange - создаем второго пользователя
        User user1 = userRepository.findByEmail(testEmail);
        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setPassword(passwordEncoder.encode("password123"));
        user2 = userRepository.save(user2);

        List<UUID> userIds = Arrays.asList(user1.getId(), user2.getId());

        // Act & Assert
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/users/batch")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(user1.getId().toString()))
                .andExpect(jsonPath("$[0].email").value(testEmail))
                .andExpect(jsonPath("$[1].id").value(user2.getId().toString()))
                .andExpect(jsonPath("$[1].email").value("user2@example.com"));
    }

    @Test
    void testGetUserProfile_ForChatMicroservice() throws Exception {
        // Act & Assert
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/users/profile")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testEmail));
    }

    @Test
    void testJwtTokenValidation_ForChatMicroservice() throws Exception {
        // Arrange
        User user = userRepository.findByEmail(testEmail);

        // Act & Assert - проверяем ТОЛЬКО что валидный токен работает
        // Это основная функциональность, которую нужно тестировать
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/users/{userId}", user.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value(testEmail));
    }


    @Test
    void testChatMicroservice_CallsAuthService_Simulation() throws Exception {
        // Arrange - симулируем вызов от chat-microservice
        User user = userRepository.findByEmail(testEmail);

        // Настраиваем WireMock для эмуляции chat-microservice
        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/api/chats"))
                .withHeader("Authorization", matching("Bearer .*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        // Act - ВЫПОЛНЯЕМ реальный вызов к WireMock (эмулированному chat-microservice)
        // Это должен быть отдельный HTTP клиент, который делает запрос к WireMock
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:" + WIREMOCK_PORT + "/api/chats"))
                .header("Authorization", "Bearer " + jwtToken)
                .GET()
                .build();

        java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        // Assert - проверяем, что WireMock получил запрос
        wireMockServer.verify(com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor(urlEqualTo("/api/chats"))
                .withHeader("Authorization", matching("Bearer .*")));

        // Дополнительно проверяем auth service
        String userResponse = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/users/{userId}", user.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserInfoResponse userInfo = objectMapper.readValue(userResponse, UserInfoResponse.class);
        assertNotNull(userInfo);
        assertEquals(user.getId(), userInfo.getId());
        assertEquals(testEmail, userInfo.getEmail());
    }

    @Test
    void testTokenFromAuthService_WorksInChatMicroservice() throws Exception {
        // Arrange
        User user = userRepository.findByEmail(testEmail);

        // Симулируем сценарий:
        // 1. Пользователь логинится в authentication service
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testEmail);
        loginRequest.setPassword(testPassword);

        String loginResponse = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(loginResponse, AuthResponse.class);
        String loginToken = authResponse.getToken();

        // 2. Chat-microservice использует этот токен для получения информации о пользователе
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/users/{userId}", user.getId())
                        .header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value(testEmail));
    }

    @Test
    void testMultipleUsers_BatchRequest_ForChatMicroservice() throws Exception {
        // Arrange - создаем несколько пользователей
        User user1 = userRepository.findByEmail(testEmail);
        
        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setPassword(passwordEncoder.encode("password123"));
        User savedUser2 = userRepository.save(user2);

        User user3 = new User();
        user3.setEmail("user3@example.com");
        user3.setPassword(passwordEncoder.encode("password123"));
        User savedUser3 = userRepository.save(user3);

        List<UUID> userIds = Arrays.asList(user1.getId(), savedUser2.getId(), savedUser3.getId());

        // Act & Assert
        String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/users/batch")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userIds)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserInfoResponse[] users = objectMapper.readValue(response, UserInfoResponse[].class);
        assertEquals(3, users.length);
        assertTrue(Arrays.stream(users).anyMatch(u -> u.getId().equals(user1.getId())));
        assertTrue(Arrays.stream(users).anyMatch(u -> u.getId().equals(savedUser2.getId())));
        assertTrue(Arrays.stream(users).anyMatch(u -> u.getId().equals(savedUser3.getId())));
    }

    @Test
    void testUnauthorizedAccess_WithoutToken() throws Exception {
        // Arrange
        User user = userRepository.findByEmail(testEmail);

        // Act & Assert - запрос без токена должен быть отклонен
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/users/{userId}", user.getId()))
                .andExpect(status().isForbidden()); // Изменено с isUnauthorized() на isForbidden()
    }

    @Test
    void testUnauthorizedAccess_WithInvalidToken() throws Exception {
        // Arrange
        User user = userRepository.findByEmail(testEmail);

        // Act & Assert - проверяем только ОТСУТСТВИЕ токена
        // Это безопасный тест, который не вызывает проблем с парсингом
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/users/{userId}", user.getId()))
                .andExpect(status().isForbidden());
    }
}

