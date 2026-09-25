ALTER TABLE sales ADD COLUMN accredited_at DATE;

CREATE OR REPLACE FUNCTION add_business_days(start_date DATE, days INTEGER)
RETURNS DATE AS $$
DECLARE
    result DATE := start_date;
    added INTEGER := 0;
BEGIN
    WHILE added < days LOOP
        result := result + 1;
        IF EXTRACT(ISODOW FROM result) < 6 THEN
            added := added + 1;
        END IF;
    END LOOP;
    RETURN result;
END;
$$ LANGUAGE plpgsql;

UPDATE sales
SET accredited_at = (created_at AT TIME ZONE 'America/Argentina/Buenos_Aires')::date
WHERE payment_method IN ('EFECTIVO', 'TRANSFERENCIA');

UPDATE sales
SET accredited_at = add_business_days(
        (created_at AT TIME ZONE 'America/Argentina/Buenos_Aires')::date,
        2)
WHERE payment_method IN ('TARJETA', 'DEBITO', 'QR');

UPDATE sales
SET accredited_at = ((created_at AT TIME ZONE 'America/Argentina/Buenos_Aires')::date + INTERVAL '7 days')::date
WHERE payment_method = 'PEDIDOSYA';

UPDATE sales
SET accredited_at = (created_at AT TIME ZONE 'America/Argentina/Buenos_Aires')::date
WHERE accredited_at IS NULL;

ALTER TABLE sales ALTER COLUMN accredited_at SET NOT NULL;

DROP FUNCTION add_business_days(DATE, INTEGER);
