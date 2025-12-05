package ru.balybin.monkey_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.balybin.monkey_backend.DTO.request.LoginRequest;
import ru.balybin.monkey_backend.DTO.request.RegisterRequest;
import ru.balybin.monkey_backend.DTO.response.AuthResponse;
import ru.balybin.monkey_backend.config.TokenProvider;
import ru.balybin.monkey_backend.config.UserMapper;
import ru.balybin.monkey_backend.exception.UserException;
import ru.balybin.monkey_backend.model.User;
import ru.balybin.monkey_backend.model.RefreshToken;
import ru.balybin.monkey_backend.service.UserService;
import ru.balybin.monkey_backend.service.RefreshTokenService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private User testUser;
    private String testEmail;
    private String testPassword;
    private String testAccess;
    private RefreshToken testRefresh;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();

        testEmail = "test@example.com";
        testPassword = "password123";
        testAccess = "test.jwt.token";
        testRefresh = new RefreshToken("refresh.token", UUID.randomUUID(), java.time.Instant.now().plusSeconds(3600));

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail(testEmail);
        testUser.setPassword("encodedPassword");
    }

    @Test
    void testRegister_Success() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(testEmail);
        registerRequest.setPassword(testPassword);

        when(userMapper.toEntity(any(RegisterRequest.class))).thenReturn(testUser);
        when(userService.registerUser(any(User.class))).thenReturn(testUser);
        when(tokenProvider.generateToken(any(), any())).thenReturn(testAccess);
        when(refreshTokenService.create(any(UUID.class))).thenReturn(testRefresh);

        // Act
        ResponseEntity<AuthResponse> response = authController.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(testUser.getId(), response.getBody().getUserId());
        assertEquals(testAccess, response.getBody().getAccessToken());
        assertNotNull(response.getBody().getRefreshToken());

        verify(userMapper, times(1)).toEntity(any(RegisterRequest.class));
        verify(userService, times(1)).registerUser(any(User.class));
        verify(tokenProvider, times(1)).generateToken(any(Authentication.class), any(UUID.class));
    }

    @Test
    void testRegister_WithMockMvc() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(testEmail);
        registerRequest.setPassword(testPassword);

        when(userMapper.toEntity(any(RegisterRequest.class))).thenReturn(testUser);
        when(userService.registerUser(any(User.class))).thenReturn(testUser);
        when(tokenProvider.generateToken(any(), any())).thenReturn(testAccess);
        when(refreshTokenService.create(any(UUID.class))).thenReturn(testRefresh);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.accessToken").value(testAccess))
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void testLogin_Success() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testEmail);
        loginRequest.setPassword(testPassword);

        when(userService.findUserByEmail(testEmail)).thenReturn(testUser);
        when(passwordEncoder.matches(testPassword, testUser.getPassword())).thenReturn(true);
        when(tokenProvider.generateToken(any(), any())).thenReturn(testAccess);
        when(refreshTokenService.create(any(UUID.class))).thenReturn(testRefresh);

        // Act
        ResponseEntity<AuthResponse> response = authController.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(testUser.getId(), response.getBody().getUserId());
        assertEquals(testAccess, response.getBody().getAccessToken());
        assertNotNull(response.getBody().getRefreshToken());

        verify(userService, times(1)).findUserByEmail(testEmail);
        verify(passwordEncoder, times(1)).matches(testPassword, testUser.getPassword());
        verify(tokenProvider, times(1)).generateToken(any(Authentication.class), any(UUID.class));
    }

    @Test
    void testLogin_WrongPassword() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testEmail);
        loginRequest.setPassword("wrongPassword");

        when(userService.findUserByEmail(testEmail)).thenReturn(testUser);
        when(passwordEncoder.matches("wrongPassword", testUser.getPassword())).thenReturn(false);

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () -> {
            authController.login(loginRequest);
        });

        assertEquals("Wrong password", exception.getMessage());
        verify(userService, times(1)).findUserByEmail(testEmail);
        verify(passwordEncoder, times(1)).matches("wrongPassword", testUser.getPassword());
        verify(tokenProvider, never()).generateToken(any(Authentication.class), any(UUID.class));
    }

    @Test
    void testLogin_WithMockMvc() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testEmail);
        loginRequest.setPassword(testPassword);

        when(userService.findUserByEmail(testEmail)).thenReturn(testUser);
        when(passwordEncoder.matches(testPassword, testUser.getPassword())).thenReturn(true);
        when(tokenProvider.generateToken(any(Authentication.class), any(UUID.class))).thenReturn(testAccess);
        when(refreshTokenService.create(any(UUID.class))).thenReturn(testRefresh);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.accessToken").value(testAccess))
                .andExpect(jsonPath("$.refreshToken").exists());
    }
}

