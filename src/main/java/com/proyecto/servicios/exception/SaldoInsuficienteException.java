package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;

public class SaldoInsuficienteException extends BusinessException {
    public SaldoInsuficienteException() {
        super(ApiResponseEnum.SALDO_INSUFICIENTE);
    }
}
