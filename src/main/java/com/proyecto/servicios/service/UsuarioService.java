package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.cliente.AgregarUsuarioRequest;
import com.proyecto.servicios.dto.cliente.UsuarioResponseDto;
import com.proyecto.servicios.dto.usuario.UsuarioActualizacionRequest;
import com.proyecto.servicios.dto.usuario.UsuarioBajaRequest;
import com.proyecto.servicios.dto.usuario.UsuarioConsultaIdRequest;
import com.proyecto.servicios.dto.usuario.UsuarioCreacionRequest;
import com.proyecto.servicios.dto.usuario.UsuarioFiltroRequest;

import java.util.List;

public interface UsuarioService {

    UsuarioResponseDto crearUsuario(UsuarioCreacionRequest request);

    List<UsuarioResponseDto> consultarUsuarios(UsuarioFiltroRequest filtro);

    UsuarioResponseDto consultarPorId(UsuarioConsultaIdRequest request);

    UsuarioResponseDto actualizarUsuario(UsuarioActualizacionRequest request);

    UsuarioResponseDto bajaUsuario(UsuarioBajaRequest request);

    List<UsuarioResponseDto> consultarConFiltro(String correo, Boolean activo);

    UsuarioResponseDto agregarUsuario(AgregarUsuarioRequest request);
}
