package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;

public class TransaccionDuplicadaException extends BusinessException {
    public TransaccionDuplicadaException(String message) {
        super(ApiResponseEnum.TRANSACCION_DUPLICADA, message);
    }
}
