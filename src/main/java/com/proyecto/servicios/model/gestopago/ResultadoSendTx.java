package com.proyecto.servicios.model.gestopago;

/**
 * Resultado de negocio de un envío a sendTx.do o confirmTx.do, ya clasificado.
 *
 * @param respuesta respuesta parseada del proveedor; nula si no hubo respuesta interpretable
 */
public record ResultadoSendTx(Tipo tipo, String codigo, String mensaje, SendTxRespuesta respuesta) {

    public enum Tipo {
        /** Transacción aplicada: se descuenta el saldo. */
        APROBADA,
        /** El proveedor indicó explícitamente que no aplicó el pago: se libera la retención. */
        RECHAZADA,
        /** No hay certeza (timeout, 5xx, código 06/82, respuesta ilegible): se resuelve con confirmTx. */
        INDETERMINADA
    }

    public static ResultadoSendTx aprobada(SendTxRespuesta respuesta) {
        return new ResultadoSendTx(Tipo.APROBADA, respuesta.codigo(), respuesta.mensaje(), respuesta);
    }

    public static ResultadoSendTx rechazada(String codigo, String mensaje) {
        return new ResultadoSendTx(Tipo.RECHAZADA, codigo, mensaje, null);
    }

    public static ResultadoSendTx rechazada(SendTxRespuesta respuesta) {
        return new ResultadoSendTx(Tipo.RECHAZADA, respuesta.codigo(), respuesta.mensaje(), respuesta);
    }

    public static ResultadoSendTx indeterminada(String codigo, String mensaje) {
        return new ResultadoSendTx(Tipo.INDETERMINADA, codigo, mensaje, null);
    }

    public static ResultadoSendTx indeterminada(SendTxRespuesta respuesta) {
        return new ResultadoSendTx(Tipo.INDETERMINADA, respuesta.codigo(), respuesta.mensaje(), respuesta);
    }
}
