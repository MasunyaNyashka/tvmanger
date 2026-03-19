package com.masunya.auth.controller;

import com.masunya.auth.dto.AuthResponse;
import com.masunya.auth.dto.LoginRequest;
import com.masunya.auth.dto.RegisterRequest;
import com.masunya.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        // Проксируем регистрацию в сервис и возвращаем JWT в стандартном DTO.
        String token = authService.register(request);
        return new AuthResponse(token);
    }
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        // Проксируем логин в сервис и возвращаем JWT в стандартном DTO.
        String token = authService.login(request);
        return new AuthResponse(token);
    }
}
