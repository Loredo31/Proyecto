package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;

public class ClienteYaRegistradoException extends BusinessException {
    public ClienteYaRegistradoException(String message) {
        super(ApiResponseEnum.DATOS_INVALIDOS, message);
    }
}

