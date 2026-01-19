package com.taskify.taskify.service.impl;

import com.taskify.taskify.dto.AuthResponse;
import com.taskify.taskify.dto.LoginRequest;
import com.taskify.taskify.dto.RegisterRequest;
import com.taskify.taskify.dto.TokenRefreshRequest;
import com.taskify.taskify.exception.TokenException;
import com.taskify.taskify.model.Role;
import com.taskify.taskify.model.User;
import com.taskify.taskify.repository.RoleRepository;
import com.taskify.taskify.repository.UserRepository;
import com.taskify.taskify.security.JwtService;
import com.taskify.taskify.security.SecurityConstants;
import com.taskify.taskify.service.AuthService;
import com.taskify.taskify.service.RefreshTokenService;
import com.taskify.taskify.service.AuditService;
import com.taskify.taskify.model.AuditAction;
import com.taskify.taskify.model.AuditTargetType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    public AuthServiceImpl(UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            AuditService auditService) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
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

        Role role = roleRepository.findByName(SecurityConstants.ROLE_USER)
                .orElseGet(() -> roleRepository.save(new Role(SecurityConstants.ROLE_USER)));

        user.getRoles().add(role);

        userRepository.save(user);

        auditService.logEvent(AuditAction.LOGIN_SUCCESS, AuditTargetType.USER,
                user.getId().toString(), user.getUsername(), java.util.Map.of("action", "registration"));
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        // 1. Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()));

        // 2. Get authenticated principal
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 3. Generate JWT
        String token = jwtService.generateToken(userDetails);

        // 4. Generate Refresh Token
        var refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername());

        auditService.logEvent(AuditAction.LOGIN_SUCCESS, AuditTargetType.AUTH, null, userDetails.getUsername(), null);

        return new AuthResponse(token, refreshToken.getToken());
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        return refreshTokenService.findByToken(request.getRefreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(refreshToken -> {
                    User user = refreshToken.getUser();
                    UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                            .username(user.getUsername())
                            .password(user.getPassword())
                            .authorities(user.getRoles().stream()
                                    .map(role -> new SimpleGrantedAuthority(role.getName()))
                                    .collect(Collectors.toList()))
                            .build();
                    String token = jwtService.generateToken(userDetails);

                    auditService.logEvent(AuditAction.TOKEN_REFRESH, AuditTargetType.AUTH, null, user.getUsername(),
                            null);

                    return new AuthResponse(token, refreshToken.getToken());
                })
                .orElseThrow(() -> new TokenException("Refresh token is not in database!"));
    }

    @Override
    @Transactional
    public void logout(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        refreshTokenService.deleteByUserId(user.getId());

        auditService.logEvent(AuditAction.LOGOUT, AuditTargetType.AUTH, null, username, null);
    }

}