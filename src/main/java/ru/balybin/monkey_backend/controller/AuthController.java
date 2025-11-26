package ru.balybin.monkey_backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import ru.balybin.monkey_backend.DTO.request.LoginRequest;
import ru.balybin.monkey_backend.DTO.request.RegisterRequest;
import ru.balybin.monkey_backend.DTO.response.AuthResponse;
import ru.balybin.monkey_backend.config.TokenProvider;
import ru.balybin.monkey_backend.config.UserMapper;
import ru.balybin.monkey_backend.exception.UserException;
import ru.balybin.monkey_backend.model.User;
import ru.balybin.monkey_backend.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController  {
    private final UserService userService;
    private final TokenProvider tokenProvider;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, TokenProvider tokenProvider,
                          UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.tokenProvider = tokenProvider;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userMapper.toEntity(request);
        User savedUser = userService.registerUser(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(savedUser.getEmail(), null);
        String jwt = tokenProvider.generateToken(auth, savedUser.getId());
        AuthResponse authResponse = new AuthResponse(jwt, userMapper.toProfileResponse(savedUser));
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.findUserByEmail(request.getEmail());
        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UserException("Wrong password");
        }

        Authentication auth = new UsernamePasswordAuthenticationToken(user.getEmail(), null);

        String jwt = tokenProvider.generateToken(auth, user.getId());
        AuthResponse authResponse = new AuthResponse(jwt, userMapper.toProfileResponse(user));

        return ResponseEntity.ok(authResponse);
    }
}
