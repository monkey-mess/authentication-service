package ru.balybin.monkey_backend.service;

import ru.balybin.monkey_backend.exception.UserException;
import ru.balybin.monkey_backend.model.User;
import ru.balybin.monkey_backend.request.UpdateUserRequest;

import java.util.List;

public interface UserService {

    public User findUserById(int id) throws UserException;

    public User findUserProfile(String jwt) throws UserException;

    public User updateUser(int user_id, UpdateUserRequest req) throws UserException;

    public List<User> searchUser(String query);
}
