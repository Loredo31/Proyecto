package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;

public class SaldoInsuficienteException extends BusinessException {
    public SaldoInsuficienteException() {
        super(ApiResponseEnum.SALDO_INSUFICIENTE);
    }

    /** Mismo código (42201) con un mensaje que explica el motivo, p. ej. saldo retenido por pagos en verificación. */
    public SaldoInsuficienteException(String mensaje) {
        super(ApiResponseEnum.SALDO_INSUFICIENTE, mensaje);
    }
}
