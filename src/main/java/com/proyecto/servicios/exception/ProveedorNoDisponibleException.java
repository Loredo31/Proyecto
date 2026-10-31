package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;

/**
 * El pago NO llegó a GestoPago (token, cifrado o conexión fallidos) y la retención ya fue liberada.
 */
public class ProveedorNoDisponibleException extends BusinessException {
    public ProveedorNoDisponibleException() {
        super(ApiResponseEnum.PROVEEDOR_PAGOS_NO_DISPONIBLE);
    }
}
