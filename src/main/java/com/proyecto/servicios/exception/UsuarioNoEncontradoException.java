package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;

public class UsuarioNoEncontradoException extends BusinessException {
    public UsuarioNoEncontradoException(String message) {
        super(ApiResponseEnum.CLIENTE_NO_ENCONTRADO, message);
    }
}

