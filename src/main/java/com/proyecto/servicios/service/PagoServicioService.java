package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.pago.PagoServicioRequest;
import com.proyecto.servicios.dto.pago.PagoServicioResponseDto;

public interface PagoServicioService {

    PagoServicioResponseDto procesarPago(Long clienteId, String idempotencyKey, PagoServicioRequest request);

    PagoServicioResponseDto consultarPago(Long clienteId, String folio);

    /**
     * Ejecuta confirmTx.do sobre los pagos en PENDIENTE_CONCILIACION dentro de su ventana válida
     * y aplica el resultado (descontar o liberar).
     *
     * @return número de transacciones resueltas
     */
    int conciliarPendientes();
}
