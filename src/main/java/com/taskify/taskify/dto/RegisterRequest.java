package com.taskify.taskify.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request object for user registration")
public class RegisterRequest {

    @Schema(description = "Username for the new account", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "Email address for the new account", example = "john@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Password for the new account", example = "Password123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    public RegisterRequest() {
    }

    public RegisterRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // getters & setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}