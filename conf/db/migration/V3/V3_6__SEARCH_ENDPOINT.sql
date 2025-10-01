CREATE OR REPLACE FUNCTION search_endpoint_configs(
    p_domain_owner_id BIGINT DEFAULT NULL,
    p_domain_owner_code VARCHAR(100) DEFAULT NULL,
    p_domain_owner_type VARCHAR(50) DEFAULT NULL,
    p_network_host_pattern VARCHAR(500) DEFAULT NULL, -- still pattern-based
    p_requires_auth BOOLEAN DEFAULT NULL,
    p_auth_type VARCHAR(50) DEFAULT NULL,
    p_use_proxy BOOLEAN DEFAULT NULL,
    p_has_endpoint BOOLEAN DEFAULT NULL,
    p_fallback_type VARCHAR(50) DEFAULT NULL,
    p_limit INT DEFAULT 100,
    p_offset INT DEFAULT 0,
    p_sort_by VARCHAR(50) DEFAULT 'ec.created_at',
    p_sort_dir VARCHAR(4) DEFAULT 'DESC'
) RETURNS TABLE (
    id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    domain_owner_id BIGINT,
    domain_code VARCHAR(100),
    domain_owner_type VARCHAR(50),
    description TEXT,
    network_config JSONB,
    security_config JSONB,
    endpoints JSONB,
    resilience_config JSONB,
    metadata JSONB,
    total_count BIGINT
) AS $$
DECLARE
v_total_count BIGINT;
    v_sort_column TEXT;
    v_sort_direction TEXT;
BEGIN
    -- Validate fallback_type: silently discard if invalid
    IF p_fallback_type IS NOT NULL
       AND p_fallback_type NOT IN ('STATIC_RESPONSE','REDIRECT_ENDPOINT','EXCEPTION') THEN
        p_fallback_type := NULL;
END IF;

    -- Validate and sanitize sort params
    v_sort_column := CASE
        WHEN p_sort_by IN ('ec.id','ec.created_at','ec.updated_at','ec.domain_code','ec.domain_owner_type')
        THEN p_sort_by
        ELSE 'ec.created_at'
END;

    v_sort_direction := CASE
        WHEN UPPER(p_sort_dir) IN ('ASC','DESC')
        THEN UPPER(p_sort_dir)
        ELSE 'DESC'
END;

    -- Count for pagination
SELECT COUNT(*) INTO v_total_count
FROM endpoint_configs ec
WHERE (p_domain_owner_id IS NULL OR ec.domain_owner_id = p_domain_owner_id)
  AND (p_domain_owner_code IS NULL OR ec.domain_owner_code = p_domain_owner_code)
  AND (p_domain_owner_type IS NULL OR ec.domain_owner_type = p_domain_owner_type)
  AND (p_network_host_pattern IS NULL OR ec.network_config->>'host' ILIKE '%' || p_network_host_pattern || '%')
  AND (p_requires_auth IS NULL OR (ec.security_config->>'requiresAuth')::BOOLEAN = p_requires_auth)
  AND (p_auth_type IS NULL OR ec.security_config->>'authType' = p_auth_type)
  AND (p_use_proxy IS NULL OR (ec.network_config->>'useProxy')::BOOLEAN = p_use_proxy)
  AND (p_has_endpoint IS NULL OR
       (p_has_endpoint AND jsonb_array_length(COALESCE(ec.endpoints, '[]'::jsonb)) > 0) OR
       (NOT p_has_endpoint AND (ec.endpoints IS NULL OR jsonb_array_length(ec.endpoints) = 0)))
  AND (p_fallback_type IS NULL OR ec.resilience_config->'fallback'->>'type' = p_fallback_type);

-- Return paginated + sorted rows
RETURN QUERY EXECUTE format(
        'SELECT
            ec.id,
            ec.created_at,
            ec.updated_at,
            ec.domain_owner_id,
            ec.domain_code,
            ec.domain_owner_type,
            ec.description,
            ec.network_config,
            ec.security_config,
            ec.endpoints,
            ec.resilience_config,
            ec.metadata,
            %L::BIGINT AS total_count
         FROM endpoint_configs ec
         WHERE ($1::BIGINT IS NULL OR ec.domain_owner_id = $1)
           AND ($2::VARCHAR IS NULL OR ec.domain_code = $2)
           AND ($3::VARCHAR IS NULL OR ec.domain_owner_type = $3)
           AND ($4::VARCHAR IS NULL OR ec.network_config->>''host'' ILIKE ''%'' || $4 || ''%'')
           AND ($5::BOOLEAN IS NULL OR (ec.security_config->>''requiresAuth'')::BOOLEAN = $5)
           AND ($6::VARCHAR IS NULL OR ec.security_config->>''authType'' = $6)
           AND ($7::BOOLEAN IS NULL OR (ec.network_config->>''useProxy'')::BOOLEAN = $7)
           AND ($8::BOOLEAN IS NULL OR
                ($8 AND jsonb_array_length(COALESCE(ec.endpoints, ''[]''::jsonb)) > 0) OR
                (NOT $8 AND (ec.endpoints IS NULL OR jsonb_array_length(ec.endpoints) = 0)))
           AND ($9::VARCHAR IS NULL OR ec.resilience_config->''fallback''->>''type'' = $9)
         ORDER BY %s %s
         LIMIT $10 OFFSET $11',
        v_total_count, v_sort_column, v_sort_direction
    )
    USING p_domain_owner_id, p_domain_owner_code, p_domain_owner_type,
          p_network_host_pattern, p_requires_auth, p_auth_type,
          p_use_proxy, p_has_endpoint, p_fallback_type,
          p_limit, p_offset;
END;
$$ LANGUAGE plpgsql;
