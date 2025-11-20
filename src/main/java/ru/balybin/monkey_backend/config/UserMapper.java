package ru.balybin.monkey_backend.config;

import org.springframework.stereotype.Component;
import ru.balybin.monkey_backend.DTO.request.RegisterRequest;
import ru.balybin.monkey_backend.DTO.request.UpdateProfileRequest;
import ru.balybin.monkey_backend.DTO.response.UserInfoResponse;
import ru.balybin.monkey_backend.DTO.response.UserProfileResponse;
import ru.balybin.monkey_backend.model.User;

@Component
public class UserMapper {

    public User toEntity(RegisterRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        return user;
    }

    public UserProfileResponse toProfileResponse(User user) {
        if(user == null) return null;

        return new UserProfileResponse(
                user.getId(),
                user.getEmail()
        );
    }

    public UserInfoResponse toInfoResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserInfoResponse(user.getId(), user.getEmail());
    }

    public void updateUserFromRequest(User user, UpdateProfileRequest req) {
        if (user == null || req == null) {
            return;
        }
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            user.setEmail(req.getEmail());
        }
    }
}
