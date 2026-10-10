package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;

public class ValidacionNegocioException extends BusinessException {
    public ValidacionNegocioException(String message) {
        super(ApiResponseEnum.DATOS_INVALIDOS, message);
    }
}

