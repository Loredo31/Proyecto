package com.proyecto.servicios.service;

import com.proyecto.servicios.config.GestoPagoTxProperties;
import com.proyecto.servicios.dto.pago.PagoServicioRequest;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.CuentaBancaria;
import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.enums.EstadoCuenta;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.exception.CuentaInactivaException;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransaccionPagoCoberturaTest {

    private static final String NUM = "4152319999888877";
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);
    private static final String PIN_HASH = ENCODER.encode("1234");
    private static final DatosEnvioTx ENVIO = new DatosEnvioTx("UPC", "20220101235900", "5555555551", "083");

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
                .id(10L).numeroCuenta(NUM)
                .saldo(new BigDecimal("500.00")).saldoRetenido(new BigDecimal("0.00"))
                .estado(EstadoCuenta.ACTIVA).pinSeguridad(PIN_HASH)
                .cliente(Cliente.builder().id(1L).eliminadoLogico(false).build())
                .build();
        request = PagoServicioRequest.builder()
                .numCuenta(Long.parseLong(NUM)).idServicio(133).idProducto(582)
                .montoPago(new BigDecimal("100.00")).idDistribuidor(83)
                .referencia("5555555551").bandera(false).pin("1234")
                .build();
    }

    private void cuentaBloqueable() {
        when(cuentaRepository.findByNumeroCuentaForUpdate(NUM)).thenReturn(Optional.of(cuenta));
    }

    private TransaccionPago previa(String referencia) {
        return TransaccionPago.builder().id(5L).folio("F-PREVIA").cuentaId(10L).referencia(referencia)
                .idProducto(582).monto(new BigDecimal("100.00")).estado(EstadoTransaccion.EXITOSA).build();
    }

    @Test
    void reservar_IdempotencyKeyRepetidaMismaOperacion() {
        cuentaBloqueable();
        TransaccionPago previa = previa("5555555551");
        when(transaccionRepository.findByIdempotencyKey("K1")).thenReturn(Optional.of(previa));

        TransaccionPagoService.ReservaSaldo reserva = service.reservarSaldo(1L, "K1", request, ENVIO);

        assertTrue(reserva.repetida());
        assertSame(previa, reserva.transaccion());
    }

    @Test
    void reservar_IdempotencyKeyRepetidaOperacionDistinta() {
        cuentaBloqueable();
        when(transaccionRepository.findByIdempotencyKey("K1")).thenReturn(Optional.of(previa("OTRA")));

        assertThrows(TransaccionDuplicadaException.class, () -> service.reservarSaldo(1L, "K1", request, ENVIO));
    }

    @Test
    void reservar_PinCorrectoReiniciaIntentosFallidos() {
        cuenta.setIntentosPinFallidos(2);
        cuentaBloqueable();
        when(transaccionRepository.findByIdempotencyKey("K2")).thenReturn(Optional.of(previa("5555555551")));

        service.reservarSaldo(1L, "K2", request, ENVIO);

        assertEquals(0, cuenta.getIntentosPinFallidos());
    }

    @Test
    void reservar_CuentaNoActiva() {
        cuenta.setEstado(EstadoCuenta.BLOQUEADA);
        cuentaBloqueable();

        assertThrows(CuentaInactivaException.class, () -> service.reservarSaldo(1L, null, request, ENVIO));
    }

    @Test
    void reservar_TitularEliminado() {
        cuenta.getCliente().setEliminadoLogico(true);
        cuentaBloqueable();

        assertThrows(CuentaInactivaException.class, () -> service.reservarSaldo(1L, null, request, ENVIO));
    }

    @Test
    void reservar_SinPinConfigurado() {
        cuenta.setPinSeguridad(null);
        cuentaBloqueable();

        assertThrows(BusinessException.class, () -> service.reservarSaldo(1L, null, request, ENVIO));
    }

    @Test
    void liberarNoEnviada_LiberaRetencion() {
        cuenta.setSaldoRetenido(new BigDecimal("100.00"));
        TransaccionPago tx = TransaccionPago.builder().id(99L).cuentaId(10L).folio("f")
                .monto(new BigDecimal("100.00")).estado(EstadoTransaccion.PROCESANDO).build();
        when(transaccionRepository.findById(99L)).thenReturn(Optional.of(tx));
        when(cuentaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(cuenta));

        TransaccionPago r = service.liberarNoEnviada(99L, "timeout de conexion");

        assertEquals(EstadoTransaccion.ERROR, r.getEstado());
        assertEquals("NO_ENVIADO", r.getCodigoRespuesta());
        assertEquals(0, cuenta.getSaldoRetenido().compareTo(BigDecimal.ZERO));
    }

    @Test
    void aplicarResultado_ConfirmTxGuardaEvidencia() {
        TransaccionPago tx = TransaccionPago.builder().id(99L).cuentaId(10L).folio("f")
                .monto(new BigDecimal("100.00")).estado(EstadoTransaccion.PENDIENTE_CONCILIACION).build();
        when(transaccionRepository.findById(99L)).thenReturn(Optional.of(tx));
        SendTxRespuesta resp = new SendTxRespuesta("06", "en proceso", null, null, null, null, "<xml/>");

        TransaccionPago r = service.aplicarResultado(99L, ResultadoSendTx.indeterminada(resp), OrigenRespuesta.CONFIRM_TX);

        assertEquals("<xml/>", r.getRespuestaConfirmacion());
        assertEquals(EstadoTransaccion.PENDIENTE_CONCILIACION, r.getEstado());
    }

    @Test
    void aplicarResultado_TransaccionFinalFalla() {
        TransaccionPago tx = TransaccionPago.builder().id(99L).folio("f").estado(EstadoTransaccion.EXITOSA).build();
        when(transaccionRepository.findById(99L)).thenReturn(Optional.of(tx));
        ResultadoSendTx resultado = ResultadoSendTx.indeterminada("06", "x");

        assertThrows(IllegalStateException.class,
                () -> service.aplicarResultado(99L, resultado, OrigenRespuesta.SEND_TX));
    }

    @Test
    void consultar_PorFolio() {
        TransaccionPago tx = TransaccionPago.builder().folio("F").clienteId(1L).build();
        when(transaccionRepository.findByFolio("F")).thenReturn(Optional.of(tx));

        assertSame(tx, service.consultar(1L, "F"));
        assertThrows(BusinessException.class, () -> service.consultar(2L, "F"));
    }

    @Test
    void conciliacion_ListadosYConteos() {
        TransaccionPago atascada = TransaccionPago.builder().estado(EstadoTransaccion.PROCESANDO).build();
        when(transaccionRepository.findByEstadoAndFechaCreacionBetween(eq(EstadoTransaccion.PENDIENTE_CONCILIACION), any(), any()))
                .thenReturn(List.of(atascada));
        when(transaccionRepository.countByEstadoAndFechaCreacionBefore(eq(EstadoTransaccion.PENDIENTE_CONCILIACION), any()))
                .thenReturn(4L);
        when(transaccionRepository.findByEstadoAndFechaActualizacionBefore(eq(EstadoTransaccion.PROCESANDO), any()))
                .thenReturn(List.of(atascada));

        assertEquals(1, service.listarPendientesConfirmables().size());
        assertEquals(4L, service.contarPendientesFueraDeVentana());
        assertEquals(1, service.moverProcesandoVencidasAConciliacion());
        assertEquals(EstadoTransaccion.PENDIENTE_CONCILIACION, atascada.getEstado());
    }
}
