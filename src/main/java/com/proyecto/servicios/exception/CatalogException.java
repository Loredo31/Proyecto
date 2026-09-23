package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;
import lombok.Getter;

/**
 * Excepción de negocio del catálogo que transporta la
 * respuesta estandarizada definida en {@link ApiResponseEnum}.
 */
@Getter
public class CatalogException extends RuntimeException {

    private final ApiResponseEnum responseStatus;

    public CatalogException(ApiResponseEnum responseStatus) {
        super(responseStatus.getMessage());
        this.responseStatus = responseStatus;
    }

    public CatalogException(ApiResponseEnum responseStatus, Throwable cause) {
        super(responseStatus.getMessage(), cause);
        this.responseStatus = responseStatus;
    }
}
