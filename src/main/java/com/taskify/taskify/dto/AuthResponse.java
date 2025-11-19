package com.taskify.taskify.dto;

public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";

    public AuthResponse() {}

    public AuthResponse(String token) {
        this.token = token;
    }

    public AuthResponse(String token, String tokenType) {
        this.token = token;
        this.tokenType = tokenType;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
