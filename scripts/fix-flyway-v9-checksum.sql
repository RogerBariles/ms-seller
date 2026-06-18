-- Borra el registro de V9 para que Flyway la vuelva a aplicar con el checksum actual.
-- Seguro si la columna ya existe: V9 usa ADD COLUMN IF NOT EXISTS.

DELETE FROM flyway_schema_history WHERE version = '9';
