-- Descuento de bienvenida (5%) que recibe un cliente en su primera compra.
-- Ya está incluido en discount_total; se guarda aparte para poder identificarlo en reportes.
ALTER TABLE sales ADD COLUMN customer_discount NUMERIC(12, 2) NOT NULL DEFAULT 0;
