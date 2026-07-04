ALTER TABLE accounting_expenses ADD COLUMN company_id UUID REFERENCES companies(id);
CREATE INDEX idx_accounting_expenses_company_id ON accounting_expenses(company_id);
