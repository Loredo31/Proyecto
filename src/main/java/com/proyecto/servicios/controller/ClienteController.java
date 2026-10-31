package com.proyecto.servicios.controller;

import com.proyecto.servicios.dto.ApiResponse;
import com.proyecto.servicios.dto.cliente.ClienteActualizacionRequest;
import com.proyecto.servicios.dto.cliente.ClienteFiltroRequest;
import com.proyecto.servicios.dto.cliente.ClienteIdRequest;
import com.proyecto.servicios.dto.cliente.ClientePatchRequest;
import com.proyecto.servicios.dto.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.dto.cliente.ClienteResponseDto;
import com.proyecto.servicios.dto.cliente.CurpRequest;
import com.proyecto.servicios.dto.cliente.NumeroCuentaRequest;
import com.proyecto.servicios.dto.cliente.RfcRequest;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping(value = "/consulta", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> consultarClientes(@RequestBody(required = false) ClienteFiltroRequest request) {
        ClienteFiltroRequest filtro = request != null ? request : new ClienteFiltroRequest();

        if (tieneValor(filtro.getCurp())) {
            return ResponseEntity.ok(ApiResponse.success(clienteService.consultarPorCurp(filtro.getCurp())));
        }
        if (tieneValor(filtro.getRfc())) {
            return ResponseEntity.ok(ApiResponse.success(clienteService.consultarPorRfc(filtro.getRfc())));
        }
        if (tieneValor(filtro.getCorreo())) {
            return ResponseEntity.ok(ApiResponse.success(clienteService.consultarPorCorreo(filtro.getCorreo())));
        }
        if (tieneValor(filtro.getNombre())) {
            return ResponseEntity.ok(ApiResponse.success(clienteService.buscarPorNombre(filtro.getNombre())));
        }
        if (tieneValor(filtro.getApellidoPaterno())) {
            return ResponseEntity.ok(ApiResponse.success(clienteService.buscarPorApellidoPaterno(filtro.getApellidoPaterno())));
        }
        if (tieneValor(filtro.getApellidoMaterno())) {
            return ResponseEntity.ok(ApiResponse.success(clienteService.buscarPorApellidoMaterno(filtro.getApellidoMaterno())));
        }
        if (Boolean.TRUE.equals(filtro.getActivos())) {
            return ResponseEntity.ok(ApiResponse.success(clienteService.consultarActivos()));
        }
        if (filtro.getFechaInicio() != null && filtro.getFechaFin() != null) {
            return ResponseEntity.ok(ApiResponse.success(
                    clienteService.consultarPorRangoFechas(filtro.getFechaInicio(), filtro.getFechaFin())));
        }

        return ResponseEntity.ok(ApiResponse.success(clienteService.consultarTodos()));
    }

    @PostMapping(value = "/detalle", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponseDto>> consultarPorId(@Valid @RequestBody ClienteIdRequest request) {
        return ResponseEntity.ok(ApiResponse.success(clienteService.consultarPorId(request.getId())));
    }

    @PostMapping(value = "/curp", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponseDto>> consultarPorCurpDirecto(@Valid @RequestBody CurpRequest request) {
        return ResponseEntity.ok(ApiResponse.success(clienteService.consultarPorCurp(request.getCurp())));
    }

    @PostMapping(value = "/rfc", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponseDto>> consultarPorRfcDirecto(@Valid @RequestBody RfcRequest request) {
        return ResponseEntity.ok(ApiResponse.success(clienteService.consultarPorRfc(request.getRfc())));
    }

    @PostMapping(value = "/cuenta", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponseDto>> consultarPorNumeroCuenta(@Valid @RequestBody NumeroCuentaRequest request) {
        return ResponseEntity.ok(ApiResponse.success(clienteService.consultarPorNumeroCuenta(String.valueOf(request.getNumeroCuenta()))));
    }

    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponseDto>> actualizarCliente(@Valid @RequestBody ClienteActualizacionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(clienteService.actualizarCliente(request.getId(), request)));
    }

    @PatchMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponseDto>> patchCliente(@Valid @RequestBody ClientePatchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(clienteService.patchCliente(request.getId(), request)));
    }

    @PutMapping(value = "/baja", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> bajaLogica(@Valid @RequestBody ClienteIdRequest request) {
        clienteService.bajaLogica(request.getId());
        return ResponseEntity.ok(ApiResponse.success("Cliente dado de baja lógica correctamente"));
    }

    private static boolean tieneValor(String valor) {
        return valor != null && !valor.isBlank();
    }
}
