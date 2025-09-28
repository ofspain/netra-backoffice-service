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

    -- Endpoint Config fields (modernized)
    ec_id BIGINT,
    ec_created_at TIMESTAMP,
    ec_updated_at TIMESTAMP,
    ec_domain_owner_id BIGINT,
    ec_domain_owner_type VARCHAR(50),
    ec_domain_owner_code VARCHAR(100),
    ec_description TEXT,
    ec_network_config JSONB,
    ec_security_config JSONB,
    ec_endpoints JSONB,
    ec_resilience_config JSONB,
    ec_metadata JSONB
) AS $$
BEGIN
RETURN QUERY
SELECT
    -- Financial Institution
    fi.id,
    fi.created_at,
    fi.updated_at,
    fi.name,
    fi.code,
    fi.domain_code,
    fi.disabled,
    fi.logo_key,
    fi.endpoint_config,

    -- Endpoint Config (validated ownership)
    ec.id,
    ec.created_at,
    ec.updated_at,
    ec.domain_owner_id,
    ec.domain_owner_type,
    ec.domain_owner_code,
    ec.description,
    ec.network_config,
    ec.security_config,
    ec.endpoints,
    ec.resilience_config,
    ec.metadata
FROM financial_institutions fi
         LEFT JOIN endpoint_configs ec
                   ON fi.endpoint_config = ec.id
                       AND ec.domain_owner_type = 'FINANCIAL_INSTITUTION'
                       AND ec.domain_owner_id = fi.id
WHERE fi.id = p_id;

-- Raise exception if FI not found
IF NOT FOUND THEN
        RAISE EXCEPTION 'Financial institution with id % not found', p_id;
END IF;
END;
$$ LANGUAGE plpgsql;
