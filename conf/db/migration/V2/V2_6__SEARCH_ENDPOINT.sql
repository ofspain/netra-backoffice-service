CREATE OR REPLACE FUNCTION search_endpoint_configs(
    p_domain_code VARCHAR(100) DEFAULT NULL,
    p_domain_type VARCHAR(50) DEFAULT NULL,
    p_base_url_pattern VARCHAR(500) DEFAULT NULL,
    p_requires_auth BOOLEAN DEFAULT NULL,
    p_auth_type VARCHAR(50) DEFAULT NULL,
    p_use_proxy BOOLEAN DEFAULT NULL,
    p_has_unique_transaction BOOLEAN DEFAULT NULL,
    p_has_multiple_transaction BOOLEAN DEFAULT NULL,
    p_fallback_type VARCHAR(50) DEFAULT NULL,  -- NEW PARAM
    p_limit INT DEFAULT 100,
    p_offset INT DEFAULT 0
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
    total_count BIGINT
) AS $$
DECLARE
v_total_count BIGINT;
BEGIN
    -- Validate fallback_type: silently discard if invalid
    IF p_fallback_type IS NOT NULL
       AND p_fallback_type NOT IN ('STATIC_RESPONSE','REDIRECT_ENDPOINT','EXCEPTION') THEN
        p_fallback_type := NULL;
END IF;

    -- Get total count for pagination
SELECT COUNT(*) INTO v_total_count
FROM endpoint_configs ec
WHERE (p_domain_code IS NULL OR ec.domain_code ILIKE '%' || p_domain_code || '%')
  AND (p_domain_type IS NULL OR ec.domain_type = p_domain_type)
  AND (p_base_url_pattern IS NULL OR ec.base_url ILIKE '%' || p_base_url_pattern || '%')
  AND (p_requires_auth IS NULL OR ec.requires_auth = p_requires_auth)
  AND (p_auth_type IS NULL OR ec.auth_type = p_auth_type)
  AND (p_use_proxy IS NULL OR ec.use_proxy = p_use_proxy)
  AND (p_has_unique_transaction IS NULL OR
       (p_has_unique_transaction AND ec.unique_transaction IS NOT NULL) OR
       (NOT p_has_unique_transaction AND ec.unique_transaction IS NULL))
  AND (p_has_multiple_transaction IS NULL OR
       (p_has_multiple_transaction AND ec.multiple_transaction IS NOT NULL) OR
       (NOT p_has_multiple_transaction AND ec.multiple_transaction IS NULL))
  AND (p_fallback_type IS NULL OR ec.fallback_config->>'type' = p_fallback_type);

-- Return paginated results with total count
RETURN QUERY
SELECT
    ec.id,
    ec.created_at,
    ec.updated_at,
    ec.domain_code,
    ec.domain_type,
    ec.description,
    ec.base_url,
    ec.timeout_millis,
    ec.use_proxy,
    ec.proxy_config,
    ec.requires_auth,
    ec.auth_type,
    ec.request_body_template,
    ec.unique_transaction,
    ec.multiple_transaction,
    ec.retry_config,
    ec.fallback_config,
    v_total_count
FROM endpoint_configs ec
WHERE (p_domain_code IS NULL OR ec.domain_code ILIKE '%' || p_domain_code || '%')
  AND (p_domain_type IS NULL OR ec.domain_type = p_domain_type)
  AND (p_base_url_pattern IS NULL OR ec.base_url ILIKE '%' || p_base_url_pattern || '%')
  AND (p_requires_auth IS NULL OR ec.requires_auth = p_requires_auth)
  AND (p_auth_type IS NULL OR ec.auth_type = p_auth_type)
  AND (p_use_proxy IS NULL OR ec.use_proxy = p_use_proxy)
  AND (p_has_unique_transaction IS NULL OR
       (p_has_unique_transaction AND ec.unique_transaction IS NOT NULL) OR
       (NOT p_has_unique_transaction AND ec.unique_transaction IS NULL))
  AND (p_has_multiple_transaction IS NULL OR
       (p_has_multiple_transaction AND ec.multiple_transaction IS NOT NULL) OR
       (NOT p_has_multiple_transaction AND ec.multiple_transaction IS NULL))
  AND (p_fallback_type IS NULL OR ec.fallback_config->>'type' = p_fallback_type)
ORDER BY ec.created_at DESC
    LIMIT p_limit
OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;
