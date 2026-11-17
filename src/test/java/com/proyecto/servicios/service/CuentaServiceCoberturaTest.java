package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.cliente.ActualizarCuentaRequest;
import com.proyecto.servicios.dto.cliente.CrearCuentaRequest;
import com.proyecto.servicios.dto.cliente.EstablecerPinRequest;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.CuentaBancaria;
import com.proyecto.servicios.enums.EstadoCuenta;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.exception.CuentaInactivaException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.PinInvalidoException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.repository.ClienteRepository;
import com.proyecto.servicios.repository.CuentaBancariaRepository;
import com.proyecto.servicios.service.Impl.CuentaServiceImpl;
import com.proyecto.servicios.util.GeneradorCuentaBancaria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaServiceCoberturaTest {

    private static final String NUM = "4152319999888877";

    @Mock
    private CuentaBancariaRepository cuentaBancariaRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private GeneradorCuentaBancaria generadorCuentaBancaria;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CuentaServiceImpl service;

    private Cliente cliente;
    private CuentaBancaria cuenta;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder().id(1L).activoLogin(true).eliminadoLogico(false).build();
        cuenta = CuentaBancaria.builder().id(10L).numeroCuenta(NUM).saldo(new BigDecimal("100"))
                .estado(EstadoCuenta.ACTIVA).cliente(cliente).build();
    }

    private void cuentaParaPin() {
        when(cuentaBancariaRepository.findByNumeroCuentaForUpdate(NUM)).thenReturn(Optional.of(cuenta));
    }

    private EstablecerPinRequest pin(String actual, String nuevo) {
        return EstablecerPinRequest.builder().numeroCuenta(Long.parseLong(NUM)).pinActual(actual).pinNuevo(nuevo).build();
    }

    @Test
    void crearCuenta_SaldoNegativo() {
        when(clienteRepository.findByIdAndEliminadoLogicoFalse(1L)).thenReturn(Optional.of(cliente));
        CrearCuentaRequest req = CrearCuentaRequest.builder().clienteId(1L).saldoInicial(new BigDecimal("-1")).build();

        assertThrows(ValidacionNegocioException.class, () -> service.crearCuenta(req));
    }

    @Test
    void consultas_PorClienteYEstatus() {
        when(cuentaBancariaRepository.findByClienteId(1L)).thenReturn(List.of(cuenta));
        when(cuentaBancariaRepository.findByEstado(EstadoCuenta.ACTIVA)).thenReturn(List.of(cuenta, cuenta));

        assertEquals(1, service.consultarPorClienteId(1L).size());
        assertEquals(2, service.consultarPorEstatus(EstadoCuenta.ACTIVA).size());
    }

    @Test
    void actualizarParcial_ActivarConClienteInactivo() {
        cliente.setActivoLogin(false);
        when(cuentaBancariaRepository.findByNumeroCuenta(NUM)).thenReturn(Optional.of(cuenta));
        ActualizarCuentaRequest req = ActualizarCuentaRequest.builder().estatus(EstadoCuenta.ACTIVA).build();

        assertThrows(ValidacionNegocioException.class, () -> service.actualizarParcial(NUM, req));
    }

    @Test
    void actualizarParcial_SaldoNegativo() {
        when(cuentaBancariaRepository.findByNumeroCuenta(NUM)).thenReturn(Optional.of(cuenta));
        ActualizarCuentaRequest req = ActualizarCuentaRequest.builder().saldo(new BigDecimal("-5")).build();

        assertThrows(ValidacionNegocioException.class, () -> service.actualizarParcial(NUM, req));
    }

    @Test
    void establecerPin_Primero() {
        cuentaParaPin();
        when(passwordEncoder.encode("1234")).thenReturn("hash");

        service.establecerPin(NUM, 1L, pin(null, "1234"));

        assertEquals("hash", cuenta.getPinSeguridad());
        assertEquals(0, cuenta.getIntentosPinFallidos());
    }

    @Test
    void establecerPin_CambioConPinActualCorrecto() {
        cuenta.setPinSeguridad("viejo");
        cuentaParaPin();
        when(passwordEncoder.matches("1111", "viejo")).thenReturn(true);
        when(passwordEncoder.encode("2222")).thenReturn("nuevo");

        service.establecerPin(NUM, 1L, pin("1111", "2222"));

        assertEquals("nuevo", cuenta.getPinSeguridad());
    }

    @Test
    void establecerPin_PinActualIncorrectoOFaltante() {
        cuenta.setPinSeguridad("viejo");
        cuentaParaPin();
        when(passwordEncoder.matches("9999", "viejo")).thenReturn(false);
        EstablecerPinRequest incorrecto = pin("9999", "2222");
        EstablecerPinRequest faltante = pin(null, "2222");

        assertThrows(PinInvalidoException.class, () -> service.establecerPin(NUM, 1L, incorrecto));
        assertThrows(PinInvalidoException.class, () -> service.establecerPin(NUM, 1L, faltante));
    }

    @Test
    void establecerPin_CuentaAjena() {
        cuentaParaPin();
        EstablecerPinRequest req = pin(null, "1234");

        assertThrows(BusinessException.class, () -> service.establecerPin(NUM, 2L, req));
        assertThrows(BusinessException.class, () -> service.establecerPin(NUM, null, req));
    }

    @Test
    void establecerPin_CuentaInactiva() {
        cuenta.setEstado(EstadoCuenta.BLOQUEADA);
        cuentaParaPin();
        EstablecerPinRequest req = pin(null, "1234");

        assertThrows(CuentaInactivaException.class, () -> service.establecerPin(NUM, 1L, req));
    }

    @Test
    void establecerPin_CuentaInexistente() {
        when(cuentaBancariaRepository.findByNumeroCuentaForUpdate(NUM)).thenReturn(Optional.empty());
        EstablecerPinRequest req = pin(null, "1234");

        assertThrows(CuentaNoEncontradaException.class, () -> service.establecerPin(NUM, 1L, req));
    }
}
