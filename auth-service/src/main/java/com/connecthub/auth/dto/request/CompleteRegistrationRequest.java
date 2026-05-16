package com.connecthub.auth.dto.request;

/**
 * Request to complete OAuth2 registration setup
 * User provides password and desired username
 */
public class CompleteRegistrationRequest {
    private String password;
    private String username;

    public CompleteRegistrationRequest() {}

    public CompleteRegistrationRequest(String password, String username) {
        this.password = password;
        this.username = username;
    }

    // Getters and setters
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
