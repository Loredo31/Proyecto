CREATE TABLE IF NOT EXISTS clientes (
    id                          BIGSERIAL PRIMARY KEY,
    nombre                      VARCHAR(50)     NOT NULL,
    segundo_nombre              VARCHAR(50),
    apellido_paterno            VARCHAR(50)     NOT NULL,
    apellido_materno            VARCHAR(50)     NOT NULL,
    fecha_nacimiento            DATE            NOT NULL,
    curp                        VARCHAR(18)     NOT NULL UNIQUE,
    rfc                         VARCHAR(13)     NOT NULL UNIQUE,
    sexo                        VARCHAR(20)     NOT NULL,
    nacionalidad                VARCHAR(50)     NOT NULL,
    estado_civil                VARCHAR(30)     NOT NULL,
    correo_electronico          VARCHAR(100)    NOT NULL UNIQUE,
    telefono_movil              VARCHAR(10)     NOT NULL UNIQUE,
    telefono_alternativo        VARCHAR(10),
    laboral_ocupacion           VARCHAR(100)    NOT NULL,
    laboral_empresa             VARCHAR(150)    NOT NULL,
    laboral_ingreso_mensual     NUMERIC(15, 2)  NOT NULL CHECK (laboral_ingreso_mensual > 0),
    biometria_enrolada          BOOLEAN         NOT NULL DEFAULT FALSE,
    biometria_tipo              VARCHAR(30),
    biometria_token             TEXT,
    biometria_fecha_registro    TIMESTAMP,
    activo_login                BOOLEAN         NOT NULL DEFAULT TRUE,
    eliminado_logico            BOOLEAN         NOT NULL DEFAULT FALSE,
    fecha_creacion              TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion         TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS domicilios (
    id                          BIGSERIAL PRIMARY KEY,
    cliente_id                  BIGINT          NOT NULL UNIQUE REFERENCES clientes(id) ON DELETE CASCADE,
    calle                       VARCHAR(150)    NOT NULL,
    num_exterior                VARCHAR(20)     NOT NULL,
    num_interior                VARCHAR(20),
    colonia                     VARCHAR(100)    NOT NULL,
    municipio                   VARCHAR(100)    NOT NULL,
    estado                      VARCHAR(100)    NOT NULL,
    codigo_postal               VARCHAR(5)      NOT NULL,
    pais                        VARCHAR(50)     NOT NULL
);

CREATE TABLE IF NOT EXISTS cuentas (
    id                      BIGSERIAL PRIMARY KEY,
    numero_cuenta           VARCHAR(16)     NOT NULL UNIQUE,
    saldo                   NUMERIC(15, 2)  NOT NULL DEFAULT 0.00 CHECK (saldo >= 0),
    estado                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVA',
    cliente_id              BIGINT          NOT NULL REFERENCES clientes(id) ON DELETE RESTRICT,
    fecha_creacion          TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion     TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS usuarios (
    id                      BIGSERIAL PRIMARY KEY,
    correo                  VARCHAR(100)    NOT NULL UNIQUE,
    password                VARCHAR(255)    NOT NULL,
    activo                  BOOLEAN         NOT NULL DEFAULT TRUE,
    cliente_id              BIGINT          NOT NULL UNIQUE REFERENCES clientes(id) ON DELETE CASCADE,
    fecha_creacion          TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion     TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_clientes_curp ON clientes(curp);
CREATE INDEX IF NOT EXISTS idx_clientes_rfc ON clientes(rfc);
CREATE INDEX IF NOT EXISTS idx_clientes_correo ON clientes(correo_electronico);
CREATE INDEX IF NOT EXISTS idx_clientes_telefono ON clientes(telefono_movil);
CREATE INDEX IF NOT EXISTS idx_clientes_fechas ON clientes(fecha_creacion);
CREATE INDEX IF NOT EXISTS idx_cuentas_numero ON cuentas(numero_cuenta);
CREATE INDEX IF NOT EXISTS idx_cuentas_cliente ON cuentas(cliente_id);
CREATE INDEX IF NOT EXISTS idx_cuentas_estado ON cuentas(estado);
CREATE INDEX IF NOT EXISTS idx_usuarios_correo ON usuarios(correo);
CREATE INDEX IF NOT EXISTS idx_usuarios_cliente ON usuarios(cliente_id);
