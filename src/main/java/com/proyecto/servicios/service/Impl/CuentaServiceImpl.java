package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.dto.cliente.ActualizarCuentaRequest;
import com.proyecto.servicios.dto.cliente.CrearCuentaRequest;
import com.proyecto.servicios.dto.cliente.CuentaBancariaResponseDto;
import com.proyecto.servicios.dto.cliente.EstablecerPinRequest;
import com.proyecto.servicios.dto.cliente.SaldoCuentaResponseDto;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.CuentaBancaria;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.enums.EstadoCuenta;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CuentaInactivaException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.PinInvalidoException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.repository.ClienteRepository;
import com.proyecto.servicios.repository.CuentaBancariaRepository;
import com.proyecto.servicios.service.CuentaService;
import com.proyecto.servicios.util.DatosSensiblesUtil;
import com.proyecto.servicios.util.GeneradorCuentaBancaria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CuentaServiceImpl implements CuentaService {

    private final CuentaBancariaRepository cuentaBancariaRepository;
    private final ClienteRepository clienteRepository;
    private final GeneradorCuentaBancaria generadorCuentaBancaria;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public CuentaBancariaResponseDto crearCuenta(CrearCuentaRequest request) {
        Cliente cliente = clienteRepository.findByIdAndEliminadoLogicoFalse(request.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID: " + request.getClienteId()));

        if (!Boolean.TRUE.equals(cliente.getActivoLogin())) {
            throw new ValidacionNegocioException("Solo los clientes activos podrán tener cuentas activas");
        }

        BigDecimal saldoInicial = request.getSaldoInicial() != null ? request.getSaldoInicial() : BigDecimal.ZERO;
        if (saldoInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionNegocioException("El saldo inicial no puede ser negativo");
        }

        String numeroCuenta = generarNumeroCuentaUnico();
        CuentaBancaria cuenta = CuentaBancaria.builder()
                .numeroCuenta(numeroCuenta)
                .saldo(saldoInicial)
                .estado(EstadoCuenta.ACTIVA)
                .cliente(cliente)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        cuenta = cuentaBancariaRepository.save(cuenta);
        log.info("Cuenta creada exitosamente: {} para cliente ID: {}", numeroCuenta, cliente.getId());

        return mapearDto(cuenta);
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaBancariaResponseDto consultarPorNumero(String numeroCuenta) {
        CuentaBancaria cuenta = cuentaBancariaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException("Cuenta no encontrada con número: " + numeroCuenta));
        return mapearDto(cuenta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaBancariaResponseDto> consultarPorClienteId(Long clienteId) {
        return cuentaBancariaRepository.findByClienteId(clienteId)
                .stream()
                .map(this::mapearDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaBancariaResponseDto> consultarPorEstatus(EstadoCuenta estatus) {
        return cuentaBancariaRepository.findByEstado(estatus)
                .stream()
                .map(this::mapearDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SaldoCuentaResponseDto consultarSaldo(String numeroCuenta) {
        CuentaBancaria cuenta = cuentaBancariaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException("Cuenta no encontrada con número: " + numeroCuenta));

        return SaldoCuentaResponseDto.builder()
                .numeroCuenta(cuenta.getNumeroCuenta())
                .saldo(cuenta.getSaldo())
                .build();
    }

    @Override
    @Transactional
    public CuentaBancariaResponseDto actualizarParcial(String numeroCuenta, ActualizarCuentaRequest request) {
        CuentaBancaria cuenta = cuentaBancariaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException("Cuenta no encontrada con número: " + numeroCuenta));

        if (request.getEstatus() != null) {
            if (request.getEstatus() == EstadoCuenta.ACTIVA && !Boolean.TRUE.equals(cuenta.getCliente().getActivoLogin())) {
                throw new ValidacionNegocioException("Solo los clientes activos podrán tener cuentas activas");
            }
            cuenta.setEstado(request.getEstatus());
        }

        if (request.getSaldo() != null) {
            if (request.getSaldo().compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidacionNegocioException("El saldo no puede ser negativo");
            }
            cuenta.setSaldo(request.getSaldo());
        }

        cuenta.setFechaActualizacion(LocalDateTime.now());
        cuenta = cuentaBancariaRepository.save(cuenta);

        log.info("Cuenta {} actualizada parcialmente", numeroCuenta);
        return mapearDto(cuenta);
    }

    @Override
    @Transactional
    public void establecerPin(String numeroCuenta, Long clienteId, EstablecerPinRequest request) {
        CuentaBancaria cuenta = cuentaBancariaRepository.findByNumeroCuentaForUpdate(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException("Cuenta no encontrada con número: " + numeroCuenta));

        if (clienteId == null || cuenta.getCliente() == null || !clienteId.equals(cuenta.getCliente().getId())) {
            throw new BusinessException(ApiResponseEnum.CUENTA_NO_PERTENECE);
        }
        if (cuenta.getEstado() != EstadoCuenta.ACTIVA) {
            throw new CuentaInactivaException("Solo se puede configurar el PIN de una cuenta activa");
        }
        if (cuenta.getPinSeguridad() != null
                && (request.getPinActual() == null || !passwordEncoder.matches(request.getPinActual(), cuenta.getPinSeguridad()))) {
            throw new PinInvalidoException();
        }

        cuenta.setPinSeguridad(passwordEncoder.encode(request.getPinNuevo()));
        cuenta.setIntentosPinFallidos(0);
        cuenta.setFechaActualizacion(LocalDateTime.now());
        log.info("PIN de seguridad actualizado para la cuenta {}", DatosSensiblesUtil.enmascarar(numeroCuenta));
    }

    private String generarNumeroCuentaUnico() {
        String numeroCuenta;
        do {
            numeroCuenta = generadorCuentaBancaria.generarNumeroCuenta();
        } while (cuentaBancariaRepository.existsByNumeroCuenta(numeroCuenta));
        return numeroCuenta;
    }

    private CuentaBancariaResponseDto mapearDto(CuentaBancaria cuenta) {
        return CuentaBancariaResponseDto.builder()
                .numeroCuenta(cuenta.getNumeroCuenta())
                .saldo(cuenta.getSaldo())
                .estado(cuenta.getEstado())
                .fechaCreacion(cuenta.getFechaCreacion())
                .build();
    }
}

