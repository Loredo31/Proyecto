package com.proyecto.servicios.enums;

/**
 * Ciclo de vida de un pago enviado a GestoPago.
 *
 * <pre>
 * PROCESANDO ──01──────────────▶ EXITOSA                (saldo descontado)
 *     │      ──rechazo─────────▶ RECHAZADA              (retención liberada)
 *     │      ──no enviado──────▶ ERROR                  (retención liberada)
 *     └──────timeout / 5xx ────▶ PENDIENTE_CONCILIACION (retención se mantiene)
 * </pre>
 */
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
