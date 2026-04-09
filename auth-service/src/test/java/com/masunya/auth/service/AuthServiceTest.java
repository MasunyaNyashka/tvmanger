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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AdminAuditLogRepository adminAuditLogRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerShouldCreateClientUserAndReturnToken() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new-user");
        request.setPassword("secret123");

        when(userRepository.existsByUsername("new-user")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");
        when(jwtUtil.generateToken(any(UUID.class), any(String.class), any(Role.class))).thenReturn("jwt-token");

        String token = authService.register(request);

        ArgumentCaptor<AuthUser> userCaptor = ArgumentCaptor.forClass(AuthUser.class);
        verify(userRepository).save(userCaptor.capture());

        AuthUser savedUser = userCaptor.getValue();
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("new-user");
        assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(savedUser.getRole()).isEqualTo(Role.CLIENT);
        assertThat(savedUser.isEnabled()).isTrue();
        assertThat(token).isEqualTo("jwt-token");
    }

    @Test
    void registerShouldRejectDuplicateUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing-user");
        request.setPassword("secret123");

        when(userRepository.existsByUsername("existing-user")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getMessage()).isEqualTo("Username already exists");
                    assertThat(businessException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                });

        verify(userRepository, never()).save(any());
        verify(jwtUtil, never()).generateToken(any(UUID.class), any(String.class), any(Role.class));
    }

    @Test
    void loginShouldReturnTokenForClientWithoutAuditLog() {
        LoginRequest request = new LoginRequest();
        request.setUsername("client");
        request.setPassword("secret123");

        AuthUser user = new AuthUser();
        user.setId(UUID.randomUUID());
        user.setUsername("client");
        user.setPasswordHash("encoded-password");
        user.setRole(Role.CLIENT);

        when(userRepository.findByUsername("client")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken(user.getId(), "client", Role.CLIENT)).thenReturn("jwt-token");

        String token = authService.login(request);

        assertThat(token).isEqualTo("jwt-token");
        verify(adminAuditLogRepository, never()).save(any());
    }

    @Test
    void loginShouldWriteAuditLogForAdmin() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("secret123");

        AuthUser user = new AuthUser();
        user.setId(UUID.randomUUID());
        user.setUsername("admin");
        user.setPasswordHash("encoded-password");
        user.setRole(Role.ADMIN);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken(user.getId(), "admin", Role.ADMIN)).thenReturn("admin-token");

        String token = authService.login(request);

        ArgumentCaptor<AdminAuditLog> auditCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepository).save(auditCaptor.capture());

        AdminAuditLog auditLog = auditCaptor.getValue();
        assertThat(token).isEqualTo("admin-token");
        assertThat(auditLog.getAdminUserId()).isEqualTo(user.getId());
        assertThat(auditLog.getAction()).isEqualTo("ADMIN_LOGIN");
        assertThat(auditLog.getEntityType()).isEqualTo("AUTH");
        assertThat(auditLog.getEntityId()).isEqualTo(user.getId());
        assertThat(auditLog.getDetails()).isEqualTo("admin");
        assertThat(auditLog.getCreatedAt()).isNotNull();
        assertThat(auditLog.getId()).isNotNull();
    }

    @Test
    void loginShouldRejectUnknownUsername() {
        LoginRequest request = new LoginRequest();
        request.setUsername("missing-user");
        request.setPassword("secret123");

        when(userRepository.findByUsername("missing-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getMessage()).isEqualTo("Invalid credentials");
                    assertThat(businessException.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
                });

        verify(jwtUtil, never()).generateToken(any(UUID.class), any(String.class), any(Role.class));
    }

    @Test
    void loginShouldRejectWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setUsername("client");
        request.setPassword("wrong-password");

        AuthUser user = new AuthUser();
        user.setId(UUID.randomUUID());
        user.setUsername("client");
        user.setPasswordHash("encoded-password");
        user.setRole(Role.CLIENT);

        when(userRepository.findByUsername("client")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getMessage()).isEqualTo("Invalid credentials");
                    assertThat(businessException.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
                });

        verify(adminAuditLogRepository, never()).save(any());
        verify(jwtUtil, never()).generateToken(any(UUID.class), any(String.class), any(Role.class));
    }
}
