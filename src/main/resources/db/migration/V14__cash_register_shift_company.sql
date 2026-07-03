-- Backfill existing rows via openedBy/seller → user → company
-- Columns, FK, and indexes already exist from the previous V12 migration.
UPDATE cash_registers cr
SET company_id = u.company_id
FROM users u
WHERE cr.opened_by = u.id
  AND cr.company_id IS NULL
  AND u.company_id IS NOT NULL;

UPDATE shifts s
SET company_id = u.company_id
FROM users u
WHERE s.seller_id = u.id
  AND s.company_id IS NULL
  AND u.company_id IS NOT NULL;

-- Enforce NOT NULL now that all rows have a company
ALTER TABLE cash_registers ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE shifts ALTER COLUMN company_id SET NOT NULL;
