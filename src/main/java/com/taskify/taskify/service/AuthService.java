package com.taskify.taskify.service;

import com.taskify.taskify.dto.AuthResponse;
import com.taskify.taskify.dto.RegisterRequest;
import com.taskify.taskify.dto.LoginRequest;

public interface AuthService {

    void register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}