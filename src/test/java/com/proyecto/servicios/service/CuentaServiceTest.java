package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.cliente.ActualizarCuentaRequest;
import com.proyecto.servicios.dto.cliente.CrearCuentaRequest;
import com.proyecto.servicios.dto.cliente.CuentaBancariaResponseDto;
import com.proyecto.servicios.dto.cliente.SaldoCuentaResponseDto;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.CuentaBancaria;
import com.proyecto.servicios.enums.EstadoCuenta;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

    @Mock
    private CuentaBancariaRepository cuentaBancariaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private GeneradorCuentaBancaria generadorCuentaBancaria;

    @InjectMocks
    private CuentaServiceImpl cuentaService;

    private Cliente clienteMock;
    private CuentaBancaria cuentaMock;

    @BeforeEach
    void setUp() {
        clienteMock = Cliente.builder()
                .id(1L)
                .nombre("Juan")
                .activoLogin(true)
                .eliminadoLogico(false)
                .build();

        cuentaMock = CuentaBancaria.builder()
                .id(10L)
                .numeroCuenta("4152319999888877")
                .saldo(new BigDecimal("1500.00"))
                .estado(EstadoCuenta.ACTIVA)
                .cliente(clienteMock)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
    }

    @Test
    void testCrearCuenta_Exito() {
        when(clienteRepository.findByIdAndEliminadoLogicoFalse(1L)).thenReturn(Optional.of(clienteMock));
        when(generadorCuentaBancaria.generarNumeroCuenta()).thenReturn("4152319999888877");
        when(cuentaBancariaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(cuentaBancariaRepository.save(any(CuentaBancaria.class))).thenAnswer(i -> i.getArgument(0));

        CrearCuentaRequest request = CrearCuentaRequest.builder()
                .clienteId(1L)
                .saldoInicial(new BigDecimal("500.00"))
                .build();

        CuentaBancariaResponseDto response = cuentaService.crearCuenta(request);

        assertNotNull(response);
        assertEquals("4152319999888877", response.getNumeroCuenta());
        assertEquals(new BigDecimal("500.00"), response.getSaldo());
        assertEquals(EstadoCuenta.ACTIVA, response.getEstado());
        verify(cuentaBancariaRepository).save(any(CuentaBancaria.class));
    }

    @Test
    void testCrearCuenta_ClienteInactivo_LanzaExcepcion() {
        clienteMock.setActivoLogin(false);
        when(clienteRepository.findByIdAndEliminadoLogicoFalse(1L)).thenReturn(Optional.of(clienteMock));

        CrearCuentaRequest request = CrearCuentaRequest.builder().clienteId(1L).build();

        assertThrows(ValidacionNegocioException.class, () -> cuentaService.crearCuenta(request));
    }

    @Test
    void testConsultarSaldo_Exito() {
        when(cuentaBancariaRepository.findByNumeroCuenta("4152319999888877")).thenReturn(Optional.of(cuentaMock));

        SaldoCuentaResponseDto response = cuentaService.consultarSaldo("4152319999888877");

        assertNotNull(response);
        assertEquals("4152319999888877", response.getNumeroCuenta());
        assertEquals(new BigDecimal("1500.00"), response.getSaldo());
    }

    @Test
    void testConsultarPorNumero_NoEncontrada() {
        when(cuentaBancariaRepository.findByNumeroCuenta("0000000000000000")).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class, () -> cuentaService.consultarPorNumero("0000000000000000"));
    }

    @Test
    void testActualizarParcial_Exito() {
        when(cuentaBancariaRepository.findByNumeroCuenta("4152319999888877")).thenReturn(Optional.of(cuentaMock));
        when(cuentaBancariaRepository.save(any(CuentaBancaria.class))).thenAnswer(i -> i.getArgument(0));

        ActualizarCuentaRequest request = ActualizarCuentaRequest.builder()
                .estatus(EstadoCuenta.BLOQUEADA)
                .saldo(new BigDecimal("2000.00"))
                .build();

        CuentaBancariaResponseDto response = cuentaService.actualizarParcial("4152319999888877", request);

        assertNotNull(response);
        assertEquals(EstadoCuenta.BLOQUEADA, response.getEstado());
        assertEquals(new BigDecimal("2000.00"), response.getSaldo());
    }
}

