CREATE OR REPLACE FUNCTION upsert_transaction_type(
    p_name VARCHAR(255),
    p_code VARCHAR(100),
    p_id BIGINT DEFAULT NULL,
    p_description TEXT DEFAULT NULL,
    p_disabled BOOLEAN DEFAULT FALSE,
    p_channels VARCHAR(50)[] DEFAULT NULL
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
v_id BIGINT;
    v_channel VARCHAR(50);
BEGIN
    -- Validate required fields
    IF p_name IS NULL OR p_code IS NULL THEN
        RAISE EXCEPTION 'Name and code are required fields';
END IF;

    -- Validate channel values if provided
    IF p_channels IS NOT NULL THEN
        FOREACH v_channel IN ARRAY p_channels LOOP
            IF v_channel NOT IN (
                'NIP', 'POS_SWITCH', 'USSD_GATEWAY',
                'WALLET_PROCESSOR', 'CARD_SCHEME', 'OFFLINE'
            ) THEN
                RAISE EXCEPTION 'Invalid channel value: %', v_channel;
END IF;
END LOOP;
END IF;

    -- Upsert the transaction type
    IF p_id IS NULL THEN
        -- Check for existing name or code
        PERFORM 1 FROM transaction_types
        WHERE name = p_name OR code = p_code
        LIMIT 1;

        IF FOUND THEN
            RAISE EXCEPTION 'Transaction type with this name or code already exists';
END IF;

INSERT INTO transaction_types (
    name, code, description, disabled, updated_at
) VALUES (
             p_name, p_code, p_description, p_disabled, NOW()
         )
    RETURNING id INTO v_id;
ELSE
        -- Check for name/code conflicts with other records
        PERFORM 1 FROM transaction_types
        WHERE (name = p_name OR code = p_code)
        AND id != p_id
        LIMIT 1;

        IF FOUND THEN
            RAISE EXCEPTION 'Another transaction type with this name or code already exists';
END IF;

UPDATE transaction_types
SET
    name = p_name,
    code = p_code,
    description = p_description,
    disabled = p_disabled,
    updated_at = NOW()
WHERE id = p_id
    RETURNING id INTO v_id;

IF NOT FOUND THEN
            RAISE EXCEPTION 'Transaction type not found with ID %', p_id;
END IF;

        -- Clear existing channels
DELETE FROM transaction_type_channels
WHERE transaction_type_id = v_id;
END IF;

    -- Insert new channels if provided
    IF p_channels IS NOT NULL THEN
        FOREACH v_channel IN ARRAY p_channels LOOP
            INSERT INTO transaction_type_channels (
                transaction_type_id, channel
            ) VALUES (
                v_id, v_channel
            )
            ON CONFLICT DO NOTHING;
END LOOP;
END IF;

RETURN v_id;
END;
$$;