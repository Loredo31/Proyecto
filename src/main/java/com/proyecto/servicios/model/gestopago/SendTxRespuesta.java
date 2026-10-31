package com.proyecto.servicios.model.gestopago;

public record SendTxRespuesta(String codigo, String mensaje, String numeroAutorizacion, String idTx,
                              String codigoCanje, String leyenda, String cuerpo) {
}
