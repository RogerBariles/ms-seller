ALTER TABLE sales ADD COLUMN company_id UUID REFERENCES companies(id);
CREATE INDEX idx_sales_company_id ON sales(company_id);
