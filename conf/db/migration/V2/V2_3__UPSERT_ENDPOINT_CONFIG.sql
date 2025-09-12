CREATE OR REPLACE FUNCTION upsert_endpoint_config(
    -- Identifiers
    p_id BIGINT DEFAULT NULL,
    p_domain_code VARCHAR(100) DEFAULT NULL,
    p_domain_type VARCHAR(50) DEFAULT NULL,
    p_description TEXT DEFAULT NULL,
    p_base_url TEXT DEFAULT NULL,
    p_timeout_millis INT DEFAULT 5000,
    p_use_proxy BOOLEAN DEFAULT FALSE,

    -- Configs
    p_proxy_config JSONB DEFAULT NULL,
    p_requires_auth BOOLEAN DEFAULT FALSE,
    p_auth_type VARCHAR(50) DEFAULT 'NONE',
    p_request_body_template TEXT DEFAULT NULL,
    p_unique_transaction JSONB DEFAULT NULL,
    p_multiple_transaction JSONB DEFAULT NULL,
    p_retry_config JSONB DEFAULT NULL,
    p_fallback_config JSONB DEFAULT NULL
) RETURNS TABLE (
    id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    domain_code VARCHAR(100),
    domain_type VARCHAR(50),
    description TEXT,
    base_url TEXT,
    timeout_millis INT,
    use_proxy BOOLEAN,
    proxy_config JSONB,
    requires_auth BOOLEAN,
    auth_type VARCHAR(50),
    request_body_template TEXT,
    unique_transaction JSONB,
    multiple_transaction JSONB,
    retry_config JSONB,
    fallback_config JSONB,
    operation_type VARCHAR(10)
) AS $$
DECLARE
    v_op VARCHAR(10);
    detail_json JSONB;
    field_name TEXT;
