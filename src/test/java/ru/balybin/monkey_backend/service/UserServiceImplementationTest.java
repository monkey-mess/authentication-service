package ru.balybin.monkey_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.balybin.monkey_backend.config.TokenProvider;
import ru.balybin.monkey_backend.config.UserMapper;
import ru.balybin.monkey_backend.exception.UserException;
import ru.balybin.monkey_backend.model.User;
import ru.balybin.monkey_backend.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplementationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImplementation userService;

    private User testUser;
    private String testEmail;
    private String testPassword;

    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        testPassword = "password123";
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail(testEmail);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
    }

    @Test
    void testRegisterUser_Success() {
        // Arrange
        User newUser = new User();
        newUser.setEmail(testEmail);
        newUser.setPassword(testPassword);
        newUser.setUsername("newuser");

        when(userRepository.existsByEmail(testEmail)).thenReturn(false);
        when(passwordEncoder.encode(testPassword)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.registerUser(newUser);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getEmail(), result.getEmail());
        verify(userRepository, times(1)).existsByEmail(testEmail);
        verify(passwordEncoder, times(1)).encode(testPassword);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterUser_UserAlreadyExists() {
        // Arrange
        User newUser = new User();
        newUser.setEmail(testEmail);
        newUser.setPassword(testPassword);

        when(userRepository.existsByEmail(testEmail)).thenReturn(true);

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () -> {
            userService.registerUser(newUser);
        });

        assertEquals("User already exists", exception.getMessage());
        verify(userRepository, times(1)).existsByEmail(testEmail);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testFindUserByEmail_Success() {
        // Arrange
        when(userRepository.findByEmail(testEmail)).thenReturn(testUser);

        // Act
        User result = userService.findUserByEmail(testEmail);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getId(), result.getId());
        verify(userRepository, times(1)).findByEmail(testEmail);
    }

    @Test
    void testFindUserByEmail_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail(testEmail)).thenReturn(null);

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () -> {
            userService.findUserByEmail(testEmail);
        });

        assertEquals("User not found with email: " + testEmail, exception.getMessage());
        verify(userRepository, times(1)).findByEmail(testEmail);
    }

    @Test
    void testFindUserById_Success() {
        // Arrange
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(testUser));

        // Act
        User result = userService.findUserById(userId);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void testFindUserById_UserNotFound() {
        // Arrange
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () -> {
            userService.findUserById(userId);
        });

        assertTrue(exception.getMessage().contains("User not found with id"));
        verify(userRepository, times(1)).findById(userId);
    }
}

