-- =====================================================================
-- V6: Codigos de verificacion por correo (2FA, recuperacion de contrasena
--     y cierre de sesion por correo). Se retira el rol de administrador.
-- =====================================================================

ALTER TABLE usuarios DROP COLUMN IF EXISTS rol;

-- Todo usuario vuelve a estar ligado a un cliente (solo si no hay registros sin cliente)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM usuarios WHERE cliente_id IS NULL) THEN
        ALTER TABLE usuarios ALTER COLUMN cliente_id SET NOT NULL;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS codigos_verificacion (
    id                  VARCHAR(36)  PRIMARY KEY,
    usuario_id          BIGINT       NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    proposito           VARCHAR(30)  NOT NULL,
    codigo_hash         VARCHAR(100) NOT NULL,
    ticket_hash         VARCHAR(64)  NOT NULL UNIQUE,
    referencia          VARCHAR(36),
    plataforma          VARCHAR(10),
    dispositivo         VARCHAR(150),
    estado              VARCHAR(20)  NOT NULL,
    intentos            INTEGER      NOT NULL DEFAULT 0,
    reenvios            INTEGER      NOT NULL DEFAULT 0,
    fecha_creacion      TIMESTAMP    NOT NULL DEFAULT NOW(),
    fecha_envio         TIMESTAMP    NOT NULL DEFAULT NOW(),
    expira_en           TIMESTAMP    NOT NULL,
    fecha_uso           TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_codigos_usuario_proposito ON codigos_verificacion(usuario_id, proposito, fecha_creacion);
