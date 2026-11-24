-- Bienvenida (5%) solo para clientes dados de alta desde "Asociar cliente": queda pendiente hasta
-- su primera venta. Los clientes ya registrados no la tienen.
ALTER TABLE customers ADD COLUMN welcome_pending BOOLEAN NOT NULL DEFAULT FALSE;
