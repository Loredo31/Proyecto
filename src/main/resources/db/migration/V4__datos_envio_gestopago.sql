-- Parámetros exactos enviados a sendTx.do (confirmTx.do exige reenviar los mismos)
-- y evidencia de la respuesta del proveedor (requisito "Log evidence" de GestoPago).
ALTER TABLE transacciones_pago ADD COLUMN IF NOT EXISTS upc                     VARCHAR(15);
ALTER TABLE transacciones_pago ADD COLUMN IF NOT EXISTS hora_local              VARCHAR(14);
ALTER TABLE transacciones_pago ADD COLUMN IF NOT EXISTS telefono_envio          VARCHAR(20);
ALTER TABLE transacciones_pago ADD COLUMN IF NOT EXISTS unidad                  VARCHAR(25);
ALTER TABLE transacciones_pago ADD COLUMN IF NOT EXISTS id_tx_proveedor         VARCHAR(30);
ALTER TABLE transacciones_pago ADD COLUMN IF NOT EXISTS codigo_canje            VARCHAR(250);
ALTER TABLE transacciones_pago ADD COLUMN IF NOT EXISTS leyenda                 TEXT;
ALTER TABLE transacciones_pago ADD COLUMN IF NOT EXISTS respuesta_proveedor     TEXT;
ALTER TABLE transacciones_pago ADD COLUMN IF NOT EXISTS respuesta_confirmacion  TEXT;

-- GestoPago admite referencias de hasta 100 caracteres (códigos de barras de recibos)
ALTER TABLE transacciones_pago ALTER COLUMN referencia TYPE VARCHAR(100);

-- Filas creadas antes de esta versión no tienen datos de envío; se marcan para revisión manual.
UPDATE transacciones_pago SET upc = 'LEGACY' || id WHERE upc IS NULL;

ALTER TABLE transacciones_pago ALTER COLUMN upc SET NOT NULL;
ALTER TABLE transacciones_pago ADD CONSTRAINT uq_transacciones_pago_upc UNIQUE (upc);
