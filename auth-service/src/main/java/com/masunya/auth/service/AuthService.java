package com.masunya.auth.service;

import com.masunya.auth.dto.LoginRequest;
import com.masunya.auth.dto.RegisterRequest;
import com.masunya.auth.entity.AuthUser;
import com.masunya.auth.repository.AuthUserRepository;
import com.masunya.auth.security.JwtUtil;
import com.masunya.common.enumerate.Role;
import com.masunya.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public String register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already exists");
        }
        AuthUser user = new AuthUser();
        user.setId(UUID.randomUUID());
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CLIENT);
        user.setEnabled(true);
        userRepository.save(user);
        return jwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );
    }

    public String login(LoginRequest request) {
        AuthUser user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(
                        "Invalid credentials",
                        HttpStatus.UNAUTHORIZED
                ));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(
                    "Invalid credentials",
                    HttpStatus.UNAUTHORIZED
            );
        }
        return jwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );
    }
}
