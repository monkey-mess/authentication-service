package ru.balybin.monkey_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.balybin.monkey_backend.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByToken(String token);
    void deleteAllByUserId(UUID userId);
}



