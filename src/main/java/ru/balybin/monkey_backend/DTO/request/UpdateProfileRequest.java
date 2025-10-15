package ru.balybin.monkey_backend.DTO.request;

public class UpdateProfileRequest {
    private String username;
    private String profilePicture;

    public UpdateProfileRequest() {}

    public UpdateProfileRequest(String username, String profilePicture) {
        this.username = username;
        this.profilePicture = profilePicture;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    public String getUsername() {
        return username;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
    public String getProfilePicture() {
        return profilePicture;
    }
}
