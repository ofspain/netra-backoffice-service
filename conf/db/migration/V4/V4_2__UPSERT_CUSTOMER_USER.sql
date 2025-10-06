CREATE OR REPLACE FUNCTION upsert_customer_user(
    -- Identifiers
    p_id BIGINT DEFAULT NULL,

    -- Core fields
    p_name VARCHAR(255) DEFAULT NULL,
    p_disabled BOOLEAN DEFAULT FALSE,

    -- Contact details
    p_user_phone VARCHAR(50) DEFAULT NULL,
    p_user_email VARCHAR(255) DEFAULT NULL,

    -- Accounts JSON (must not be empty)
    p_accounts JSONB DEFAULT '[]',

    -- Auth link
    p_identity_uuid TEXT DEFAULT NULL
) RETURNS TABLE (
    id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    name VARCHAR(255),
    disabled BOOLEAN,
    user_phone VARCHAR(50),
    user_email VARCHAR(255),
    accounts JSONB,
    identity_uuid TEXT,
    operation_type VARCHAR(10)
) AS $$
#variable_conflict use_column
DECLARE
v_existing customer_users;
v_result customer_users;
v_operation_type VARCHAR(10);
BEGIN
    ----------------------------------------------------------------------
    -- VALIDATION
    ----------------------------------------------------------------------
IF p_name IS NULL OR trim(p_name) = '' THEN
  RAISE EXCEPTION 'Name is required';
END IF;

IF p_user_phone IS NULL OR trim(p_user_phone) = '' THEN
   RAISE EXCEPTION 'User phone is required';
END IF;

IF p_identity_uuid IS NULL OR trim(p_identity_uuid) = '' THEN
   RAISE EXCEPTION 'Identity UUID is required';
END IF;

IF p_accounts IS NULL OR jsonb_array_length(p_accounts) = 0 THEN
    RAISE EXCEPTION 'Accounts JSON must not be null or empty';
END IF;

    ----------------------------------------------------------------------
    -- INSERT OR UPDATE BRANCH
    ----------------------------------------------------------------------
IF p_id IS NULL THEN
------------------------------------------------------------------
-- INSERT BRANCH
------------------------------------------------------------------
-- Uniqueness checks
    IF EXISTS (SELECT 1 FROM customer_users WHERE user_phone = p_user_phone) THEN
        RAISE EXCEPTION 'User with phone % already exists', p_user_phone;
    END IF;

    IF EXISTS (SELECT 1 FROM customer_users WHERE identity_uuid = p_identity_uuid) THEN
        RAISE EXCEPTION 'User with identity_uuid % already exists', p_identity_uuid;
    END IF;

    INSERT INTO customer_users (name, disabled, user_phone, user_email, accounts, identity_uuid)
        VALUES (p_name, p_disabled, p_user_phone, p_user_email, p_accounts, p_identity_uuid)
    RETURNING * INTO v_result;

    v_operation_type := 'INSERT';
ELSE
------------------------------------------------------------------
-- UPDATE BRANCH
------------------------------------------------------------------
    SELECT * INTO v_existing FROM customer_users WHERE id = p_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'CustomerUser with id % not found', p_id;
    END IF;

-- Unique checks if changing phone or identity_uuid
    IF p_user_phone IS DISTINCT FROM v_existing.user_phone THEN
        IF EXISTS (SELECT 1 FROM customer_users WHERE user_phone = p_user_phone AND id <> p_id) THEN
            RAISE EXCEPTION 'User with phone % already exists', p_user_phone;
        END IF;
    END IF;

    IF p_identity_uuid IS DISTINCT FROM v_existing.identity_uuid THEN
        IF EXISTS (SELECT 1 FROM customer_users WHERE identity_uuid = p_identity_uuid AND id <> p_id) THEN
            RAISE EXCEPTION 'User with identity_uuid % already exists', p_identity_uuid;
        END IF;
    END IF;

    UPDATE customer_users
        SET
            updated_at = NOW(),
            name = COALESCE(p_name, name),
            disabled = COALESCE(p_disabled, disabled),
            user_phone = COALESCE(p_user_phone, user_phone),
            user_email = COALESCE(p_user_email, user_email),
            accounts = COALESCE(p_accounts, accounts),
            identity_uuid = COALESCE(p_identity_uuid, identity_uuid)
    WHERE id = p_id
    RETURNING * INTO v_result;

    v_operation_type := 'UPDATE';
END IF;

    ----------------------------------------------------------------------
    -- RETURN RESULT
    ----------------------------------------------------------------------
RETURN QUERY
SELECT
    v_result.id,
    v_result.created_at,
    v_result.updated_at,
    v_result.name,
    v_result.disabled,
    v_result.user_phone,
    v_result.user_email,
    v_result.accounts,
    v_result.identity_uuid,
    v_operation_type::VARCHAR(10);

EXCEPTION
    WHEN unique_violation THEN
        IF SQLERRM LIKE '%user_phone%' THEN
            RAISE EXCEPTION 'Duplicate phone: %', p_user_phone;
        ELSIF SQLERRM LIKE '%identity_uuid%' THEN
            RAISE EXCEPTION 'Duplicate identity_uuid: %', p_identity_uuid;
        ELSE
            RAISE;
        END IF;
    WHEN OTHERS THEN
        RAISE;
END;
$$ LANGUAGE plpgsql;
