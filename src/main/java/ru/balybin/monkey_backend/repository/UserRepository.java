package ru.balybin.monkey_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.balybin.monkey_backend.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    public User findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.full_name LIKE %:query% OR u.email LIKE %:query%")
    List<User> searchUsers(@Param("query") String name);
}
