CREATE TABLE tariffs (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    price NUMERIC(12, 2) NOT NULL,
    connection_type VARCHAR(30) NOT NULL,
    description VARCHAR(1000),
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tariff_channels (
    tariff_id UUID NOT NULL REFERENCES tariffs(id) ON DELETE CASCADE,
    channel_name VARCHAR(100) NOT NULL
);

CREATE INDEX idx_tariffs_name ON tariffs(name);
CREATE INDEX idx_tariffs_archived ON tariffs(archived);
