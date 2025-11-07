package ru.balybin.monkey_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.balybin.monkey_backend.exception.UserException;
import ru.balybin.monkey_backend.model.User;
import ru.balybin.monkey_backend.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String testEmail;
    private String testPassword;
    private String testUsername;

    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        testPassword = "password123";
        testUsername = "testuser";
        userRepository.deleteAll();
    }

    @Test
    void testRegisterUser_Integration_Success() {
        // Arrange
        User user = new User();
        user.setEmail(testEmail);
        user.setUsername(testUsername);
        user.setPassword(testPassword);

        // Act
        User savedUser = userService.registerUser(user);

        // Assert
        assertNotNull(savedUser);
        assertNotNull(savedUser.getId());
        assertEquals(testEmail, savedUser.getEmail());
        assertEquals(testUsername, savedUser.getUsername());
        assertNotEquals(testPassword, savedUser.getPassword()); // Password should be encoded
        assertTrue(passwordEncoder.matches(testPassword, savedUser.getPassword()));

        // Verify user exists in database
        User foundUser = userRepository.findByEmail(testEmail);
        assertNotNull(foundUser);
        assertEquals(savedUser.getId(), foundUser.getId());
    }

    @Test
    void testRegisterUser_Integration_DuplicateEmail() {
        // Arrange - create existing user
        User existingUser = new User();
        existingUser.setEmail(testEmail);
        existingUser.setUsername("existinguser");
        existingUser.setPassword(passwordEncoder.encode(testPassword));
        userRepository.save(existingUser);

        User newUser = new User();
        newUser.setEmail(testEmail);
        newUser.setUsername("newuser");
        newUser.setPassword(testPassword);

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () -> {
            userService.registerUser(newUser);
        });

        assertEquals("User already exists", exception.getMessage());
        assertEquals(1, userRepository.count());
    }

    @Test
    void testFindUserByEmail_Integration_Success() {
        // Arrange - create user
        User user = new User();
        user.setEmail(testEmail);
        user.setUsername(testUsername);
        user.setPassword(passwordEncoder.encode(testPassword));
        userRepository.save(user);

        // Act
        User foundUser = userService.findUserByEmail(testEmail);

        // Assert
        assertNotNull(foundUser);
        assertEquals(testEmail, foundUser.getEmail());
        assertEquals(testUsername, foundUser.getUsername());
    }

    @Test
    void testFindUserByEmail_Integration_NotFound() {
        // Act & Assert
        UserException exception = assertThrows(UserException.class, () -> {
            userService.findUserByEmail(testEmail);
        });

        assertEquals("User not found with email: " + testEmail, exception.getMessage());
    }

    @Test
    void testFindUserById_Integration_Success() {
        // Arrange - create user
        User user = new User();
        user.setEmail(testEmail);
        user.setUsername(testUsername);
        user.setPassword(passwordEncoder.encode(testPassword));
        User savedUser = userRepository.save(user);

        // Act
        User foundUser = userService.findUserById(savedUser.getId());

        // Assert
        assertNotNull(foundUser);
        assertEquals(savedUser.getId(), foundUser.getId());
        assertEquals(testEmail, foundUser.getEmail());
        assertEquals(testUsername, foundUser.getUsername());
    }

    @Test
    void testFindUserById_Integration_NotFound() {
        // Act & Assert
        UserException exception = assertThrows(UserException.class, () -> {
            userService.findUserById(999L);
        });

        assertTrue(exception.getMessage().contains("User not found with id"));
    }

    @Test
    void testRegisterAndFindUser_Integration_FullFlow() {
        // Step 1: Register user
        User user = new User();
        user.setEmail(testEmail);
        user.setUsername(testUsername);
        user.setPassword(testPassword);

        User savedUser = userService.registerUser(user);
        assertNotNull(savedUser.getId());

        // Step 2: Find by email
        User foundByEmail = userService.findUserByEmail(testEmail);
        assertEquals(savedUser.getId(), foundByEmail.getId());

        // Step 3: Find by id
        User foundById = userService.findUserById(savedUser.getId());
        assertEquals(savedUser.getId(), foundById.getId());
        assertEquals(testEmail, foundById.getEmail());
    }

    @Test
    void testPasswordEncoding_Integration() {
        // Arrange
        User user = new User();
        user.setEmail(testEmail);
        user.setUsername(testUsername);
        user.setPassword(testPassword);

        // Act
        User savedUser = userService.registerUser(user);

        // Assert
        String encodedPassword = savedUser.getPassword();
        assertNotEquals(testPassword, encodedPassword);
        assertTrue(encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$")); // BCrypt format
        assertTrue(passwordEncoder.matches(testPassword, encodedPassword));
    }
}

