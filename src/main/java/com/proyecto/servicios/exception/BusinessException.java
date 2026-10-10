package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ApiResponseEnum responseStatus;

    public BusinessException(ApiResponseEnum responseStatus) {
        super(responseStatus.getMessage());
        this.responseStatus = responseStatus;
    }

    public BusinessException(ApiResponseEnum responseStatus, String message) {
        super(message);
        this.responseStatus = responseStatus;
    }
}

