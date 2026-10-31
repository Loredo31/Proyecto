package com.proyecto.servicios.enums;

public enum EstadoTransaccion {
    PROCESANDO,
    EXITOSA,
    RECHAZADA,
    ERROR,
    PENDIENTE_CONCILIACION;

    public boolean esFinal() {
        return this == EXITOSA || this == RECHAZADA || this == ERROR;
    }
}
