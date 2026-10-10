package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;

public class UsuarioInactivoException extends BusinessException {
    public UsuarioInactivoException(String message) {
        super(ApiResponseEnum.ACCESO_DENEGADO, message);
    }
}