BEGIN
    ----------------------------------------------------------------------
    -- BASIC VALIDATION
    ----------------------------------------------------------------------
    IF p_domain_code IS NULL OR p_domain_code = '' THEN
        RAISE EXCEPTION 'domain_code required';
    END IF;
    IF p_domain_type IS NULL OR p_domain_type = '' THEN
        RAISE EXCEPTION 'domain_type required';
    END IF;
    IF p_base_url IS NULL OR p_base_url = '' THEN
        RAISE EXCEPTION 'base_url required';
    END IF;
    IF p_timeout_millis NOT BETWEEN 100 AND 30000 THEN
        RAISE EXCEPTION 'timeout_millis must be between 100 and 30000';
    END IF;
    IF p_requires_auth AND p_auth_type = 'NONE' THEN
        RAISE EXCEPTION 'auth_type cannot be NONE when requires_auth = true';
    END IF;

    ----------------------------------------------------------------------
    -- INLINE JSON VALIDATION
    ----------------------------------------------------------------------

    -- proxy_config
    IF p_proxy_config IS NOT NULL THEN
        IF NOT (p_proxy_config ? 'host' AND p_proxy_config ? 'port') THEN
            RAISE EXCEPTION 'proxy_config must contain host and port';
        END IF;
        IF (p_proxy_config->>'port')::INT NOT BETWEEN 1 AND 65535 THEN
            RAISE EXCEPTION 'proxy_config.port must be 1–65535';
        END IF;
    END IF;

    -- retry_config
    IF p_retry_config IS NOT NULL THEN
        IF NOT (p_retry_config ? 'maxAttempts') THEN
            RAISE EXCEPTION 'retry_config must contain maxAttempts';
        END IF;
        IF (p_retry_config->>'maxAttempts')::INT NOT BETWEEN 1 AND 10 THEN
            RAISE EXCEPTION 'retry_config.maxAttempts must be 1–10';
        END IF;
    END IF;

    -- fallback_config
    IF p_fallback_config IS NOT NULL THEN
        IF NOT (p_fallback_config ? 'type') THEN
            RAISE EXCEPTION 'fallback_config must contain type';
        END IF;
        IF (p_fallback_config->>'type') NOT IN ('STATIC_RESPONSE','REDIRECT_ENDPOINT','EXCEPTION') THEN
            RAISE EXCEPTION 'fallback_config.type invalid';
        END IF;
    END IF;

    -- endpoint details (unique_transaction & multiple_transaction)
    -- Ensure at least one endpoint detail is present
    IF p_unique_transaction IS NULL AND p_multiple_transaction IS NULL THEN
        RAISE EXCEPTION 'At least one of unique_transaction or multiple_transaction must be provided';
    END IF;
    FOR detail_json, field_name IN
        SELECT p_unique_transaction, 'unique_transaction'
        UNION ALL
        SELECT p_multiple_transaction, 'multiple_transaction'
        LOOP
            IF detail_json IS NOT NULL THEN
                IF NOT (detail_json ? 'url') THEN
                    RAISE EXCEPTION '% must contain url', field_name;
                END IF;
                IF detail_json ? 'method' AND (detail_json->>'method') NOT IN ('GET','POST') THEN
                    RAISE EXCEPTION '% method must be GET or POST', field_name;
                END IF;
                IF detail_json ? 'headers' AND jsonb_typeof(detail_json->'headers') = 'array'
                    THEN
                        FOR i IN 0..jsonb_array_length(detail_json->'headers')-1 LOOP
                            IF NOT (detail_json->'headers'->i ? 'name') THEN
                                RAISE EXCEPTION '% header[%] missing name', field_name, i;
                            END IF;
                        END LOOP;
                END IF;
            END IF;
    END LOOP;

    ----------------------------------------------------------------------
    -- INSERT OR UPDATE
    ----------------------------------------------------------------------
    IF p_id IS NULL THEN
        INSERT INTO endpoint_configs (
            domain_code, domain_type, description, base_url, timeout_millis,
            use_proxy, proxy_config, requires_auth, auth_type, request_body_template,
            unique_transaction, multiple_transaction, retry_config, fallback_config
        ) VALUES (
            p_domain_code, p_domain_type, p_description, p_base_url, p_timeout_millis,
            p_use_proxy, p_proxy_config, p_requires_auth, p_auth_type, p_request_body_template,
            p_unique_transaction, p_multiple_transaction, p_retry_config, p_fallback_config
        )
        RETURNING *, 'INSERT' INTO STRICT id, created_at, updated_at, domain_code,
            domain_type, description, base_url, timeout_millis, use_proxy, proxy_config,
            requires_auth, auth_type, request_body_template, unique_transaction,
            multiple_transaction, retry_config, fallback_config, v_op;
    ELSE
        UPDATE endpoint_configs SET
             updated_at = NOW(),
             domain_code = COALESCE(p_domain_code, domain_code),
             domain_type = COALESCE(p_domain_type, domain_type),
             description = COALESCE(p_description, description),
             base_url = COALESCE(p_base_url, base_url),
             timeout_millis = COALESCE(p_timeout_millis, timeout_millis),
             use_proxy = COALESCE(p_use_proxy, use_proxy),
             proxy_config = COALESCE(p_proxy_config, proxy_config),
             requires_auth = COALESCE(p_requires_auth, requires_auth),
             auth_type = COALESCE(p_auth_type, auth_type),
             request_body_template = COALESCE(p_request_body_template, request_body_template),
             unique_transaction = COALESCE(p_unique_transaction, unique_transaction),
             multiple_transaction = COALESCE(p_multiple_transaction, multiple_transaction),
             retry_config = COALESCE(p_retry_config, retry_config),
             fallback_config = COALESCE(p_fallback_config, fallback_config)
        WHERE id = p_id
        RETURNING *, 'UPDATE' INTO STRICT id, created_at, updated_at, domain_code,
            domain_type, description, base_url, timeout_millis, use_proxy, proxy_config,
            requires_auth, auth_type, request_body_template, unique_transaction,
            multiple_transaction, retry_config, fallback_config, v_op;
    END IF;
    operation_type := v_op;
    RETURN NEXT;
END;
$$ LANGUAGE plpgsql;
