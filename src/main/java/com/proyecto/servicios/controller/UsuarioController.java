package com.proyecto.servicios.controller;

import com.proyecto.servicios.dto.cliente.AgregarUsuarioRequest;
import com.proyecto.servicios.dto.cliente.UsuarioResponseDto;
import com.proyecto.servicios.dto.usuario.UsuarioActualizacionRequest;
import com.proyecto.servicios.dto.usuario.UsuarioBajaRequest;
import com.proyecto.servicios.dto.usuario.UsuarioConsultaIdRequest;
import com.proyecto.servicios.dto.usuario.UsuarioCreacionRequest;
import com.proyecto.servicios.dto.usuario.UsuarioFiltroRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping(
            value = "/crear",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<GenericResponse<UsuarioResponseDto>> crearUsuario(
            @Valid @RequestBody UsuarioCreacionRequest request
    ) {
        log.info("Petición recibida para crear usuario con correo: {}", request.getCorreo());
        UsuarioResponseDto usuario = usuarioService.crearUsuario(request);
        return new ResponseEntity<>(
                GenericResponse.exito("Usuario creado exitosamente", usuario),
                HttpStatus.CREATED
        );
    }

    @PostMapping(
            value = "/consulta",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<GenericResponse<List<UsuarioResponseDto>>> consultarUsuarios(
            @Valid @RequestBody(required = false) UsuarioFiltroRequest request
    ) {
        log.info("Petición recibida para consultar usuarios con criterios dinámicos");
        List<UsuarioResponseDto> usuarios = usuarioService.consultarUsuarios(request);
        return new ResponseEntity<>(
                GenericResponse.exito("Consulta de usuarios ejecutada exitosamente", usuarios),
                HttpStatus.OK
        );
    }

    @PostMapping(
            value = "/detalle",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<GenericResponse<UsuarioResponseDto>> consultarPorId(
            @Valid @RequestBody UsuarioConsultaIdRequest request
    ) {
        log.info("Petición recibida para consultar detalle de usuario ID: {}", request.getId());
        UsuarioResponseDto usuario = usuarioService.consultarPorId(request);
        return new ResponseEntity<>(
                GenericResponse.exito("Usuario encontrado", usuario),
                HttpStatus.OK
        );
    }

    @PutMapping(
            value = "/actualizar",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<GenericResponse<UsuarioResponseDto>> actualizarUsuario(
            @Valid @RequestBody UsuarioActualizacionRequest request
    ) {
        log.info("Petición recibida para actualizar usuario ID: {}", request.getId());
        UsuarioResponseDto usuario = usuarioService.actualizarUsuario(request);
        return new ResponseEntity<>(
                GenericResponse.exito("Usuario actualizado exitosamente", usuario),
                HttpStatus.OK
        );
    }

    @PutMapping(
            value = "/baja",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<GenericResponse<UsuarioResponseDto>> bajaUsuario(
            @Valid @RequestBody UsuarioBajaRequest request
    ) {
        log.info("Petición recibida para dar de baja al usuario ID: {}", request.getId());
        UsuarioResponseDto usuario = usuarioService.bajaUsuario(request);
        return new ResponseEntity<>(
                GenericResponse.exito("Usuario dado de baja exitosamente", usuario),
                HttpStatus.OK
        );
    }

    @PutMapping(
            value = "/agregar",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<GenericResponse<UsuarioResponseDto>> agregarUsuario(
            @Valid @RequestBody AgregarUsuarioRequest request
    ) {
        log.info("Petición compatible para agregar usuario a cliente ID: {}", request.getClienteId());
        UsuarioResponseDto usuario = usuarioService.agregarUsuario(request);
        return new ResponseEntity<>(
                GenericResponse.exito("Usuario agregado exitosamente", usuario),
                HttpStatus.OK
        );
    }
}
