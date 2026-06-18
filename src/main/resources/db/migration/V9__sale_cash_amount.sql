ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS cash_amount NUMERIC(12, 2) NOT NULL DEFAULT 0;

UPDATE sales
SET cash_amount = total
WHERE payment_method = 'EFECTIVO'
  AND cash_amount = 0;
