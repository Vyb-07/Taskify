package com.taskify.taskify.service;

import com.taskify.taskify.dto.AuthResponse;
import com.taskify.taskify.dto.RegisterRequest;
import com.taskify.taskify.dto.LoginRequest;
import com.taskify.taskify.dto.TokenRefreshRequest;

public interface AuthService {

    void register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(TokenRefreshRequest request);

    void logout(String username);
}