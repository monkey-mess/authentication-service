package ru.balybin.monkey_backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.balybin.monkey_backend.DTO.request.UpdateProfileRequest;
import ru.balybin.monkey_backend.DTO.response.UserProfileResponse;
import ru.balybin.monkey_backend.model.User;
import ru.balybin.monkey_backend.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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
}
