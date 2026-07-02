ALTER TABLE products ADD COLUMN company_id UUID REFERENCES companies(id);
CREATE INDEX idx_products_company_id ON products(company_id);
