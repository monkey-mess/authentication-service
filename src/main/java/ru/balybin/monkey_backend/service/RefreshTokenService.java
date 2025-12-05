package ru.balybin.monkey_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.balybin.monkey_backend.exception.UserException;
import ru.balybin.monkey_backend.model.RefreshToken;
import ru.balybin.monkey_backend.repository.RefreshTokenRepository;

import java.time.Instant;
import java.time.Duration;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration refreshTtl;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               @Value("${auth.refresh.ttl-days:7}") long ttlDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTtl = Duration.ofDays(ttlDays);
    }

    public RefreshToken create(UUID userId) {
        RefreshToken token = new RefreshToken(UUID.randomUUID().toString(), userId,
                Instant.now().plus(refreshTtl));
        return refreshTokenRepository.save(token);
    }

    public RefreshToken validate(String token) {
        RefreshToken stored = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new UserException("Invalid refresh token"));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.deleteByToken(token);
            throw new UserException("Refresh token expired");
        }
        return stored;
    }

    @Transactional
    public void delete(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    @Transactional
    public void deleteAllForUser(UUID userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
    }
}



