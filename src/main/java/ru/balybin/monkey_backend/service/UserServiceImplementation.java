package ru.balybin.monkey_backend.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.balybin.monkey_backend.DTO.request.UpdateProfileRequest;
import ru.balybin.monkey_backend.DTO.response.UserProfileResponse;
import ru.balybin.monkey_backend.config.TokenProvider;
import ru.balybin.monkey_backend.config.UserMapper;
import ru.balybin.monkey_backend.exception.UserException;
import ru.balybin.monkey_backend.model.User;
import ru.balybin.monkey_backend.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImplementation implements UserService {

    private UserRepository userRepository;
    private TokenProvider tokenProvider;
    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;

    public UserServiceImplementation(UserRepository userRepository, TokenProvider tokenProvider,
                                     UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User registerUser(User user) throws UserException {
        if(userRepository.existsByEmail(user.getEmail())) {
            throw new UserException("User already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    @Override
    public User findUserByEmail(String email) throws UserException {
        User user = userRepository.findByEmail(email);
        if(user == null) {
            throw new UserException("User not found with email: " + email);
        }
        return user;
    }

    @Override
    public User findUserById(UUID id) {
        Optional<User> opt = userRepository.findById(id);
        if(opt.isPresent()){
            return opt.get();
        }
        throw new UserException("User not found with id" + id);
    }

    @Override
    public UserProfileResponse findUserProfile(String jwt) {
        String email = tokenProvider.getEmailFromToken(jwt);
        if(email == null){
            throw new BadCredentialsException("Received invalid token");
        }
        User user = findUserByEmail(email);
        return userMapper.toProfileResponse(user);
    }

    @Override
    public UserProfileResponse updateUser(UUID userId, UpdateProfileRequest req) throws UserException {
        User user = findUserById(userId);
        userMapper.updateUserFromRequest(user, req);
        User updatedUser = userRepository.save(user);
        return userMapper.toProfileResponse(updatedUser);
    }

    @Override
    public List<User> searchUser(String query) {
        List<User> users = userRepository.searchUsers(query);
        return users;
    }
}
