#!/usr/bin/env node
/**
 * Convierte el catálogo de GestoPago exportado de MongoDB (gestopago.catalogo_productos.json) en un archivo SQL
 * para PostgreSQL (tabla catalogo_productos). Acepta un arreglo JSON o un documento por línea (formato mongoexport).
 *
 * Uso:
 *   node scripts/catalogo-a-sql.mjs <archivo.json> [salida.sql]
 *   psql "<URL de la base>" -f salida.sql
 *
 * El SQL crea la tabla si no existe (igual que la migración V7) e inserta o actualiza cada producto
 * (ON CONFLICT), así que se puede ejecutar varias veces sin duplicar.
 */
import { readFileSync, writeFileSync } from 'node:fs';

const [, , entrada, salida = 'catalogo_productos.sql'] = process.argv;
if (!entrada) {
  console.error('Uso: node scripts/catalogo-a-sql.mjs <archivo.json> [salida.sql]');
  process.exit(1);
}

function leerDocumentos(texto) {
  const limpio = texto.replace(/^﻿/, '').trim();
  if (limpio.startsWith('[')) {
    try {
      return JSON.parse(limpio);
    } catch (e) {
      console.error(`El archivo no es un JSON válido (¿está incompleto o le falta el "]" final?): ${e.message}`);
      process.exit(1);
    }
  }
  return limpio.split(/\r?\n/).filter((l) => l.trim()).map((linea, i) => {
    try {
      return JSON.parse(linea);
    } catch (e) {
      console.error(`Línea ${i + 1} inválida: ${e.message}`);
      process.exit(1);
    }
  });
}

const entero = (v) => {
  const n = Number.parseInt(String(v ?? '').trim(), 10);
  return Number.isFinite(n) ? n : null;
};
const decimal = (v) => {
  const n = Number.parseFloat(String(v ?? '').trim());
  return Number.isFinite(n) ? n.toFixed(2) : null;
};
const texto = (v) => {
  if (v === null || v === undefined) return null;
  const s = String(v).trim();
  return s === '' ? null : s;
};
const sql = (v) => (v === null ? 'NULL' : typeof v === 'number' ? String(v) : `'${String(v).replace(/'/g, "''")}'`);

const documentos = leerDocumentos(readFileSync(entrada, 'utf8'));
const filas = new Map(); // por idProducto, el último gana
let omitidos = 0;

for (const d of documentos) {
  const idProducto = entero(d.idProducto);
  const idServicio = entero(d.idServicio);
  const nombre = texto(d.nombreProducto ?? d.producto);
  if (idProducto === null || idServicio === null || !nombre) {
    omitidos++;
    continue;
  }
  filas.set(idProducto, [
    idProducto, idServicio, texto(d.servicio), nombre.slice(0, 255), entero(d.idCatTipoServicio), entero(d.tipoFront),
    decimal(d.precio), texto(d.tipoReferencia), texto(d.legend),
  ]);
}

const columnas = 'id_producto, id_servicio, servicio, nombre_producto, id_cat_tipo_servicio, tipo_front, precio, tipo_referencia, legend';
const lineas = [
  '-- Generado por scripts/catalogo-a-sql.mjs',
  `-- Origen: ${entrada} | productos: ${filas.size} | omitidos: ${omitidos}`,
  'BEGIN;',
  `CREATE TABLE IF NOT EXISTS catalogo_productos (
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
);`,
  'CREATE INDEX IF NOT EXISTS idx_catalogo_productos_servicio ON catalogo_productos (id_servicio);',
];

const todas = [...filas.values()];
for (let i = 0; i < todas.length; i += 500) {
  const lote = todas.slice(i, i + 500)
    .map((f) => `(${f.map((v, j) => (j === 6 && v !== null ? v : sql(v))).join(', ')})`)
    .join(',\n');
  lineas.push(`INSERT INTO catalogo_productos (${columnas}) VALUES\n${lote}
ON CONFLICT (id_producto) DO UPDATE SET
  id_servicio = EXCLUDED.id_servicio, servicio = EXCLUDED.servicio, nombre_producto = EXCLUDED.nombre_producto,
  id_cat_tipo_servicio = EXCLUDED.id_cat_tipo_servicio, tipo_front = EXCLUDED.tipo_front, precio = EXCLUDED.precio,
  tipo_referencia = EXCLUDED.tipo_referencia, legend = EXCLUDED.legend, activo = TRUE, fecha_actualizacion = NOW();`);
}
lineas.push('COMMIT;', `SELECT COUNT(*) AS productos_en_catalogo FROM catalogo_productos;`, '');

writeFileSync(salida, lineas.join('\n\n'), 'utf8');
console.log(`Listo: ${filas.size} productos -> ${salida}${omitidos ? ` (${omitidos} omitidos por datos incompletos)` : ''}`);
