package com.masunya.auth.service;

import com.masunya.auth.audit.AdminAuditLog;
import com.masunya.auth.audit.AdminAuditLogRepository;
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

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AdminAuditLogRepository adminAuditLogRepository;

    public String register(RegisterRequest request) {
        // Не допускаем регистрацию с уже занятым логином.
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already exists");
        }
        // Создаем клиента по умолчанию и сохраняем в БД.
        AuthUser user = new AuthUser();
        user.setId(UUID.randomUUID());
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CLIENT);
        user.setEnabled(true);
        userRepository.save(user);
        // Сразу возвращаем JWT, чтобы пользователь был авторизован после регистрации.
        return jwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );
    }

    public String login(LoginRequest request) {
        // Находим пользователя и отдаем единый ответ при невалидных credentials.
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
        if (user.getRole() == Role.ADMIN) {
            // Логируем входы админов в отдельный аудит.
            saveAdminAudit(user.getId(), "ADMIN_LOGIN", "AUTH", user.getId(), user.getUsername());
        }
        // Генерируем новый JWT на успешный вход.
        return jwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );
    }

    private void saveAdminAudit(UUID adminUserId, String action, String entityType, UUID entityId, String details) {
        AdminAuditLog log = new AdminAuditLog();
        log.setId(UUID.randomUUID());
        log.setCreatedAt(Instant.now());
        log.setAdminUserId(adminUserId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        adminAuditLogRepository.save(log);
    }
}
