INSERT INTO users_auth (
    id,
    username,
    password_hash,
    role,
    enabled,
    created_at
)
VALUES (
    gen_random_uuid(),
    'admin',
    '$2a$10$FFV69XJK25DYTzm876NTNemRQMCTMoIVNdW2GwAsQDEK8ffBvJMKG',
    'ADMIN',
    true,
    now()
)
ON CONFLICT (username) DO NOTHING;