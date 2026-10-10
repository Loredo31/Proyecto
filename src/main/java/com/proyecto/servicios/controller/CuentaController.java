package com.proyecto.servicios.controller;

import com.proyecto.servicios.dto.ApiResponse;
import com.proyecto.servicios.dto.cliente.ActualizarCuentaRequest;
import com.proyecto.servicios.dto.cliente.CrearCuentaRequest;
import com.proyecto.servicios.dto.cliente.CuentaBancariaResponseDto;
import com.proyecto.servicios.dto.cliente.SaldoCuentaResponseDto;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.enums.EstadoCuenta;
import com.proyecto.servicios.service.CuentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService cuentaService;

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

    @GetMapping(value = "/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CuentaBancariaResponseDto>> consultarPorNumero(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(ApiResponse.success(cuentaService.consultarPorNumero(numeroCuenta)));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CuentaBancariaResponseDto>>> consultarCuentas(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) EstadoCuenta estatus) {

        if (clienteId != null) {
            return ResponseEntity.ok(ApiResponse.success(cuentaService.consultarPorClienteId(clienteId)));
        }
        if (estatus != null) {
            return ResponseEntity.ok(ApiResponse.success(cuentaService.consultarPorEstatus(estatus)));
        }

        return ResponseEntity.ok(ApiResponse.success(cuentaService.consultarPorEstatus(EstadoCuenta.ACTIVA)));
    }

    @GetMapping(value = "/{numeroCuenta}/saldo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<SaldoCuentaResponseDto>> consultarSaldo(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(ApiResponse.success(cuentaService.consultarSaldo(numeroCuenta)));
    }

    @PatchMapping(value = "/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CuentaBancariaResponseDto>> actualizarCuenta(
            @PathVariable String numeroCuenta,
            @Valid @RequestBody ActualizarCuentaRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cuentaService.actualizarParcial(numeroCuenta, request)));
    }
}

