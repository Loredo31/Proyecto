package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.GestoPagoTxProperties;
import com.proyecto.servicios.entity.cliente.CuentaBancaria;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.enums.EstadoCuenta;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.exception.CuentaInactivaException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.PinInvalidoException;
import com.proyecto.servicios.repository.CuentaBancariaRepository;
import com.proyecto.servicios.service.PinSeguridadService;
import com.proyecto.servicios.util.DatosSensiblesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;

@Slf4j
@Service
@RequiredArgsConstructor
public class PinSeguridadServiceImpl implements PinSeguridadService {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();
    private static final String CUENTA_NO_ENCONTRADA = "Cuenta no encontrada";

    private final CuentaBancariaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;
    private final GestoPagoTxProperties properties;

    @Override
    @Transactional(noRollbackFor = PinInvalidoException.class)
    public void validar(Long clienteId, String pin, String numeroCuenta) {
        PinSeguridadService.exigirFormato(pin);
        CuentaBancaria cuenta = obtenerCuenta(clienteId, numeroCuenta);
        if (cuenta.getPinSeguridad() == null) {
            throw new BusinessException(ApiResponseEnum.PIN_NO_CONFIGURADO);
        }
        verificar(cuenta, pin);
    }

    @Override
    @Transactional(noRollbackFor = PinInvalidoException.class)
    public void validarSiConfigurado(Long clienteId, String pin, String numeroCuenta) {
        CuentaBancaria cuenta = obtenerCuenta(clienteId, numeroCuenta);
        if (cuenta.getPinSeguridad() != null) {
            PinSeguridadService.exigirFormato(pin);
            verificar(cuenta, pin);
        }
    }

    private CuentaBancaria obtenerCuenta(Long clienteId, String numeroCuenta) {
        CuentaBancaria cuenta;
        if (numeroCuenta != null) {
            cuenta = cuentaRepository.findByNumeroCuentaForUpdate(numeroCuenta)
                    .orElseThrow(() -> new CuentaNoEncontradaException(CUENTA_NO_ENCONTRADA));
            if (cuenta.getCliente() == null || !cuenta.getCliente().getId().equals(clienteId)) {
                throw new CuentaNoEncontradaException(CUENTA_NO_ENCONTRADA);
            }
        } else {
            Long cuentaId = cuentaRepository.findByClienteId(clienteId).stream()
                    .filter(c -> c.getEstado() == EstadoCuenta.ACTIVA && c.getPinSeguridad() != null)
                    .min(Comparator.comparing(CuentaBancaria::getId))
                    .map(CuentaBancaria::getId)
                    .orElseThrow(() -> new BusinessException(ApiResponseEnum.PIN_NO_CONFIGURADO,
                            "Configure el PIN de seguridad de su cuenta antes de realizar esta operación"));
            cuenta = cuentaRepository.findByIdForUpdate(cuentaId)
                    .orElseThrow(() -> new CuentaNoEncontradaException(CUENTA_NO_ENCONTRADA));
        }
        if (cuenta.getEstado() != EstadoCuenta.ACTIVA) {
            throw new CuentaInactivaException("La cuenta se encuentra " + cuenta.getEstado().name().toLowerCase());
        }
        return cuenta;
    }

    private void verificar(CuentaBancaria cuenta, String pin) {
        if (passwordEncoder.matches(pin, cuenta.getPinSeguridad())) {
            if (cuenta.getIntentosPinFallidos() > 0) {
                cuenta.setIntentosPinFallidos(0);
            }
            return;
        }
        int intentos = cuenta.getIntentosPinFallidos() + 1;
        cuenta.setIntentosPinFallidos(intentos);
        cuenta.setFechaActualizacion(LocalDateTime.now(ZONE_ID));
        if (intentos >= properties.getMaxIntentosPin()) {
            cuenta.setEstado(EstadoCuenta.BLOQUEADA);
            log.warn("Cuenta {} bloqueada por {} intentos de PIN fallidos",
                    DatosSensiblesUtil.enmascarar(cuenta.getNumeroCuenta()), intentos);
        }
        cuentaRepository.save(cuenta);
        throw new PinInvalidoException();
    }

}
