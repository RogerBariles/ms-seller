-- Efectivo que efectivamente queda en la caja al cerrarla (contado por quien cierra).
-- La próxima caja de la empresa se abre con este monto. NULL en las cajas cerradas antes de este cambio.
ALTER TABLE cash_registers ADD COLUMN closing_cash NUMERIC(12, 2);
