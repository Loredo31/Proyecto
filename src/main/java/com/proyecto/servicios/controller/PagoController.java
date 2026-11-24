package com.proyecto.servicios.controller;

import com.proyecto.servicios.dto.ApiResponse;
import com.proyecto.servicios.dto.PaginaDto;
import com.proyecto.servicios.dto.pago.ConsultaHistorialRequest;
import com.proyecto.servicios.dto.pago.ConsultaPagoRequest;
import com.proyecto.servicios.dto.pago.PagoServicioRequest;
import com.proyecto.servicios.dto.pago.PagoServicioResponseDto;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.seguridad.AccesoRecursos;
import com.proyecto.servicios.service.PagoServicioService;
import com.proyecto.servicios.service.PinSeguridadService;
import com.proyecto.servicios.util.DatosSensiblesUtil;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoServicioService pagoServicioService;
    private final AccesoRecursos accesoRecursos;

    /** Ejecuta la compra. El PIN de la cuenta viaja en el header X-Auth-Pin y se valida en el servidor. */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagoServicioResponseDto>> procesarPago(
            @Parameter(description = "PIN de seguridad de la cuenta (4 a 6 dígitos)")
            @RequestHeader(value = PinSeguridadService.HEADER, required = false) String pin,
            @Valid @RequestBody PagoServicioRequest request) {

        Long clienteId = accesoRecursos.clienteActual();
        PinSeguridadService.exigirFormato(pin);
        request.setPin(pin);
        log.info("Petición de pago: cliente={}, cuenta={}, idServicio={}, idProducto={}",
                clienteId, DatosSensiblesUtil.enmascarar(String.valueOf(request.getNumCuenta())),
                request.getIdServicio(), request.getIdProducto());

        return responder(pagoServicioService.procesarPago(clienteId, request.getIdempotencyKey(), request));
    }

    @PostMapping(value = "/detalle", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagoServicioResponseDto>> consultarPago(@Valid @RequestBody ConsultaPagoRequest request) {
        return responder(pagoServicioService.consultarPago(accesoRecursos.clienteActual(), request.getFolio()));
    }

    /** Historial de compras exitosas del usuario autenticado. */
    @PostMapping(value = "/historial", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PaginaDto<PagoServicioResponseDto>>> historial(
            @Valid @RequestBody(required = false) ConsultaHistorialRequest request) {
        ConsultaHistorialRequest pagina = request != null ? request : new ConsultaHistorialRequest();
        return ResponseEntity.ok(ApiResponse.success(
                pagoServicioService.historial(accesoRecursos.clienteActual(), pagina.getPagina(), pagina.getTamano())));
    }

    /** Pagos en verificación con el proveedor (saldo retenido). */
    @PostMapping(value = "/pendientes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PaginaDto<PagoServicioResponseDto>>> pendientes(
            @Valid @RequestBody(required = false) ConsultaHistorialRequest request) {
        ConsultaHistorialRequest pagina = request != null ? request : new ConsultaHistorialRequest();
        return ResponseEntity.ok(ApiResponse.success(
                pagoServicioService.pendientes(accesoRecursos.clienteActual(), pagina.getPagina(), pagina.getTamano())));
    }

    /** Pagos rechazados por el proveedor o no realizados (no se hizo ningún cargo). */
    @PostMapping(value = "/rechazadas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PaginaDto<PagoServicioResponseDto>>> rechazadas(
            @Valid @RequestBody(required = false) ConsultaHistorialRequest request) {
        ConsultaHistorialRequest pagina = request != null ? request : new ConsultaHistorialRequest();
        return ResponseEntity.ok(ApiResponse.success(
                pagoServicioService.rechazadas(accesoRecursos.clienteActual(), pagina.getPagina(), pagina.getTamano())));
    }

    /** Consulta a GestoPago el resultado real de un pago pendiente (no vuelve a cobrar). */
    @PostMapping(value = "/verificar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagoServicioResponseDto>> verificarPago(@Valid @RequestBody ConsultaPagoRequest request) {
        return responder(pagoServicioService.verificarPago(accesoRecursos.clienteActual(), request.getFolio()));
    }


    private static ResponseEntity<ApiResponse<PagoServicioResponseDto>> responder(PagoServicioResponseDto pago) {
        EstadoTransaccion estado = pago.getEstado();
        ApiResponseEnum resultado = switch (estado) {
            case EXITOSA -> ApiResponseEnum.PAGO_EXITOSO;
            case PROCESANDO, PENDIENTE_CONCILIACION -> ApiResponseEnum.PAGO_EN_CONCILIACION;
            case RECHAZADA, ERROR -> ApiResponseEnum.PAGO_RECHAZADO;
        };
        return ResponseEntity.status(resultado.getHttpStatus())
                .body(ApiResponse.success(resultado.getCode(), resultado.getMessage(), pago));
    }
}
