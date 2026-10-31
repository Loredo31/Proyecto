package com.proyecto.servicios.model.gestopago;

public record ResultadoSendTx(Tipo tipo, String codigo, String mensaje, SendTxRespuesta respuesta) {

    public enum Tipo {
        APROBADA,
        RECHAZADA,
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
