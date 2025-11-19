package com.taskify.taskify.service.impl;

import com.taskify.taskify.dto.AuthResponse;
import com.taskify.taskify.dto.LoginRequest;
import com.taskify.taskify.dto.RegisterRequest;
import com.taskify.taskify.model.Role;
import com.taskify.taskify.model.User;
import com.taskify.taskify.repository.RoleRepository;
import com.taskify.taskify.repository.UserRepository;
import com.taskify.taskify.security.JwtService;
import com.taskify.taskify.service.AuthService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getUsername(), request.getEmail(), encodedPassword);

        Role role = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

        user.getRoles().add(role);

        userRepository.save(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        // 1. Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // 2. Get authenticated principal
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 3. Generate JWT
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(token);
    }


}