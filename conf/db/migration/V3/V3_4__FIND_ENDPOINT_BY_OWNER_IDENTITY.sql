-- 🔥 Drop old function first
DROP FUNCTION IF EXISTS find_endpoint_configs_by_domain(
    VARCHAR, VARCHAR, INT, INT
    );

-- ✅ Recreate with new schema and domain_owner_id
CREATE OR REPLACE FUNCTION find_endpoint_configs_by_domain(
    p_domain_owner_id BIGINT DEFAULT NULL,
    p_domain_owner_code VARCHAR(100) DEFAULT NULL,
    p_domain_owner_type VARCHAR(50) DEFAULT NULL,
    p_limit INT DEFAULT 100,
    p_offset INT DEFAULT 0
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
    total_count BIGINT
) AS $$
DECLARE
v_total_count BIGINT;
BEGIN
    -- Count total for pagination
SELECT COUNT(*) INTO v_total_count
FROM endpoint_configs ec
WHERE (p_domain_owner_id IS NULL OR ec.domain_owner_id = p_domain_owner_id)
  AND (p_domain_owner_code IS NULL OR ec.domain_owner_code ILIKE '%' || p_domain_owner_code || '%')
  AND (p_domain_owner_type IS NULL OR ec.domain_owner_type = p_domain_owner_type);

-- Return filtered + paginated rows with total count
RETURN QUERY
SELECT
    ec.id,
    ec.created_at,
    ec.updated_at,
    ec.domain_owner_id,
    ec.domain_owner_code,
    ec.domain_owner_type,
    ec.description,
    ec.network_config,
    ec.security_config,
    ec.endpoints,
    ec.resilience_config,
    ec.metadata,
    v_total_count
FROM endpoint_configs ec
WHERE (p_domain_owner_id IS NULL OR ec.domain_owner_id = p_domain_owner_id)
  AND (p_domain_owner_code IS NULL OR ec.domain_owner_code ILIKE '%' || p_domain_owner_code || '%')
  AND (p_domain_owner_type IS NULL OR ec.domain_owner_type = p_domain_owner_type)
ORDER BY ec.created_at DESC
    LIMIT p_limit
OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;
