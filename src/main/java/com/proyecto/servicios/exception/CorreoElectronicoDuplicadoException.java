package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;

public class CorreoElectronicoDuplicadoException extends BusinessException {
    public CorreoElectronicoDuplicadoException(String message) {
        super(ApiResponseEnum.CORREO_DUPLICADO, message);
    }
}

