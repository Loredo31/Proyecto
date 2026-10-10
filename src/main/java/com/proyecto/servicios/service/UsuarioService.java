package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.cliente.AgregarUsuarioRequest;
import com.proyecto.servicios.dto.cliente.UsuarioResponseDto;

import java.util.List;

public interface UsuarioService {

    List<UsuarioResponseDto> consultarConFiltro(String correo, Boolean activo);

    UsuarioResponseDto agregarUsuario(AgregarUsuarioRequest request);
}

