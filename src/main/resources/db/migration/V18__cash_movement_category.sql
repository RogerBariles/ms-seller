-- Clasifica los movimientos de efectivo y vincula los egresos con su origen.
-- Los movimientos ya cargados quedan como OTHER y siguen afectando el resultado como antes.
ALTER TABLE shift_cash_movements ADD COLUMN category VARCHAR(30) NOT NULL DEFAULT 'OTHER';

ALTER TABLE accounting_expenses ADD COLUMN payment_source VARCHAR(20);
ALTER TABLE accounting_expenses ADD COLUMN cash_movement_id UUID REFERENCES shift_cash_movements(id);

CREATE INDEX idx_accounting_expenses_cash_movement_id ON accounting_expenses(cash_movement_id);
