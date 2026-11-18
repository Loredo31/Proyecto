package com.proyecto.servicios.exception;

/**
 * El proveedor de correo no aceptó el mensaje. No hereda de {@link BusinessException} a propósito:
 * así la transacción se revierte y el código que no llegó al usuario no queda registrado.
 */
public class CorreoNoEnviadoException extends RuntimeException {

    public CorreoNoEnviadoException(String message) {
        super(message);
    }

    public CorreoNoEnviadoException(String message, Throwable cause) {
        super(message, cause);
    }
}
