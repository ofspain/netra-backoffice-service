CREATE OR REPLACE FUNCTION get_financial_institution_with_endpoint(p_id BIGINT)
RETURNS TABLE (
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
    ec_fallback_config JSONB
) AS $$
BEGIN
RETURN QUERY
SELECT
    -- Financial Institution columns
    fi.id,
    fi.created_at,
    fi.updated_at,
    fi.name,
    fi.code,
    fi.domain_code,
    fi.disabled,
    fi.logo_key,
    fi.endpoint_config,

    -- Endpoint Config columns
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

FROM financial_institutions fi
         LEFT JOIN endpoint_configs ec ON fi.endpoint_config = ec.id
WHERE fi.id = p_id;

-- Raise exception if no financial institution found
IF NOT FOUND THEN
        RAISE EXCEPTION 'Financial institution with id % not found', p_id;
END IF;
END;
$$ LANGUAGE plpgsql;