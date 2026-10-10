package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;

public class ContrasenaInvalidaException extends BusinessException {
    public ContrasenaInvalidaException(String message) {
        super(ApiResponseEnum.DATOS_INVALIDOS, message);
    }
}

