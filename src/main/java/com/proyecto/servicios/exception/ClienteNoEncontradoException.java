package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;

public class ClienteNoEncontradoException extends BusinessException {
    public ClienteNoEncontradoException(String message) {
        super(ApiResponseEnum.CLIENTE_NO_ENCONTRADO, message);
    }
}

