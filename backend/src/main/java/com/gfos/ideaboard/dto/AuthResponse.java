package com.gfos.ideaboard.dto;

public class AuthResponse {

    private String token;
    private String refreshToken;
    private UserDTO user;
    private long expiresIn;

    public AuthResponse() {}

    public AuthResponse(String token, String refreshToken, UserDTO user, long expiresIn) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.user = user;
        this.expiresIn = expiresIn;
    }

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

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
