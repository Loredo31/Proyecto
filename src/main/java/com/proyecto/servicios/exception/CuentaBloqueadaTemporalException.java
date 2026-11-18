package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;

public class CuentaBloqueadaTemporalException extends BusinessException {

    public CuentaBloqueadaTemporalException(long minutosRestantes) {
        super(ApiResponseEnum.CUENTA_BLOQUEADA_TEMPORAL,
                "La cuenta está bloqueada por demasiados intentos fallidos. Intente de nuevo en "
                        + Math.max(1, minutosRestantes) + " minuto(s)");
    }
}
