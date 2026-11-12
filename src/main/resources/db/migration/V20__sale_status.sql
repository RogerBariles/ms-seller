-- Estado de la venta para poder anularla.
-- Las ventas existentes quedan como vendidas y con detalle vacío.
ALTER TABLE sales ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'SOLD';
ALTER TABLE sales ADD COLUMN detail VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE sales ADD COLUMN cancelled_at TIMESTAMPTZ;
ALTER TABLE sales ADD COLUMN cancelled_by UUID REFERENCES users(id);

CREATE INDEX idx_sales_status ON sales(status);
