CREATE TABLE connection_orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    tariff_id UUID NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    address VARCHAR(300) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    admin_comment VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_connection_orders_user_id ON connection_orders(user_id);
CREATE INDEX idx_connection_orders_status ON connection_orders(status);
CREATE INDEX idx_connection_orders_created_at ON connection_orders(created_at);
