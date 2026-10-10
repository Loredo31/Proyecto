package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;
import lombok.Getter;

@Getter
public class ExternalServiceException extends RuntimeException {

    private final int httpStatus;

    public ExternalServiceException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public ExternalServiceException(String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public ApiResponseEnum toResponseStatus() {
        return switch (httpStatus) {
            case 401, 403 -> ApiResponseEnum.ERROR_AUTENTICACION;
            default -> ApiResponseEnum.SERVICIO_EXTERNO_NO_DISPONIBLE;
        };
    }
}

