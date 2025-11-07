package ru.balybin.monkey_backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.balybin.monkey_backend.DTO.request.UpdateProfileRequest;
import ru.balybin.monkey_backend.DTO.response.UserInfoResponse;
import ru.balybin.monkey_backend.DTO.response.UserProfileResponse;
import ru.balybin.monkey_backend.model.User;
import ru.balybin.monkey_backend.service.UserService;
import ru.balybin.monkey_backend.config.UserMapper;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(@RequestHeader("Authorization") String jwt) {
        String token = jwt.startsWith("Bearer ") ? jwt.substring(7) : jwt;
        UserProfileResponse user = userService.findUserProfile(token);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse updatedUser = userService.updateUser(userId, request);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String query) {
        List<User> users = userService.searchUser(query);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserInfoResponse> getUserById(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long userId) {
        // Валидация токена происходит через JwtTokenValidator
        User user = userService.findUserById(userId);
        UserInfoResponse userInfo = userMapper.toInfoResponse(user);
        return ResponseEntity.ok(userInfo);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<UserInfoResponse>> getUsersByIds(
            @RequestHeader("Authorization") String jwt,
            @RequestBody List<Long> userIds) {
        // Валидация токена происходит через JwtTokenValidator
        List<UserInfoResponse> users = userIds.stream()
                .map(userId -> {
                    try {
                        User user = userService.findUserById(userId);
                        return userMapper.toInfoResponse(user);
                    } catch (Exception e) {
                        return null; // Или можно выбросить исключение
                    }
                })
                .filter(user -> user != null)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }
}
