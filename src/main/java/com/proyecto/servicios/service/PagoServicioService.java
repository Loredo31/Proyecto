package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.PaginaDto;
import com.proyecto.servicios.dto.pago.PagoServicioRequest;
import com.proyecto.servicios.dto.pago.PagoServicioResponseDto;

public interface PagoServicioService {

    PagoServicioResponseDto procesarPago(Long clienteId, String idempotencyKey, PagoServicioRequest request);

    PagoServicioResponseDto consultarPago(Long clienteId, String folio);

    int conciliarPendientes();

    /** Compras exitosas del cliente, de la más reciente a la más antigua. */
    PaginaDto<PagoServicioResponseDto> historial(Long clienteId, int pagina, int tamano);

    /** Pagos en proceso o en conciliación con el proveedor (saldo retenido). */
    PaginaDto<PagoServicioResponseDto> pendientes(Long clienteId, int pagina, int tamano);

    /** Pagos rechazados por el proveedor o no realizados (sin cargo; el saldo se liberó). */
    PaginaDto<PagoServicioResponseDto> rechazadas(Long clienteId, int pagina, int tamano);

    /** Consulta a GestoPago (confirmTx) el resultado real de un pago pendiente del cliente. */
    PagoServicioResponseDto verificarPago(Long clienteId, String folio);
}
