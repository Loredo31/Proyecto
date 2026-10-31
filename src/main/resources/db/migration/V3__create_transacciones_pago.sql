-- Saldo retenido: monto apartado por pagos en curso o en conciliación (saldo disponible = saldo - saldo_retenido)
ALTER TABLE cuentas ADD COLUMN IF NOT EXISTS saldo_retenido        NUMERIC(15, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE cuentas ADD COLUMN IF NOT EXISTS pin_seguridad         VARCHAR(255);
ALTER TABLE cuentas ADD COLUMN IF NOT EXISTS intentos_pin_fallidos INTEGER        NOT NULL DEFAULT 0;
ALTER TABLE cuentas ADD CONSTRAINT chk_cuentas_saldo_retenido CHECK (saldo_retenido >= 0 AND saldo_retenido <= saldo);

CREATE TABLE IF NOT EXISTS transacciones_pago (
    id                      BIGSERIAL PRIMARY KEY,
    folio                   VARCHAR(36)     NOT NULL UNIQUE,
    idempotency_key         VARCHAR(64)     UNIQUE,
    cuenta_id               BIGINT          NOT NULL REFERENCES cuentas(id) ON DELETE RESTRICT,
    numero_cuenta           VARCHAR(16)     NOT NULL,
    cliente_id              BIGINT          NOT NULL,
    id_servicio             INTEGER         NOT NULL,
    id_producto             INTEGER         NOT NULL,
    referencia              VARCHAR(50)     NOT NULL,
    con_referencia          BOOLEAN         NOT NULL,
    monto                   NUMERIC(15, 2)  NOT NULL CHECK (monto > 0),
    estado                  VARCHAR(30)     NOT NULL,
    codigo_respuesta        VARCHAR(20),
    mensaje_respuesta       VARCHAR(500),
    numero_autorizacion     VARCHAR(100),
    version                 BIGINT          NOT NULL DEFAULT 0,
    fecha_creacion          TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion     TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tx_pago_duplicados ON transacciones_pago(cuenta_id, referencia, monto, fecha_creacion);
CREATE INDEX IF NOT EXISTS idx_tx_pago_estado     ON transacciones_pago(estado, fecha_actualizacion);
CREATE INDEX IF NOT EXISTS idx_tx_pago_cliente    ON transacciones_pago(cliente_id);
