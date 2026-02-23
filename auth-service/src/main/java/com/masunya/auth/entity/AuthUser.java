package com.masunya.auth.entity;

import com.masunya.common.enumerate.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name = "users_auth")
@Getter
@Setter
@NoArgsConstructor
public class AuthUser {
    @Id
    @Column(nullable = false)
    private UUID id;
    @Column(nullable = false, unique = true, length = 100)
    private String username;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
