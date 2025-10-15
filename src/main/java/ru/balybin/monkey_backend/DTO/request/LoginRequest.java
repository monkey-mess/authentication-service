package ru.balybin.monkey_backend.DTO.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public class LoginRequest {

    @NotNull(message = "Login is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotNull(message = "Password is required")
    private String password;

    public LoginRequest() {}

    public LoginRequest(String login, String password) {
        this.email = login;
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    public String getPassword() {
        return password;
    }
}
