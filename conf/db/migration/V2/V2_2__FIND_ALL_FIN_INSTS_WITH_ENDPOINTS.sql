CREATE OR REPLACE FUNCTION find_all_financial_institutions_with_endpoints(
    p_limit INT DEFAULT 100,
    p_offset INT DEFAULT 0,
    p_sort_by VARCHAR(50) DEFAULT 'fi.created_at',
    p_sort_dir VARCHAR(4) DEFAULT 'DESC'
) RETURNS TABLE (
    -- Financial Institution fields
    fi_id BIGINT,
    fi_created_at TIMESTAMP,
    fi_updated_at TIMESTAMP,
    fi_name VARCHAR(255),
    fi_code VARCHAR(100),
    fi_domain_code VARCHAR(50),
    fi_disabled BOOLEAN,
    fi_logo_key TEXT,
    fi_endpoint_config BIGINT,
    
    -- Endpoint Config fields
    ec_id BIGINT,
    ec_created_at TIMESTAMP,
    ec_updated_at TIMESTAMP,
    ec_domain_code VARCHAR(100),
    ec_domain_type VARCHAR(50),
    ec_description TEXT,
    ec_base_url TEXT,
    ec_timeout_millis INT,
    ec_use_proxy BOOLEAN,
    ec_proxy_config JSONB,
    ec_requires_auth BOOLEAN,
    ec_auth_type VARCHAR(50),
    ec_request_body_template TEXT,
    ec_unique_transaction JSONB,
    ec_multiple_transaction JSONB,
    ec_retry_config JSONB,
    ec_fallback_config JSONB,
    
    -- Pagination info
    total_count BIGINT
) AS $$
DECLARE
v_total_count BIGINT;
    v_sort_column TEXT;
    v_sort_direction TEXT;
BEGIN
    -- Validate and sanitize sort parameters
    v_sort_column := CASE 
        WHEN p_sort_by IN ('fi.id', 'fi.created_at', 'fi.updated_at', 'fi.name', 'fi.code', 'fi.domain_code',
                          'ec.domain_code', 'ec.created_at') 
        THEN p_sort_by 
        ELSE 'fi.created_at'
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
            fi.id AS fi_id,
            fi.created_at AS fi_created_at,
            fi.updated_at AS fi_updated_at,
            fi.name AS fi_name,
            fi.code AS fi_code,
            fi.domain_code AS fi_domain_code,
            fi.disabled AS fi_disabled,
            fi.logo_key AS fi_logo_key,
            fi.endpoint_config AS fi_endpoint_config,
            
            ec.id AS ec_id,
            ec.created_at AS ec_created_at,
            ec.updated_at AS ec_updated_at,
            ec.domain_code AS ec_domain_code,
            ec.domain_type AS ec_domain_type,
            ec.description AS ec_description,
            ec.base_url AS ec_base_url,
            ec.timeout_millis AS ec_timeout_millis,
            ec.use_proxy AS ec_use_proxy,
            ec.proxy_config AS ec_proxy_config,
            ec.requires_auth AS ec_requires_auth,
            ec.auth_type AS ec_auth_type,
            ec.request_body_template AS ec_request_body_template,
            ec.unique_transaction AS ec_unique_transaction,
            ec.multiple_transaction AS ec_multiple_transaction,
            ec.retry_config AS ec_retry_config,
            ec.fallback_config AS ec_fallback_config,
            
            %L::BIGINT AS total_count
         FROM financial_institutions fi
         LEFT JOIN endpoint_configs ec ON fi.endpoint_config = ec.id
         ORDER BY %s %s
         LIMIT $1 OFFSET $2',
        v_total_count, v_sort_column, v_sort_direction
    ) USING p_limit, p_offset;
END;
$$ LANGUAGE plpgsql;