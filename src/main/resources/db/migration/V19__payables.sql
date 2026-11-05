-- Gastos fijos: plantillas que generan una cuenta a pagar por mes.
CREATE TABLE recurring_payables (
    id UUID PRIMARY KEY,
    detail VARCHAR(500) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    day_of_month INTEGER NOT NULL CHECK (day_of_month BETWEEN 1 AND 31),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    start_date DATE NOT NULL,
    company_id UUID NOT NULL REFERENCES companies(id),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recurring_payables_company_id ON recurring_payables(company_id);

-- Cuentas a pagar: recién generan un egreso cuando se pagan.
CREATE TABLE payables (
    id UUID PRIMARY KEY,
    detail VARCHAR(500) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_source VARCHAR(20),
    recurring_id UUID REFERENCES recurring_payables(id),
    period DATE,
    paid_at TIMESTAMPTZ,
    paid_by UUID REFERENCES users(id),
    company_id UUID NOT NULL REFERENCES companies(id),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_payables_recurring_period UNIQUE (recurring_id, period)
);

CREATE INDEX idx_payables_company_status_due ON payables(company_id, status, due_date);
