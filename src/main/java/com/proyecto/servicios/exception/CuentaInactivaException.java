package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;

public class CuentaInactivaException extends BusinessException {
    public CuentaInactivaException(String message) {
        super(ApiResponseEnum.CUENTA_INACTIVA, message);
    }
}
