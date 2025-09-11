CREATE OR REPLACE FUNCTION find_all_financial_institutions(
    p_limit INT DEFAULT 100,
    p_offset INT DEFAULT 0,
    p_sort_by VARCHAR(50) DEFAULT 'created_at',
    p_sort_dir VARCHAR(4) DEFAULT 'DESC'
) RETURNS TABLE (
    id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    name VARCHAR(255),
    code VARCHAR(100),
    domain_code VARCHAR(50),
    disabled BOOLEAN,
    logo_key TEXT,
    endpoint_config BIGINT,
    total_count BIGINT
) AS $$
DECLARE
v_total_count BIGINT;
    v_sort_column TEXT;
    v_sort_direction TEXT;
BEGIN
    -- Validate and sanitize sort parameters
    v_sort_column := CASE
        WHEN p_sort_by IN ('id', 'created_at', 'updated_at', 'name', 'code', 'domain_code')
        THEN p_sort_by
        ELSE 'created_at'
END;

    v_sort_direction := CASE
        WHEN UPPER(p_sort_dir) IN ('ASC', 'DESC')
        THEN UPPER(p_sort_dir)
        ELSE 'DESC'
END;

    -- Get total count
SELECT COUNT(*) INTO v_total_count FROM financial_institutions;

-- Return paginated results with total count
RETURN QUERY EXECUTE format(
        'SELECT
            fi.id,
            fi.created_at,
            fi.updated_at,
            fi.name,
            fi.code,
            fi.domain_code,
            fi.disabled,
            fi.logo_key,
            fi.endpoint_config,
            %L::BIGINT AS total_count
         FROM financial_institutions fi
         ORDER BY %I %s
         LIMIT $1 OFFSET $2',
        v_total_count, v_sort_column, v_sort_direction
    ) USING p_limit, p_offset;
END;
$$ LANGUAGE plpgsql;