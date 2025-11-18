package com.taskify.taskify.service;

import com.taskify.taskify.dto.RegisterRequest;

public interface AuthService {
    void register(RegisterRequest request);
}