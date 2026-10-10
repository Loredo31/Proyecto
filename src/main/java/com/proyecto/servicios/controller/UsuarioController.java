package com.proyecto.servicios.controller;

import com.proyecto.servicios.dto.ApiResponse;
import com.proyecto.servicios.dto.cliente.AgregarUsuarioRequest;
import com.proyecto.servicios.dto.cliente.UsuarioResponseDto;
import com.proyecto.servicios.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping(value = "/filtro", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<UsuarioResponseDto>>> consultarFiltro(
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) Boolean activo) {
        log.info("Consultando usuarios con filtro correo: {}, activo: {}", correo, activo);
        List<UsuarioResponseDto> lista = usuarioService.consultarConFiltro(correo, activo);
        return ResponseEntity.ok(ApiResponse.success(lista));
    }

    @PutMapping(value = "/agregar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UsuarioResponseDto>> agregarUsuario(@Valid @RequestBody AgregarUsuarioRequest request) {
        log.info("Petición para agregar usuario a cliente ID: {}", request.getClienteId());
        UsuarioResponseDto response = usuarioService.agregarUsuario(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

