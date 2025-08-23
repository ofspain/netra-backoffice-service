CREATE OR REPLACE FUNCTION find_financial_institution(
    search_key TEXT,
    search_value TEXT
)
RETURNS SETOF financial_institutions AS $$
DECLARE
sql TEXT;
BEGIN
    -- Only allow certain columns
    IF search_key NOT IN ('id', 'domain_code', 'code') THEN
        RAISE EXCEPTION 'Invalid search key: %', search_key;
END IF;

    -- Build dynamic SQL with proper casting
IF search_key = 'id' THEN
        -- id is BIGINT, cast search_value to bigint
        sql := format('SELECT * FROM financial_institutions WHERE %I = $1::bigint', search_key);
ELSE
        -- domain_code and code are VARCHAR/TEXT, no cast needed
        sql := format('SELECT * FROM financial_institutions WHERE %I = $1', search_key);
END IF;

    -- Execute dynamically
RETURN QUERY EXECUTE sql USING search_value;
END;
$$ LANGUAGE plpgsql STABLE;
