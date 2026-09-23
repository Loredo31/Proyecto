package com.proyecto.servicios.enums;

import org.springframework.http.HttpStatus;

/**
 * Catálogo de respuestas estandarizadas de la API.
 * Centraliza códigos, mensajes y estado HTTP para garantizar
 * una única fuente de verdad en el manejo de errores.
 */
public enum ApiResponseEnum {

    EXITO(HttpStatus.OK, "000", "Operación exitosa"),
    CATALOGO_NO_DISPONIBLE(HttpStatus.NOT_FOUND, "40401", "El catálogo de productos no se encuentra disponible en caché"),
    SERVICIO_EXTERNO_NO_DISPONIBLE(HttpStatus.SERVICE_UNAVAILABLE, "50301", "El servicio externo de catálogo no se encuentra disponible"),
    ERROR_AUTENTICACION(HttpStatus.UNAUTHORIZED, "40101", "Autenticación fallida contra el servicio externo de catálogo"),
    TIMEOUT_SERVICIO(HttpStatus.GATEWAY_TIMEOUT, "50401", "Tiempo de espera agotado al consultar el servicio externo de catálogo"),
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR, "50001", "Error interno al procesar la solicitud");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ApiResponseEnum(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
