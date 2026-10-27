package com.proyecto.servicios.model.gestopago;

/**
 * Parámetros de sendTx que se generan una sola vez por transacción y se persisten, para poder
 * reenviarlos idénticos en confirmTx.
 */
public record DatosEnvioTx(String upc, String horaLocal, String telefono, String unidad) {
}
