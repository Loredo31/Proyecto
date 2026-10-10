package com.proyecto.servicios.controller;

import com.proyecto.servicios.dto.ApiResponse;
import com.proyecto.servicios.dto.cliente.ClienteActualizacionRequest;
import com.proyecto.servicios.dto.cliente.ClientePatchRequest;
import com.proyecto.servicios.dto.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.dto.cliente.ClienteResponseDto;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping(value = "/onboarding", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponseDto>> registrarCliente(@Valid @RequestBody ClienteRegistroRequest request) {
        log.info("Registrando cliente con CURP: {}", request.getDatosPersonales().getCurp());
        ClienteResponseDto responseDto = clienteService.registrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                ApiResponseEnum.CREADO.getCode(),
                ApiResponseEnum.CREADO.getMessage(),
                responseDto
        ));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> consultarClientes(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String apellidoPaterno,
            @RequestParam(required = false) String apellidoMaterno,
            @RequestParam(required = false) String curp,
            @RequestParam(required = false) String rfc,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) Boolean activos,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {

        if (curp != null && !curp.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(clienteService.consultarPorCurp(curp)));
        }
        if (rfc != null && !rfc.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(clienteService.consultarPorRfc(rfc)));
        }
        if (correo != null && !correo.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(clienteService.consultarPorCorreo(correo)));
        }
        if (nombre != null && !nombre.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(clienteService.buscarPorNombre(nombre)));
        }
        if (apellidoPaterno != null && !apellidoPaterno.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(clienteService.buscarPorApellidoPaterno(apellidoPaterno)));
        }
        if (apellidoMaterno != null && !apellidoMaterno.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(clienteService.buscarPorApellidoMaterno(apellidoMaterno)));
        }
        if (Boolean.TRUE.equals(activos)) {
            return ResponseEntity.ok(ApiResponse.success(clienteService.consultarActivos()));
        }
        if (fechaInicio != null && fechaFin != null) {
            return ResponseEntity.ok(ApiResponse.success(clienteService.consultarPorRangoFechas(fechaInicio, fechaFin)));
        }

        return ResponseEntity.ok(ApiResponse.success(clienteService.consultarTodos()));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponseDto>> consultarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(clienteService.consultarPorId(id)));
    }

    @GetMapping(value = "/curp/{curp}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponseDto>> consultarPorCurpDirecto(@PathVariable String curp) {
        return ResponseEntity.ok(ApiResponse.success(clienteService.consultarPorCurp(curp)));
    }

    @GetMapping(value = "/rfc/{rfc}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponseDto>> consultarPorRfcDirecto(@PathVariable String rfc) {
        return ResponseEntity.ok(ApiResponse.success(clienteService.consultarPorRfc(rfc)));
    }

    @GetMapping(value = "/cuenta/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponseDto>> consultarPorNumeroCuenta(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(ApiResponse.success(clienteService.consultarPorNumeroCuenta(numeroCuenta)));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponseDto>> actualizarCliente(
            @PathVariable Long id,
            @Valid @RequestBody ClienteActualizacionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(clienteService.actualizarCliente(id, request)));
    }

    @PatchMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponseDto>> patchCliente(
            @PathVariable Long id,
            @RequestBody ClientePatchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(clienteService.patchCliente(id, request)));
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> bajaLogica(@PathVariable Long id) {
        clienteService.bajaLogica(id);
        return ResponseEntity.ok(ApiResponse.success("Cliente dado de baja lógica correctamente"));
    }
}

