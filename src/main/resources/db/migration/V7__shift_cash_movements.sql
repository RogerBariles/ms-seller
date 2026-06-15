CREATE TABLE shift_cash_movements (
    id UUID PRIMARY KEY,
    shift_id UUID NOT NULL REFERENCES shifts(id),
    movement_type VARCHAR(20) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    detail VARCHAR(500) NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_shift_cash_movements_shift_id ON shift_cash_movements(shift_id);
