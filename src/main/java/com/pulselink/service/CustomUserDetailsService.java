package com.pulselink.service;

import com.pulselink.model.User;
import com.pulselink.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            throw new UsernameNotFoundException("Email cannot be empty");
        }
        String cleanEmail = username.trim();
        User user = userRepository.findByEmail(cleanEmail)
                .orElseGet(() -> userRepository.findByEmail(cleanEmail.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username)));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new UsernameNotFoundException("User account is inactive: " + username);
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getName()))
        );
    }
}
