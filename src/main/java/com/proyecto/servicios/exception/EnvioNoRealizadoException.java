package com.proyecto.servicios.exception;

/**
 * Uso interno del flujo de pagos: garantiza que la petición sendTx NO fue recibida por GestoPago
 * (falló antes de enviarse o la conexión nunca se estableció), por lo que es seguro liberar la retención.
 */
public class EnvioNoRealizadoException extends RuntimeException {

    public EnvioNoRealizadoException(String message) {
        super(message);
    }

    public EnvioNoRealizadoException(String message, Throwable cause) {
        super(message, cause);
    }
}
