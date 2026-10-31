package com.proyecto.servicios.controller;

import com.proyecto.servicios.dto.ApiResponse;
import com.proyecto.servicios.dto.cliente.ActualizarCuentaRequest;
import com.proyecto.servicios.dto.cliente.CrearCuentaRequest;
import com.proyecto.servicios.dto.cliente.CuentaBancariaResponseDto;
import com.proyecto.servicios.dto.cliente.CuentaFiltroRequest;
import com.proyecto.servicios.dto.cliente.EstablecerPinRequest;
import com.proyecto.servicios.dto.cliente.NumeroCuentaRequest;
import com.proyecto.servicios.dto.cliente.SaldoCuentaResponseDto;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.enums.EstadoCuenta;
import com.proyecto.servicios.service.CuentaService;
import com.proyecto.servicios.util.JwtTokenService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService cuentaService;
    private final JwtTokenService jwtTokenService;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CuentaBancariaResponseDto>> crearCuenta(@Valid @RequestBody CrearCuentaRequest request) {
        log.info("Petición recibida para crear cuenta a cliente ID: {}", request.getClienteId());
        CuentaBancariaResponseDto response = cuentaService.crearCuenta(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                ApiResponseEnum.CREADO.getCode(),
                ApiResponseEnum.CREADO.getMessage(),
                response
        ));
    }

    @PostMapping(value = "/detalle", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CuentaBancariaResponseDto>> consultarPorNumero(@Valid @RequestBody NumeroCuentaRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cuentaService.consultarPorNumero(String.valueOf(request.getNumeroCuenta()))));
    }

    @PostMapping(value = "/consulta", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CuentaBancariaResponseDto>>> consultarCuentas(
            @RequestBody(required = false) CuentaFiltroRequest request) {
        CuentaFiltroRequest filtro = request != null ? request : new CuentaFiltroRequest();

        if (filtro.getClienteId() != null) {
            return ResponseEntity.ok(ApiResponse.success(cuentaService.consultarPorClienteId(filtro.getClienteId())));
        }
        if (filtro.getEstatus() != null) {
            return ResponseEntity.ok(ApiResponse.success(cuentaService.consultarPorEstatus(filtro.getEstatus())));
        }

        return ResponseEntity.ok(ApiResponse.success(cuentaService.consultarPorEstatus(EstadoCuenta.ACTIVA)));
    }

    @PostMapping(value = "/saldo", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<SaldoCuentaResponseDto>> consultarSaldo(@Valid @RequestBody NumeroCuentaRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cuentaService.consultarSaldo(String.valueOf(request.getNumeroCuenta()))));
    }

    @PatchMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CuentaBancariaResponseDto>> actualizarCuenta(@Valid @RequestBody ActualizarCuentaRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cuentaService.actualizarParcial(String.valueOf(request.getNumeroCuenta()), request)));
    }

    @PutMapping(value = "/pin", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> establecerPin(
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody EstablecerPinRequest request) {
        cuentaService.establecerPin(String.valueOf(request.getNumeroCuenta()), jwtTokenService.obtenerClienteIdDesdeHeader(authorization), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
