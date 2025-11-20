package ru.balybin.monkey_backend.DTO.response;

import java.util.UUID;

public class UserProfileResponse {
    private UUID id;
    private String email;

    public UserProfileResponse() {}

    public UserProfileResponse(UUID id, String email) {
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
