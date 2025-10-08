package ru.balybin.monkey_backend.service;

import org.springframework.security.authentication.BadCredentialsException;
import ru.balybin.monkey_backend.config.TokenProvider;
import ru.balybin.monkey_backend.exception.UserException;
import ru.balybin.monkey_backend.model.User;
import ru.balybin.monkey_backend.repository.UserRepository;
import ru.balybin.monkey_backend.request.UpdateUserRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserServiceImplementation implements UserService {

    private UserRepository userRepository;
    private TokenProvider tokenProvider;

    public UserServiceImplementation(UserRepository userRepository, TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public User findUserById(int id) {
        Optional<User> opt = userRepository.findById((long) id);
        if(opt.isPresent()){
            return opt.get();
        }
        throw new UserException("User not found with id" + id);
    }

    @Override
    public User findUserProfile(String jwt) {
        String email = tokenProvider.getEmailFromToken(jwt);

        if(email == null){
            throw new BadCredentialsException("Received invalid token");
        }
        User user = userRepository.findByEmail(email);

        if(user == null){
            throw new UserException("User not found with email" + email);
        }
        return user;
    }

    @Override
    public User updateUser(int userid, UpdateUserRequest req) throws UserException {
        User user = findUserById(userid);

        if(req.getFull_name() != null){
            user.setFull_name(req.getFull_name());
        }
        if(req.getProfile_picture() != null){
            user.setProfile_picture(req.getProfile_picture());
        }
        return userRepository.save(user);
    }

    @Override
    public List<User> searchUser(String query) {
        List<User> users = userRepository.searchUsers(query);
        return users;
    }
}
