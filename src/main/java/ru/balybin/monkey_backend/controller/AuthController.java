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
import ru.balybin.monkey_backend.model.RefreshToken;
import ru.balybin.monkey_backend.service.RefreshTokenService;
import ru.balybin.monkey_backend.service.UserService;
import ru.balybin.monkey_backend.DTO.request.RefreshRequest;
import ru.balybin.monkey_backend.DTO.request.LogoutRequest;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/auth")
public class AuthController  {
    private final UserService userService;
    private final TokenProvider tokenProvider;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public AuthController(UserService userService, TokenProvider tokenProvider,
                          UserMapper userMapper, PasswordEncoder passwordEncoder,
                          RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.tokenProvider = tokenProvider;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userMapper.toEntity(request);
        User savedUser = userService.registerUser(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(savedUser.getEmail(), null);
        String jwt = tokenProvider.generateToken(auth, savedUser.getId());
        RefreshToken refresh = refreshTokenService.create(savedUser.getId());
        AuthResponse authResponse = new AuthResponse(savedUser.getId(), jwt, refresh.getToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.findUserByEmail(request.getEmail());
        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UserException("Wrong password");
        }

        Authentication auth = new UsernamePasswordAuthenticationToken(user.getEmail(), null);

        String jwt = tokenProvider.generateToken(auth, user.getId());
        RefreshToken refresh = refreshTokenService.create(user.getId());
        AuthResponse authResponse = new AuthResponse(user.getId(), jwt, refresh.getToken());

        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshToken existing = refreshTokenService.validate(request.getRefreshToken());
        User user = userService.findUserById(existing.getUserId());

        // rotate refresh
        refreshTokenService.delete(request.getRefreshToken());
        RefreshToken newRefresh = refreshTokenService.create(user.getId());

        Authentication auth = new UsernamePasswordAuthenticationToken(user.getEmail(), null);
        String newAccess = tokenProvider.generateToken(auth, user.getId());

        AuthResponse authResponse = new AuthResponse(user.getId(), newAccess, newRefresh.getToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        refreshTokenService.delete(request.getRefreshToken());
        return ResponseEntity.ok().build();
    }
}
