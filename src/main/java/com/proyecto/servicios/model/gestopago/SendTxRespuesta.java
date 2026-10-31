package com.proyecto.servicios.model.gestopago;

/**
 * Campos relevantes de la respuesta XML de sendTx.do / confirmTx.do. {@code cuerpo} conserva la
 * respuesta cruda como evidencia.
 */
public record SendTxRespuesta(String codigo, String mensaje, String numeroAutorizacion, String idTx,
                              String codigoCanje, String leyenda, String cuerpo) {
}
