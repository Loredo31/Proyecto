package com.proyecto.servicios.controller;

import com.proyecto.servicios.dto.ApiResponse;
import com.proyecto.servicios.dto.pago.ConsultaPagoRequest;
import com.proyecto.servicios.dto.pago.PagoServicioRequest;
import com.proyecto.servicios.dto.pago.PagoServicioResponseDto;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.service.PagoServicioService;
import com.proyecto.servicios.util.DatosSensiblesUtil;
import com.proyecto.servicios.util.JwtTokenService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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
    private final JwtTokenService jwtTokenService;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagoServicioResponseDto>> procesarPago(
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody PagoServicioRequest request) {

        Long clienteId = clienteAutenticado(authorization);
        log.info("Petición de pago: cliente={}, cuenta={}, idServicio={}, idProducto={}",
                clienteId, DatosSensiblesUtil.enmascarar(String.valueOf(request.getNumCuenta())),
                request.getIdServicio(), request.getIdProducto());

        return responder(pagoServicioService.procesarPago(clienteId, request.getIdempotencyKey(), request));
    }

    @PostMapping(value = "/detalle", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagoServicioResponseDto>> consultarPago(
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody ConsultaPagoRequest request) {
        return responder(pagoServicioService.consultarPago(clienteAutenticado(authorization), request.getFolio()));
    }

    private Long clienteAutenticado(String authorization) {
        Long clienteId = jwtTokenService.obtenerClienteIdDesdeHeader(authorization);
        if (clienteId == null) {
            throw new BusinessException(ApiResponseEnum.ACCESO_DENEGADO, "El token no está asociado a un cliente");
        }
        return clienteId;
    }

    private static ResponseEntity<ApiResponse<PagoServicioResponseDto>> responder(PagoServicioResponseDto pago) {
        ApiResponseEnum resultado = switch (pago.getEstado()) {
            case EXITOSA -> ApiResponseEnum.PAGO_EXITOSO;
            case PROCESANDO, PENDIENTE_CONCILIACION -> ApiResponseEnum.PAGO_EN_CONCILIACION;
            case RECHAZADA, ERROR -> ApiResponseEnum.PAGO_RECHAZADO;
        };
        return ResponseEntity.status(resultado.getHttpStatus())
                .body(ApiResponse.success(resultado.getCode(), resultado.getMessage(), pago));
    }
}
