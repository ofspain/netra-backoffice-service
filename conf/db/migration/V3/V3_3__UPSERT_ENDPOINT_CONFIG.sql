CREATE OR REPLACE FUNCTION upsert_endpoint_config(
    -- Identifiers
    p_id BIGINT DEFAULT NULL,
    p_domain_owner_id BIGINT DEFAULT NULL,
    p_domain_owner_type VARCHAR(50) DEFAULT NULL,
    p_domain_owner_code VARCHAR(100) DEFAULT NULL,
    p_description TEXT DEFAULT NULL,

    -- Configs (JSONB)
    p_network_config JSONB DEFAULT NULL,
    p_security_config JSONB DEFAULT NULL,
    p_endpoints JSONB DEFAULT NULL,
    p_resilience_config JSONB DEFAULT NULL,
    p_metadata JSONB DEFAULT NULL
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
    metadata JSONB,
    operation_type VARCHAR(10)
) AS $$
DECLARE
v_op VARCHAR(10);
BEGIN
    ----------------------------------------------------------------------
    -- BASIC VALIDATION
    ----------------------------------------------------------------------
IF p_domain_owner_code IS NULL OR p_domain_owner_code = '' THEN
        RAISE EXCEPTION 'domain_owner_code required';
END IF;
IF p_domain_owner_id IS NULL OR p_domain_owner_type IS NULL THEN
        RAISE EXCEPTION 'domain_owner_id and domain_owner_type required';
END IF;

    -- Optional JSON validations
    IF p_network_config IS NOT NULL THEN
        IF NOT (p_network_config ? 'baseUrl') THEN
            RAISE EXCEPTION 'network_config must contain baseUrl';
END IF;
        IF (p_network_config->>'timeoutMillis')::INT NOT BETWEEN 100 AND 30000 THEN
            RAISE EXCEPTION 'network_config.timeoutMillis must be between 100 and 30000';
END IF;
END IF;

    IF p_resilience_config IS NOT NULL THEN
        IF NOT (p_resilience_config ? 'retry') THEN
            RAISE EXCEPTION 'resilience_config must contain retry config';
END IF;
END IF;

    ----------------------------------------------------------------------
    -- INSERT OR UPDATE
    ----------------------------------------------------------------------
    IF p_id IS NULL THEN
        INSERT INTO endpoint_configs (
            domain_owner_id, domain_owner_type, domain_owner_code, description,
            network_config, security_config, endpoints, resilience_config, metadata
        ) VALUES (
            p_domain_owner_id, p_domain_owner_type, p_domain_owner_code, p_description,
            COALESCE(p_network_config, '{}'::jsonb),
            COALESCE(p_security_config, '{}'::jsonb),
            COALESCE(p_endpoints, '{}'::jsonb),
            COALESCE(p_resilience_config, '{}'::jsonb),
            COALESCE(p_metadata, '{}'::jsonb)
        )
        RETURNING *, 'INSERT' INTO STRICT id, created_at, updated_at,
            domain_owner_id, domain_owner_type, domain_owner_code, description,
            network_config, security_config, endpoints, resilience_config, metadata, v_op;
ELSE
UPDATE endpoint_configs SET
    updated_at = NOW(),
    domain_owner_id = COALESCE(p_domain_owner_id, domain_owner_id),
    domain_owner_type = COALESCE(p_domain_owner_type, domain_owner_type),
    domain_owner_code = COALESCE(p_domain_owner_code, domain_owner_code),
    description = COALESCE(p_description, description),
    network_config = COALESCE(p_network_config, network_config),
    security_config = COALESCE(p_security_config, security_config),
    endpoints = COALESCE(p_endpoints, endpoints),
    resilience_config = COALESCE(p_resilience_config, resilience_config),
    metadata = COALESCE(p_metadata, metadata)
WHERE id = p_id
    RETURNING *, 'UPDATE' INTO STRICT id, created_at, updated_at,
    domain_owner_id, domain_owner_type, domain_owner_code, description,
    network_config, security_config, endpoints, resilience_config, metadata, v_op;
END IF;

    operation_type := v_op;
    RETURN NEXT;
END;
$$ LANGUAGE plpgsql;
