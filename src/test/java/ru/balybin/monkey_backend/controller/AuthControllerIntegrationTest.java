package ru.balybin.monkey_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import ru.balybin.monkey_backend.model.User;
import ru.balybin.monkey_backend.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String testEmail;
    private String testPassword;

    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        testPassword = "password123";
        userRepository.deleteAll();
    }

    @Test
    void testRegister_Integration_Success() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(testEmail);
        registerRequest.setPassword(testPassword);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.userInfo.email").value(testEmail));

        // Verify user was saved in database
        User savedUser = userRepository.findByEmail(testEmail);
        assertNotNull(savedUser);
        assertEquals(testEmail, savedUser.getEmail());
        assertTrue(passwordEncoder.matches(testPassword, savedUser.getPassword()));
    }

    @Test
    void testRegister_Integration_DuplicateEmail() throws Exception {
        // Arrange - create existing user
        User existingUser = new User();
        existingUser.setEmail(testEmail);
        existingUser.setPassword(passwordEncoder.encode(testPassword));
        userRepository.save(existingUser);

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(testEmail);
        registerRequest.setPassword("newpassword123");

        // Act & Assert - Change from isInternalServerError() to isBadRequest()
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest()); // Changed from isInternalServerError()

        // Verify only one user exists
        assertEquals(1, userRepository.count());
    }

    @Test
    void testLogin_Integration_Success() throws Exception {
        // Arrange - create user
        User user = new User();
        user.setEmail(testEmail);
        user.setPassword(passwordEncoder.encode(testPassword));
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testEmail);
        loginRequest.setPassword(testPassword);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.userInfo.email").value(testEmail));
    }

    @Test
    void testLogin_Integration_WrongPassword() throws Exception {
        // Arrange - create user
        User user = new User();
        user.setEmail(testEmail);
        user.setPassword(passwordEncoder.encode(testPassword));
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testEmail);
        loginRequest.setPassword("wrongPassword");

        // Act & Assert - Change from isInternalServerError() to isBadRequest()
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest()); // Changed from isInternalServerError()
    }


    @Test
    void testLogin_Integration_UserNotFound() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nonexistent@example.com");
        loginRequest.setPassword(testPassword);

        // Act & Assert - Change from isInternalServerError() to isBadRequest()
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest()); // Changed from isInternalServerError()
    }

    @Test
    void testRegisterAndLogin_Integration_FullFlow() throws Exception {
        // Step 1: Register
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(testEmail);
        registerRequest.setPassword(testPassword);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        // Step 2: Login with registered credentials
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testEmail);
        loginRequest.setPassword(testPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.userInfo.email").value(testEmail));

        // Verify user exists
        User savedUser = userRepository.findByEmail(testEmail);
        assertNotNull(savedUser);
        assertEquals(testEmail, savedUser.getEmail());
    }

    @Test
    void testRegister_Integration_InvalidEmail() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("invalid-email");
        registerRequest.setPassword(testPassword);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegister_Integration_ShortPassword() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(testEmail);
        registerRequest.setPassword("short"); // Less than 8 characters

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }
}

