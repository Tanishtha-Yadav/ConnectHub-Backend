package com.connecthub.auth.dto.response;

public class LoginResponse {

    private String token;
    private String refreshToken;
    private String message;
    private UserResponse user;

    // Constructors
    public LoginResponse() {
    }

    public LoginResponse(String token, String refreshToken, String message, UserResponse user) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.message = message;
        this.user = user;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }
}

