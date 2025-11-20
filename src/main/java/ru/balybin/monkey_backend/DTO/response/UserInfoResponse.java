package ru.balybin.monkey_backend.DTO.response;

import java.util.UUID;

public class UserInfoResponse {
    private UUID id;
    private String email;

    public UserInfoResponse() {}

    public UserInfoResponse(UUID id, String email) {
        this.id = id;
        this.email = email;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}


