package ru.balybin.monkey_backend.DTO.request;

public class UpdateProfileRequest {
    private String email;

    public UpdateProfileRequest() {}

    public UpdateProfileRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
