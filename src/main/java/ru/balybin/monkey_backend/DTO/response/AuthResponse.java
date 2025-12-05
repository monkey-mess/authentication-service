package ru.balybin.monkey_backend.DTO.response;

import java.util.UUID;

/**
 * Соответствует API.md: userId + accessToken + refreshToken.
 */
public class AuthResponse {
    private UUID userId;
    private String accessToken;
    private String refreshToken;

    public AuthResponse() {}

    public AuthResponse(UUID userId, String accessToken, String refreshToken) {
        this.userId = userId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
