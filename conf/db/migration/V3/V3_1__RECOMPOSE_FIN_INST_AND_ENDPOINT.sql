DROP TABLE IF EXISTS financial_institutions CASCADE;
DROP TABLE IF EXISTS endpoint_configs CASCADE;

CREATE TABLE endpoint_configs (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    -- ownership identity
    domain_owner_id BIGINT NOT NULL,
    domain_owner_code VARCHAR(100) NOT NULL,
    domain_owner_type VARCHAR(50) NOT NULL,

    description TEXT,

    network_config JSONB NOT NULL,
    security_config JSONB NOT NULL,
    endpoints JSONB NOT NULL,
    resilience_config JSONB NOT NULL,
    metadata JSONB
);

CREATE INDEX idx_endpoint_config_domain_type ON endpoint_configs(domain_owner_type);
CREATE INDEX idx_endpoint_config_domain_id ON endpoint_configs(domain_owner_id);
CREATE INDEX idx_endpoint_config_network_gin ON endpoint_configs USING GIN (network_config);
CREATE INDEX idx_endpoint_config_security_gin ON endpoint_configs USING GIN (security_config);
CREATE INDEX idx_endpoint_config_endpoints_gin ON endpoint_configs USING GIN (endpoints);
CREATE INDEX idx_endpoint_config_resilience_gin ON endpoint_configs USING GIN (resilience_config);
CREATE INDEX idx_endpoint_config_metadata_gin ON endpoint_configs USING GIN (metadata);

CREATE TABLE financial_institutions (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(100) NOT NULL UNIQUE,
    domain_code VARCHAR(50) NOT NULL UNIQUE,
    disabled BOOLEAN NOT NULL DEFAULT FALSE,
    logo_key TEXT,
    endpoint_config BIGINT UNIQUE REFERENCES endpoint_configs(id) ON DELETE CASCADE
);

-- Indexes (note: unique constraints already add indexes for code & domain_code)
CREATE INDEX idx_fi_created_at ON financial_institutions(created_at);
CREATE INDEX idx_fi_name ON financial_institutions(name);
CREATE INDEX idx_fi_disabled ON financial_institutions(disabled);
