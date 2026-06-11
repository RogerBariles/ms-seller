ALTER TABLE cash_registers DROP CONSTRAINT IF EXISTS cash_registers_business_date_key;

CREATE INDEX IF NOT EXISTS idx_cash_registers_business_date ON cash_registers(business_date);
CREATE INDEX IF NOT EXISTS idx_cash_registers_business_date_status ON cash_registers(business_date, status);
