CREATE OR REPLACE FUNCTION find_endpoint_config_by_id(
    p_id BIGINT
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
    fallback_config JSONB
) AS $$
BEGIN
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
    ec.fallback_config
FROM endpoint_configs ec
WHERE ec.id = p_id;
END;
$$ LANGUAGE plpgsql;
