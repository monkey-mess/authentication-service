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
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setProfile_picture(request.getProfilePicture());
        return user;
    }

    public UserProfileResponse toProfileResponse(User user) {
        if(user == null) return null;

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getProfile_picture()
        );
    }

    public UserInfoResponse toInfoResponse(User user) {
        if(user == null) return null;

        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setProfilePicture(user.getProfile_picture());
        return response;
    }

    public void updateUserFromRequest(User user, UpdateProfileRequest request) {
        if(request == null) return;

        if(request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }

        if(request.getProfilePicture() != null) {
            user.setProfile_picture(request.getProfilePicture());
        }
    }
}
