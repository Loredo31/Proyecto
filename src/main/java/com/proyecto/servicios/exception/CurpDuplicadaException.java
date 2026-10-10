package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;

public class CurpDuplicadaException extends BusinessException {
    public CurpDuplicadaException(String message) {
        super(ApiResponseEnum.CURP_DUPLICADO, message);
    }
}

