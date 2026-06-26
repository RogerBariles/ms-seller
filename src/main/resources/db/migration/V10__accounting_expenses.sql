CREATE TABLE accounting_expenses (
    id UUID PRIMARY KEY,
    detail VARCHAR(500) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_accounting_expenses_created_at ON accounting_expenses(created_at);
