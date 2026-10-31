package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;

public class PinInvalidoException extends BusinessException {
    public PinInvalidoException() {
        super(ApiResponseEnum.PIN_INVALIDO);
    }
}
