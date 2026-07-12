-- Stock management
-- Adds current_stock column to products and creates stock_movements table

ALTER TABLE products ADD COLUMN current_stock INTEGER NOT NULL DEFAULT 0;

CREATE TABLE stock_movements (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id),
    quantity_change INTEGER NOT NULL,
    type VARCHAR(20) NOT NULL,
    reference_type VARCHAR(20) NOT NULL,
    reference_id UUID,
    notes TEXT,
    company_id UUID NOT NULL REFERENCES companies(id),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stock_movements_product_id ON stock_movements(product_id);
CREATE INDEX idx_stock_movements_company_id ON stock_movements(company_id);
CREATE INDEX idx_stock_movements_created_at ON stock_movements(created_at DESC);
