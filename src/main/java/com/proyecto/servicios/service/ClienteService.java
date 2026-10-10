package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.cliente.ClienteActualizacionRequest;
import com.proyecto.servicios.dto.cliente.ClientePatchRequest;
import com.proyecto.servicios.dto.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.dto.cliente.ClienteResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface ClienteService {

    ClienteResponseDto registrarCliente(ClienteRegistroRequest request);

    ClienteResponseDto actualizarCliente(Long id, ClienteActualizacionRequest request);

    ClienteResponseDto patchCliente(Long id, ClientePatchRequest request);

    void bajaLogica(Long id);

    List<ClienteResponseDto> consultarTodos();

    ClienteResponseDto consultarPorId(Long id);

    ClienteResponseDto consultarPorCurp(String curp);

    ClienteResponseDto consultarPorRfc(String rfc);

    ClienteResponseDto consultarPorCorreo(String correo);

    ClienteResponseDto consultarPorNumeroCuenta(String numeroCuenta);

    List<ClienteResponseDto> buscarPorNombre(String nombre);

    List<ClienteResponseDto> buscarPorApellidoPaterno(String apellidoPaterno);

    List<ClienteResponseDto> buscarPorApellidoMaterno(String apellidoMaterno);

    List<ClienteResponseDto> consultarActivos();

    List<ClienteResponseDto> consultarPorRangoFechas(LocalDate inicio, LocalDate fin);
}

