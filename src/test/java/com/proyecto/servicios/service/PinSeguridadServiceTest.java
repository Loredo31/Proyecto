package com.proyecto.servicios.service;

import com.proyecto.servicios.config.GestoPagoTxProperties;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.CuentaBancaria;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.enums.EstadoCuenta;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.exception.CuentaInactivaException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.PinInvalidoException;
import com.proyecto.servicios.repository.CuentaBancariaRepository;
import com.proyecto.servicios.service.Impl.PinSeguridadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PinSeguridadServiceTest {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);
    private static final String HASH_1234 = ENCODER.encode("1234");
    private static final String NUM = "4152319999888877";

    @Mock
    private CuentaBancariaRepository cuentaRepository;

    private PinSeguridadServiceImpl service;
    private CuentaBancaria cuenta;

    @BeforeEach
    void setUp() {
        service = new PinSeguridadServiceImpl(cuentaRepository, ENCODER, new GestoPagoTxProperties());
        cuenta = CuentaBancaria.builder().id(10L).numeroCuenta(NUM).estado(EstadoCuenta.ACTIVA)
                .pinSeguridad(HASH_1234).cliente(Cliente.builder().id(1L).build()).build();
    }

    private void cuentaPorNumero() {
        when(cuentaRepository.findByNumeroCuentaForUpdate(NUM)).thenReturn(Optional.of(cuenta));
    }

    private ApiResponseEnum codigo(Runnable r) {
        return assertThrows(BusinessException.class, r::run).getResponseStatus();
    }

    @Test
    void validar_PinCorrectoReiniciaIntentos() {
        cuenta.setIntentosPinFallidos(2);
        cuentaPorNumero();

        assertDoesNotThrow(() -> service.validar(1L, "1234", NUM));
        assertEquals(0, cuenta.getIntentosPinFallidos());
    }

    @Test
    void validar_PinIncorrectoCuentaIntentosYBloquea() {
        cuentaPorNumero();

        assertThrows(PinInvalidoException.class, () -> service.validar(1L, "9999", NUM));
        assertEquals(1, cuenta.getIntentosPinFallidos());
        assertThrows(PinInvalidoException.class, () -> service.validar(1L, "9999", NUM));
        assertThrows(PinInvalidoException.class, () -> service.validar(1L, "9999", NUM));
        assertEquals(EstadoCuenta.BLOQUEADA, cuenta.getEstado());
        verify(cuentaRepository, org.mockito.Mockito.times(3)).save(cuenta);
    }

    @Test
    void validar_FormatoYPresenciaDelPin() {
        assertEquals(ApiResponseEnum.PIN_REQUERIDO, codigo(() -> service.validar(1L, null, NUM)));
        assertEquals(ApiResponseEnum.PIN_REQUERIDO, codigo(() -> service.validar(1L, " ", NUM)));
        assertEquals(ApiResponseEnum.DATOS_INVALIDOS, codigo(() -> service.validar(1L, "12", NUM)));
        assertEquals(ApiResponseEnum.DATOS_INVALIDOS, codigo(() -> service.validar(1L, "1234567", NUM)));
        assertEquals(ApiResponseEnum.DATOS_INVALIDOS, codigo(() -> service.validar(1L, "12a4", NUM)));
        verify(cuentaRepository, never()).findByNumeroCuentaForUpdate(any());
    }

    @Test
    void validar_CuentaAjenaInexistenteOInactiva() {
        when(cuentaRepository.findByNumeroCuentaForUpdate("1")).thenReturn(Optional.empty());
        assertThrows(CuentaNoEncontradaException.class, () -> service.validar(1L, "1234", "1"));

        cuentaPorNumero();
        assertThrows(CuentaNoEncontradaException.class, () -> service.validar(2L, "1234", NUM));

        cuenta.setEstado(EstadoCuenta.BLOQUEADA);
        assertThrows(CuentaInactivaException.class, () -> service.validar(1L, "1234", NUM));
    }

    @Test
    void validar_SinPinConfigurado() {
        cuenta.setPinSeguridad(null);
        cuentaPorNumero();

        assertEquals(ApiResponseEnum.PIN_NO_CONFIGURADO, codigo(() -> service.validar(1L, "1234", NUM)));
    }

    @Test
    void validar_SinNumeroUsaLaCuentaPrincipalConPin() {
        CuentaBancaria sinPin = CuentaBancaria.builder().id(5L).estado(EstadoCuenta.ACTIVA).build();
        CuentaBancaria bloqueada = CuentaBancaria.builder().id(6L).estado(EstadoCuenta.BLOQUEADA).pinSeguridad(HASH_1234).build();
        when(cuentaRepository.findByClienteId(1L)).thenReturn(List.of(sinPin, bloqueada, cuenta));
        when(cuentaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(cuenta));

        assertDoesNotThrow(() -> service.validar(1L, "1234", null));
    }

    @Test
    void validar_SinCuentaConPinConfigurado() {
        when(cuentaRepository.findByClienteId(1L)).thenReturn(List.of());

        assertEquals(ApiResponseEnum.PIN_NO_CONFIGURADO, codigo(() -> service.validar(1L, "1234", null)));
    }

    @Test
    void validarSiConfigurado_OmiteSiNoHayPin() {
        cuenta.setPinSeguridad(null);
        cuentaPorNumero();

        assertDoesNotThrow(() -> service.validarSiConfigurado(1L, null, NUM));
    }

    @Test
    void validarSiConfigurado_ExigeElPinActual() {
        cuentaPorNumero();

        assertEquals(ApiResponseEnum.PIN_REQUERIDO, codigo(() -> service.validarSiConfigurado(1L, null, NUM)));
        assertThrows(PinInvalidoException.class, () -> service.validarSiConfigurado(1L, "4321", NUM));
        assertDoesNotThrow(() -> service.validarSiConfigurado(1L, "1234", NUM));
    }
}
