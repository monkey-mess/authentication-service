package ru.balybin.monkey_backend.DTO.response;

public class AuthResponse {
    private String token;
    private UserProfileResponse userInfo;

    public AuthResponse() {}

    public AuthResponse(String token, UserProfileResponse userInfo) {
        this.token = token;
        this.userInfo = userInfo;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserProfileResponse getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserProfileResponse userInfo) {
        this.userInfo = userInfo;
    }
}
