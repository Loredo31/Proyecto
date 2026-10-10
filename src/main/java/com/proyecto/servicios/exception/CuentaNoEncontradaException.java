package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;

public class CuentaNoEncontradaException extends BusinessException {
    public CuentaNoEncontradaException(String message) {
        super(ApiResponseEnum.CUENTA_NO_ENCONTRADA, message);
    }
}

