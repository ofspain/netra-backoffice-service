CREATE OR REPLACE FUNCTION upsert_financial_institution(
    -- Identifiers
    p_id BIGINT DEFAULT NULL,
    p_name VARCHAR(255) DEFAULT NULL,
    p_code VARCHAR(100) DEFAULT NULL,
    p_domain_code VARCHAR(50),

    -- Status & Configuration
    p_disabled BOOLEAN DEFAULT FALSE,
    p_logo_key TEXT DEFAULT NULL,
    p_endpoint_config BIGINT DEFAULT NULL,

    -- Validation options
    p_validate_uniqueness BOOLEAN DEFAULT TRUE,
    p_validate_endpoint_existence BOOLEAN DEFAULT TRUE
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
    operation_type VARCHAR(10)
) AS $$
#variable_conflict use_column
DECLARE
v_current_record financial_institutions;
    v_result_record financial_institutions;
    v_operation_type VARCHAR(10);
    v_code_exists BOOLEAN;
    v_domain_code_exists BOOLEAN;
    v_endpoint_exists BOOLEAN;
BEGIN
    ----------------------------------------------------------------------
    -- BASIC VALIDATION
    ----------------------------------------------------------------------
    IF p_name IS NULL OR p_name = '' THEN
        RAISE EXCEPTION 'name required';
END IF;

    IF p_code IS NULL OR p_code = '' THEN
        RAISE EXCEPTION 'code required';
END IF;

    IF p_domain_code IS NULL OR p_domain_code = '' THEN
        RAISE EXCEPTION 'domain_code required';
END IF;

    -- Validate endpoint_config existence if provided and validation is enabled
    IF p_endpoint_config IS NOT NULL AND p_validate_endpoint_existence THEN
        SELECT EXISTS(SELECT 1 FROM endpoint_configs WHERE id = p_endpoint_config) INTO v_endpoint_exists;

    IF NOT v_endpoint_exists THEN
            RAISE EXCEPTION 'endpoint_config with id % does not exist', p_endpoint_config;
    END IF;
END IF;

    ----------------------------------------------------------------------
    -- UNIQUENESS VALIDATION
    ----------------------------------------------------------------------
IF p_validate_uniqueness THEN
   IF p_id IS NULL THEN
            -- INSERT operation - check for existing code and domain_code
       SELECT EXISTS(SELECT 1 FROM financial_institutions WHERE code = p_code) INTO v_code_exists;

   IF v_code_exists THEN
      RAISE EXCEPTION 'code % already exists', p_code;
     END IF;

   SELECT EXISTS(SELECT 1 FROM financial_institutions WHERE domain_code = p_domain_code) INTO v_domain_code_exists;

     IF v_domain_code_exists THEN
           RAISE EXCEPTION 'domain_code % already exists', p_domain_code;
     END IF;
ELSE
            -- UPDATE operation - check for conflicts with other records
    SELECT * INTO v_current_record FROM financial_institutions WHERE id = p_id;

IF NOT FOUND THEN
    RAISE EXCEPTION 'FinancialInstitution with id % not found', p_id;
END IF;

            -- Check if code is being changed and validate uniqueness
IF p_code IS DISTINCT FROM v_current_record.code THEN
   SELECT EXISTS(SELECT 1 FROM financial_institutions WHERE code = p_code AND id != p_id) INTO v_code_exists;

IF v_code_exists THEN
   RAISE EXCEPTION 'code % already exists', p_code;
END IF;
END IF;

-- Check if domain_code is being changed and validate uniqueness
IF p_domain_code IS DISTINCT FROM v_current_record.domain_code THEN
   SELECT EXISTS(SELECT 1 FROM financial_institutions WHERE domain_code = p_domain_code AND id != p_id) INTO v_domain_code_exists;

IF v_domain_code_exists THEN
   RAISE EXCEPTION 'domain_code % already exists', p_domain_code;
END IF;
END IF;
END IF;
END IF;

    ----------------------------------------------------------------------
    -- INSERT OR UPDATE
    ----------------------------------------------------------------------
IF p_id IS NULL THEN
   -- INSERT operation
        INSERT INTO financial_institutions (
            name, code, domain_code, disabled, logo_key, endpoint_config
        ) VALUES (
            p_name, p_code, p_domain_code, p_disabled, p_logo_key, p_endpoint_config
        )
        RETURNING *, 'INSERT' INTO v_result_record, v_operation_type;
ELSE
        -- UPDATE operation
UPDATE financial_institutions SET
      updated_at = NOW(),
      name = COALESCE(p_name, name),
      code = COALESCE(p_code, code),
      domain_code = COALESCE(p_domain_code, domain_code),
      disabled = COALESCE(p_disabled, disabled),
      logo_key = COALESCE(p_logo_key, logo_key),
      endpoint_config = COALESCE(p_endpoint_config, endpoint_config)
WHERE id = p_id
    RETURNING *, 'UPDATE' INTO v_result_record, v_operation_type;
END IF;

    ----------------------------------------------------------------------
    -- RETURN RESULTS
    ----------------------------------------------------------------------
RETURN QUERY
SELECT
    v_result_record.id,
    v_result_record.created_at,
    v_result_record.updated_at,
    v_result_record.name,
    v_result_record.code,
    v_result_record.domain_code,
    v_result_record.disabled,
    v_result_record.logo_key,
    v_result_record.endpoint_config,
    v_operation_type::VARCHAR(10);

EXCEPTION
    WHEN unique_violation THEN
        -- Handle specific unique constraint violations
        IF SQLERRM LIKE '%financial_institutions_code_key%' THEN
            RAISE EXCEPTION 'Duplicate code: %', p_code;
        ELSIF SQLERRM LIKE '%financial_institutions_domain_code_key%' THEN
            RAISE EXCEPTION 'Duplicate domain_code: %', p_domain_code;
ELSE
            RAISE;
END IF;
WHEN foreign_key_violation THEN
        IF SQLERRM LIKE '%endpoint_config%' THEN
            RAISE EXCEPTION 'endpoint_config with id % does not exist', p_endpoint_config;
ELSE
            RAISE;
END IF;
WHEN OTHERS THEN
        RAISE;
END;
$$ LANGUAGE plpgsql;