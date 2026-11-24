-- Catálogo de productos de GestoPago (antes en MongoDB). Lo llena la sincronización diaria con
-- getProductList.do o la carga inicial con scripts/catalogo-a-sql.mjs (inserta con ON CONFLICT).
CREATE TABLE IF NOT EXISTS catalogo_productos (
    id_producto          INTEGER        PRIMARY KEY,
    id_servicio          INTEGER        NOT NULL,
    servicio             VARCHAR(150),
    nombre_producto      VARCHAR(255)   NOT NULL,
    id_cat_tipo_servicio INTEGER,
    tipo_front           INTEGER,
    precio               NUMERIC(12, 2),
    tipo_referencia      VARCHAR(10),
    legend               TEXT,
    activo               BOOLEAN        NOT NULL DEFAULT TRUE,
    fecha_actualizacion  TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_catalogo_productos_servicio ON catalogo_productos (id_servicio);
