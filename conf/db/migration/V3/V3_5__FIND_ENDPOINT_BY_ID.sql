CREATE OR REPLACE FUNCTION find_endpoint_config_by_id(
    p_id BIGINT
) RETURNS TABLE (
    id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    domain_owner_id BIGINT,
    domain_owner_type VARCHAR(50),
    domain_owner_code VARCHAR(100),
    description TEXT,
    network_config JSONB,
    security_config JSONB,
    endpoints JSONB,
    resilience_config JSONB,
    metadata JSONB
) AS $$
BEGIN
RETURN QUERY
SELECT
    ec.id,
    ec.created_at,
    ec.updated_at,
    ec.domain_owner_id,
    ec.domain_owner_type,
    ec.domain_owner_code,
    ec.description,
    ec.network_config,
    ec.security_config,
    ec.endpoints,
    ec.resilience_config,
    ec.metadata
FROM endpoint_configs ec
WHERE ec.id = p_id;
END;
$$ LANGUAGE plpgsql;
