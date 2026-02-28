CREATE TABLE service_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL,
    tariff_id UUID,
    address VARCHAR(300) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    details VARCHAR(1000),
    status VARCHAR(30) NOT NULL,
    admin_comment VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_service_requests_user_id ON service_requests(user_id);
CREATE INDEX idx_service_requests_status ON service_requests(status);
CREATE INDEX idx_service_requests_type ON service_requests(type);
CREATE INDEX idx_service_requests_created_at ON service_requests(created_at);
