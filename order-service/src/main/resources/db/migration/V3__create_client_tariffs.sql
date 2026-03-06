CREATE TABLE client_tariffs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    tariff_id UUID NOT NULL,
    custom_price NUMERIC(12, 2),
    custom_conditions VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_client_tariffs_user_id ON client_tariffs(user_id);
CREATE INDEX idx_client_tariffs_tariff_id ON client_tariffs(tariff_id);
CREATE INDEX idx_client_tariffs_created_at ON client_tariffs(created_at);
