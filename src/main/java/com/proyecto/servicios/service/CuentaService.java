package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.cliente.ActualizarCuentaRequest;
import com.proyecto.servicios.dto.cliente.CrearCuentaRequest;
import com.proyecto.servicios.dto.cliente.CuentaBancariaResponseDto;
import com.proyecto.servicios.dto.cliente.SaldoCuentaResponseDto;
import com.proyecto.servicios.enums.EstadoCuenta;

import java.util.List;

public interface CuentaService {

    CuentaBancariaResponseDto crearCuenta(CrearCuentaRequest request);

    CuentaBancariaResponseDto consultarPorNumero(String numeroCuenta);

    List<CuentaBancariaResponseDto> consultarPorClienteId(Long clienteId);

    List<CuentaBancariaResponseDto> consultarPorEstatus(EstadoCuenta estatus);

    SaldoCuentaResponseDto consultarSaldo(String numeroCuenta);

    CuentaBancariaResponseDto actualizarParcial(String numeroCuenta, ActualizarCuentaRequest request);
}

