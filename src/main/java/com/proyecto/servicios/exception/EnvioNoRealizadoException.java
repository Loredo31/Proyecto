package com.proyecto.servicios.exception;

public class EnvioNoRealizadoException extends RuntimeException {

    public EnvioNoRealizadoException(String message) {
        super(message);
    }

    public EnvioNoRealizadoException(String message, Throwable cause) {
        super(message, cause);
    }
}
