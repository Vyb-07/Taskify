package com.taskify.taskify.service.impl;

import com.taskify.taskify.model.Role;
import com.taskify.taskify.model.User;
import com.taskify.taskify.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    // Constructor injection (Spring will auto-wire)
    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads a user by username (used by Spring Security during authentication).
     *
     * This method converts your User entity into Spring Security's UserDetails.
     * If a user is not found, it throws UsernameNotFoundException which Spring handles.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Fetch user entity from DB
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // 2. Convert domain roles into GrantedAuthority collection
        Collection<GrantedAuthority> authorities = mapRolesToAuthorities(user.getRoles());

        // 3. Build and return Spring Security User (implements UserDetails)
        //    Note: we return username, password (already encoded), and authorities.
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }

    private Collection<GrantedAuthority> mapRolesToAuthorities(Collection<Role> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }
}