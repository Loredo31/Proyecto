package com.proyecto.servicios.service;

import com.proyecto.servicios.config.GestoPagoTxProperties;
import com.proyecto.servicios.dto.pago.PagoServicioRequest;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.CuentaBancaria;
import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.enums.EstadoCuenta;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.exception.PinInvalidoException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.exception.TransaccionDuplicadaException;
import com.proyecto.servicios.model.gestopago.DatosEnvioTx;
import com.proyecto.servicios.model.gestopago.ResultadoSendTx;
import com.proyecto.servicios.model.gestopago.SendTxRespuesta;
import com.proyecto.servicios.repository.CuentaBancariaRepository;
import com.proyecto.servicios.repository.TransaccionPagoRepository;
import com.proyecto.servicios.service.Impl.TransaccionPagoServiceImpl;
import com.proyecto.servicios.service.TransaccionPagoService.OrigenRespuesta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransaccionPagoServiceTest {

    private static final String NUM_CUENTA = "4152319999888877";
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);
    private static final DatosEnvioTx ENVIO = new DatosEnvioTx("GPABC123", "20220101235900", "5555555551", "083");

    @Mock
    private CuentaBancariaRepository cuentaRepository;

    @Mock
    private TransaccionPagoRepository transaccionRepository;

    private TransaccionPagoServiceImpl service;
    private CuentaBancaria cuenta;
    private PagoServicioRequest request;

    @BeforeEach
    void setUp() {
        service = new TransaccionPagoServiceImpl(cuentaRepository, transaccionRepository, ENCODER, new GestoPagoTxProperties());

        cuenta = CuentaBancaria.builder()
                .id(10L)
                .numeroCuenta(NUM_CUENTA)
                .saldo(new BigDecimal("500.00"))
                .saldoRetenido(new BigDecimal("0.00"))
                .estado(EstadoCuenta.ACTIVA)
                .pinSeguridad(ENCODER.encode("1234"))
                .cliente(Cliente.builder().id(1L).eliminadoLogico(false).build())
                .build();

        request = PagoServicioRequest.builder()
                .numCuenta(Long.parseLong(NUM_CUENTA))
                .idServicio(133)
                .idProducto(582)
                .montoPago(new BigDecimal("100.00"))
                .idDistribuidor(83)
                .referencia("5555555551")
                .bandera(false)
                .pin("1234")
                .build();
    }

    private void cuentaBloqueable() {
        when(cuentaRepository.findByNumeroCuentaForUpdate(NUM_CUENTA)).thenReturn(Optional.of(cuenta));
    }

    private TransaccionPago transaccionEnCurso(EstadoTransaccion estado) {
        cuenta.setSaldoRetenido(new BigDecimal("100.00"));
        TransaccionPago tx = TransaccionPago.builder().id(99L).cuentaId(10L).folio("f")
                .monto(new BigDecimal("100.00")).estado(estado).build();
        when(transaccionRepository.findById(99L)).thenReturn(Optional.of(tx));
        return tx;
    }

    @Test
    void testReservarSaldo_Exito_RetieneMontoYGuardaDatosDeEnvio() {
        cuentaBloqueable();
        when(transaccionRepository.existsByCuentaIdAndReferenciaAndIdProductoAndMontoAndEstadoInAndFechaCreacionAfter(
                anyLong(), anyString(), anyInt(), any(), anyCollection(), any())).thenReturn(false);
        when(transaccionRepository.save(any(TransaccionPago.class))).thenAnswer(i -> i.getArgument(0));

        TransaccionPagoService.ReservaSaldo reserva = service.reservarSaldo(1L, null, request, ENVIO);

        assertFalse(reserva.repetida());
        assertEquals(EstadoTransaccion.PROCESANDO, reserva.transaccion().getEstado());
        assertEquals("GPABC123", reserva.transaccion().getUpc());
        assertEquals("20220101235900", reserva.transaccion().getHoraLocal());
        assertEquals(new BigDecimal("500.00"), cuenta.getSaldo());
        assertEquals(new BigDecimal("100.00"), cuenta.getSaldoRetenido());
    }

    @Test
    void testReservarSaldo_SaldoDisponibleInsuficiente_ConsideraRetenciones() {
        cuenta.setSaldoRetenido(new BigDecimal("450.00"));
        cuentaBloqueable();

        assertThrows(SaldoInsuficienteException.class, () -> service.reservarSaldo(1L, null, request, ENVIO));
        verify(transaccionRepository, never()).save(any());
    }

    @Test
    void testReservarSaldo_PinIncorrecto_TercerIntentoBloqueaCuenta() {
        cuenta.setIntentosPinFallidos(2);
        request.setPin("9999");
        cuentaBloqueable();

        assertThrows(PinInvalidoException.class, () -> service.reservarSaldo(1L, null, request, ENVIO));
        assertEquals(3, cuenta.getIntentosPinFallidos());
        assertEquals(EstadoCuenta.BLOQUEADA, cuenta.getEstado());
    }

    @Test
    void testReservarSaldo_CuentaDeOtroCliente_Rechaza() {
        cuentaBloqueable();

        assertThrows(BusinessException.class, () -> service.reservarSaldo(2L, null, request, ENVIO));
        verify(transaccionRepository, never()).save(any());
    }

    @Test
    void testReservarSaldo_PagoDuplicadoEnVentana_Rechaza() {
        cuentaBloqueable();
        when(transaccionRepository.existsByCuentaIdAndReferenciaAndIdProductoAndMontoAndEstadoInAndFechaCreacionAfter(
                anyLong(), anyString(), anyInt(), any(), anyCollection(), any())).thenReturn(true);

        assertThrows(TransaccionDuplicadaException.class, () -> service.reservarSaldo(1L, null, request, ENVIO));
    }

    @Test
    void testAplicarResultado_Aprobada_DescuentaSaldoYGuardaEvidencia() {
        TransaccionPago tx = transaccionEnCurso(EstadoTransaccion.PROCESANDO);
        when(cuentaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(cuenta));

        service.aplicarResultado(99L, ResultadoSendTx.aprobada(new SendTxRespuesta(
                "01", "Operacion realizada con exito", "65765544", "291676393", "40919958", "Instrucciones", "<RESPONSE/>")),
                OrigenRespuesta.SEND_TX);

        assertEquals(new BigDecimal("400.00"), cuenta.getSaldo());
        assertEquals(new BigDecimal("0.00"), cuenta.getSaldoRetenido());
        assertEquals(EstadoTransaccion.EXITOSA, tx.getEstado());
        assertEquals("65765544", tx.getNumeroAutorizacion());
        assertEquals("40919958", tx.getCodigoCanje());
        assertEquals("<RESPONSE/>", tx.getRespuestaProveedor());
    }

    @Test
    void testAplicarResultado_Rechazada_LiberaRetencionSinDescontar() {
        TransaccionPago tx = transaccionEnCurso(EstadoTransaccion.PROCESANDO);
        when(cuentaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(cuenta));

        service.aplicarResultado(99L, ResultadoSendTx.rechazada("02", "Saldo insuficiente"), OrigenRespuesta.SEND_TX);

        assertEquals(new BigDecimal("500.00"), cuenta.getSaldo());
        assertEquals(new BigDecimal("0.00"), cuenta.getSaldoRetenido());
        assertEquals(EstadoTransaccion.RECHAZADA, tx.getEstado());
    }

    @Test
    void testAplicarResultado_Indeterminada_ConservaRetencion() {
        TransaccionPago tx = transaccionEnCurso(EstadoTransaccion.PROCESANDO);

        service.aplicarResultado(99L, ResultadoSendTx.indeterminada("82", "Tiempo de espera alcanzado"),
                OrigenRespuesta.SEND_TX);

        assertEquals(EstadoTransaccion.PENDIENTE_CONCILIACION, tx.getEstado());
        assertEquals(new BigDecimal("100.00"), cuenta.getSaldoRetenido());
        verify(cuentaRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void testAplicarResultado_TransaccionYaFinal_NoDescuentaDosVeces() {
        transaccionEnCurso(EstadoTransaccion.EXITOSA);

        assertThrows(IllegalStateException.class, () -> service.aplicarResultado(99L,
                ResultadoSendTx.aprobada(new SendTxRespuesta("06", "Aplicada", "1", "2", null, null, null)),
                OrigenRespuesta.CONFIRM_TX));
        assertEquals(new BigDecimal("500.00"), cuenta.getSaldo());
    }
}
