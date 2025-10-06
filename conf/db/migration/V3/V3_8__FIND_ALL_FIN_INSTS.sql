CREATE OR REPLACE FUNCTION retrieve_all_financial_institutions()
RETURNS TABLE (
    id BIGINT,
    name VARCHAR(255),
    code VARCHAR(100),
    domain_code VARCHAR(50),
    disabled BOOLEAN,
    logo_key TEXT,
    endpoint_config BIGINT
) AS $$
BEGIN
RETURN QUERY
SELECT
    fi.id,
    fi.name,
    fi.code,
    fi.domain_code,
    fi.disabled,
    fi.logo_key,
    fi.endpoint_config
FROM financial_institutions fi
where fi.disabled = false
ORDER BY fi.name ASC;
END;
$$ LANGUAGE plpgsql;
