-- =====================================================================
-- V5: Seguridad - roles, bloqueo por intentos fallidos y sesiones unicas
-- =====================================================================

-- Roles y bloqueo temporal de la cuenta de acceso
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS rol               VARCHAR(20) NOT NULL DEFAULT 'USER';
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS intentos_fallidos INTEGER     NOT NULL DEFAULT 0;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS bloqueado_hasta   TIMESTAMP;

-- Los administradores no estan ligados a un cliente
ALTER TABLE usuarios ALTER COLUMN cliente_id DROP NOT NULL;

-- Sesiones activas (una por usuario) respaldadas por el servidor
CREATE TABLE IF NOT EXISTS sesiones (
    id                      VARCHAR(36)  PRIMARY KEY,
    usuario_id              BIGINT       NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    plataforma              VARCHAR(10)  NOT NULL,
    dispositivo             VARCHAR(150),
    refresh_hash            VARCHAR(64)  NOT NULL UNIQUE,
    refresh_hash_anterior   VARCHAR(64),
    estado                  VARCHAR(20)  NOT NULL,
    motivo_cierre           VARCHAR(150),
    fecha_creacion          TIMESTAMP    NOT NULL DEFAULT NOW(),
    ultima_actividad        TIMESTAMP    NOT NULL DEFAULT NOW(),
    expira_en               TIMESTAMP    NOT NULL,
    fecha_cierre            TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sesiones_usuario_estado ON sesiones(usuario_id, estado);
CREATE INDEX IF NOT EXISTS idx_sesiones_refresh_anterior ON sesiones(refresh_hash_anterior);

-- Solicitudes para cerrar la sesion activa y abrir una nueva en otro dispositivo
CREATE TABLE IF NOT EXISTS solicitudes_sesion (
    id                      VARCHAR(36)  PRIMARY KEY,
    usuario_id              BIGINT       NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    sesion_actual_id        VARCHAR(36)  NOT NULL REFERENCES sesiones(id) ON DELETE CASCADE,
    plataforma              VARCHAR(10)  NOT NULL,
    dispositivo             VARCHAR(150),
    secreto_hash            VARCHAR(64)  NOT NULL,
    estado                  VARCHAR(20)  NOT NULL,
    fecha_creacion          TIMESTAMP    NOT NULL DEFAULT NOW(),
    expira_en               TIMESTAMP    NOT NULL,
    fecha_respuesta         TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_solicitudes_usuario_estado ON solicitudes_sesion(usuario_id, estado);

-- Nombre del producto comprado (historial consistente aunque cambie el catalogo)
ALTER TABLE transacciones_pago ADD COLUMN IF NOT EXISTS nombre_producto VARCHAR(150);
CREATE INDEX IF NOT EXISTS idx_tx_pago_cliente_estado_fecha ON transacciones_pago(cliente_id, estado, fecha_creacion DESC);
