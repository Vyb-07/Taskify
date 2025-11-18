package com.taskify.taskify.service.impl;

import com.taskify.taskify.dto.RegisterRequest;
import com.taskify.taskify.model.Role;
import com.taskify.taskify.model.User;
import com.taskify.taskify.repository.RoleRepository;
import com.taskify.taskify.repository.UserRepository;
import com.taskify.taskify.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // 1. check username/email availability
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        // 2. create User with encoded password
        String encoded = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getUsername(), request.getEmail(), encoded);

        // 3. assign default role ROLE_USER (create role if missing)
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

        user.getRoles().add(userRole);

        // 4. save user
        userRepository.save(user);
    }
}