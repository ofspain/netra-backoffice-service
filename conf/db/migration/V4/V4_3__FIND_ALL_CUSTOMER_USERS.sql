CREATE OR REPLACE FUNCTION find_all_customer_users(
    p_limit INT DEFAULT 100,
    p_offset INT DEFAULT 0,
    p_sort_by VARCHAR(50) DEFAULT 'created_at',
    p_sort_dir VARCHAR(4) DEFAULT 'DESC'
)
RETURNS TABLE (
    id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    name VARCHAR(255),
    disabled BOOLEAN,
    user_phone VARCHAR(50),
    user_email VARCHAR(50),
    accounts JSONB,
    identity_uuid TEXT,
    total_count BIGINT
)
AS $$
DECLARE
v_total_count BIGINT;
    v_sort_column TEXT;
    v_sort_direction TEXT;
BEGIN
    ----------------------------------------------------------------------
    -- VALIDATE AND SANITIZE SORT PARAMETERS
    ----------------------------------------------------------------------
    v_sort_column := CASE
        WHEN p_sort_by IN ('id', 'created_at', 'updated_at', 'name', 'user_phone', 'user_email')
            THEN p_sort_by
        ELSE 'created_at'
END;

    v_sort_direction := CASE
        WHEN UPPER(p_sort_dir) IN ('ASC', 'DESC')
            THEN UPPER(p_sort_dir)
        ELSE 'DESC'
END;

    ----------------------------------------------------------------------
    -- GET TOTAL COUNT (BEFORE PAGINATION)
    ----------------------------------------------------------------------
SELECT COUNT(*) INTO v_total_count
FROM customer_users;

----------------------------------------------------------------------
-- RETURN PAGINATED RESULTS WITH TOTAL COUNT
----------------------------------------------------------------------
RETURN QUERY EXECUTE format(
        'SELECT
            cu.id,
            cu.created_at,
            cu.updated_at,
            cu.name,
            cu.disabled,
            cu.user_phone,
            cu.user_email,
            cu.accounts,
            cu.identity_uuid,
            %L::BIGINT AS total_count
         FROM customer_users cu
         ORDER BY %I %s
         LIMIT $1 OFFSET $2',
        v_total_count, v_sort_column, v_sort_direction
    )
    USING p_limit, p_offset;

END;
$$ LANGUAGE plpgsql;
