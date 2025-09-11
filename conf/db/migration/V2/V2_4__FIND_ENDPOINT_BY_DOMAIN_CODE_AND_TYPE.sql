CREATE OR REPLACE FUNCTION find_endpoint_configs_by_domain(
    p_domain_code VARCHAR(100) DEFAULT NULL,
    p_domain_type VARCHAR(50) DEFAULT NULL,
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
    -- Get total count for pagination
SELECT COUNT(*) INTO v_total_count
FROM endpoint_configs ec
WHERE (p_domain_code IS NULL OR ec.domain_code ILIKE '%' || p_domain_code || '%')
  AND (p_domain_type IS NULL OR ec.domain_type = p_domain_type);

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
ORDER BY ec.created_at DESC
    LIMIT p_limit
OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;