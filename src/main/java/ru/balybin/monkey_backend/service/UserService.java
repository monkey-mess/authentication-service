package ru.balybin.monkey_backend.service;

import ru.balybin.monkey_backend.DTO.response.UserProfileResponse;
import ru.balybin.monkey_backend.exception.UserException;
import ru.balybin.monkey_backend.model.User;
import ru.balybin.monkey_backend.DTO.request.UpdateProfileRequest;

import java.util.List;
import java.util.UUID;

public interface UserService {
    public User registerUser(User user) throws UserException;

    public User findUserByEmail(String email) throws UserException;

    public User findUserById(UUID id) throws UserException;

    public UserProfileResponse findUserProfile(String jwt) throws UserException;

    public UserProfileResponse updateUser(UUID user_id, UpdateProfileRequest req) throws UserException;

    public List<User> searchUser(String query);
}
