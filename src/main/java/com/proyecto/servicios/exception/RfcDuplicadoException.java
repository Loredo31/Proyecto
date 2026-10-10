package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;

public class RfcDuplicadoException extends BusinessException {
    public RfcDuplicadoException(String message) {
        super(ApiResponseEnum.RFC_DUPLICADO, message);
    }
}

