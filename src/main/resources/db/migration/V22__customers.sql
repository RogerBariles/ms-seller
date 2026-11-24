-- Clientes que se pueden asociar a una venta. El DNI es único en todo el sistema
-- (un mismo cliente puede comprar en cualquier sucursal). WhatsApp o mail: al menos uno.
CREATE TABLE customers (
    id UUID PRIMARY KEY,
    full_name VARCHAR(160) NOT NULL,
    dni VARCHAR(15) NOT NULL UNIQUE,
    birth_date DATE NOT NULL,
    whatsapp VARCHAR(30),
    email VARCHAR(180),
    company_id UUID REFERENCES companies(id),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_customers_contact CHECK (whatsapp IS NOT NULL OR email IS NOT NULL)
);

ALTER TABLE sales ADD COLUMN customer_id UUID REFERENCES customers(id);
CREATE INDEX idx_sales_customer_id ON sales(customer_id);
