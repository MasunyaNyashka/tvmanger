CREATE TABLE admin_audit_log (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    admin_user_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    details VARCHAR(2000)
);

CREATE INDEX idx_auth_admin_audit_created_at ON admin_audit_log(created_at DESC);
CREATE INDEX idx_auth_admin_audit_admin_user_id ON admin_audit_log(admin_user_id);
CREATE INDEX idx_auth_admin_audit_entity ON admin_audit_log(entity_type, entity_id);
