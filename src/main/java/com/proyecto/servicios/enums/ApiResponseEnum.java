package com.proyecto.servicios.enums;

import org.springframework.http.HttpStatus;

public enum ApiResponseEnum {

    EXITO(HttpStatus.OK, "000", "Operación exitosa"),
    CREADO(HttpStatus.CREATED, "201", "Registro creado exitosamente"),
    CURP_DUPLICADO(HttpStatus.CONFLICT, "40901", "La CURP ya se encuentra registrada"),
    RFC_DUPLICADO(HttpStatus.CONFLICT, "40902", "El RFC ya se encuentra registrado"),
    TELEFONO_DUPLICADO(HttpStatus.CONFLICT, "40903", "El teléfono móvil ya se encuentra registrado"),
    CORREO_DUPLICADO(HttpStatus.CONFLICT, "40904", "El correo electrónico ya se encuentra registrado"),
    CLIENTE_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "40402", "El cliente no fue encontrado"),
    CUENTA_NO_ENCONTRADA(HttpStatus.NOT_FOUND, "40403", "La cuenta bancaria no fue encontrada"),
    CUENTA_BLOQUEADA(HttpStatus.FORBIDDEN, "40301", "La cuenta bancaria se encuentra bloqueada"),
    ACCESO_DENEGADO(HttpStatus.FORBIDDEN, "40302", "El acceso a la aplicación se encuentra bloqueado"),
    DATOS_INVALIDOS(HttpStatus.BAD_REQUEST, "40001", "Los datos enviados son inválidos"),
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

