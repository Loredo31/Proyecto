package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;

public class CredencialesInvalidasException extends BusinessException {
    public CredencialesInvalidasException(String message) {
        super(ApiResponseEnum.CREDENCIALES_INVALIDAS, message);
    }
}

