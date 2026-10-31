package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.pago.PagoServicioRequest;
import com.proyecto.servicios.dto.pago.PagoServicioResponseDto;

public interface PagoServicioService {

    PagoServicioResponseDto procesarPago(Long clienteId, String idempotencyKey, PagoServicioRequest request);

    PagoServicioResponseDto consultarPago(Long clienteId, String folio);

    int conciliarPendientes();
}
