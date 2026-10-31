package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;

public class ProveedorNoDisponibleException extends BusinessException {
    public ProveedorNoDisponibleException() {
        super(ApiResponseEnum.PROVEEDOR_PAGOS_NO_DISPONIBLE);
    }
}
