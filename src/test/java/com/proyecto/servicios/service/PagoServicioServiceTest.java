package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.ProductDto;
import com.proyecto.servicios.dto.pago.PagoServicioRequest;
import com.proyecto.servicios.dto.pago.PagoServicioResponseDto;
import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.exception.EnvioNoRealizadoException;
import com.proyecto.servicios.exception.ProveedorNoDisponibleException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.mapper.SendTxPayloadMapper;
import com.proyecto.servicios.mapper.TransaccionPagoMapper;
import com.proyecto.servicios.model.gestopago.DatosEnvioTx;
import com.proyecto.servicios.model.gestopago.ResultadoSendTx;
import com.proyecto.servicios.model.gestopago.SendTxPayload;
import com.proyecto.servicios.model.gestopago.SendTxRespuesta;
import com.proyecto.servicios.service.Impl.PagoServicioServiceImpl;
import com.proyecto.servicios.service.TransaccionPagoService.OrigenRespuesta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServicioServiceTest {

    private static final Long CLIENTE_ID = 1L;
    private static final Long TX_ID = 99L;

    @Mock
    private ValidadorProductoPago validadorProducto;

    @Mock
    private TransaccionPagoService transaccionPagoService;

    @Mock
    private GestoPagoTxGateway gestoPagoTxGateway;

    @Mock
    private SendTxPayloadMapper payloadMapper;

    @Mock
    private TransaccionPagoMapper transaccionMapper;

    @InjectMocks
    private PagoServicioServiceImpl pagoServicioService;

    private PagoServicioRequest request;
    private ProductDto producto;
    private DatosEnvioTx envio;
    private TransaccionPago transaccion;
    private SendTxPayload payload;

    @BeforeEach
    void setUp() {
        request = PagoServicioRequest.builder()
                .numCuenta(4152319999888877L)
                .idServicio(133)
                .idProducto(582)
                .montoPago(new BigDecimal("100.00"))
                .idDistribuidor(83)
                .referencia("5555555551")
                .bandera(false)
                .pin("1234")
                .build();

        producto = ProductDto.builder().id("582").tipoFront(1).build();
        envio = new DatosEnvioTx("GPABC", "20220101235900", "5555555551", "083");

        transaccion = TransaccionPago.builder()
                .id(TX_ID)
                .folio("folio-1")
                .monto(new BigDecimal("100.00"))
                .estado(EstadoTransaccion.PROCESANDO)
                .build();

        payload = SendTxPayload.builder().idProducto(582).build();
    }

    private void reservaExitosa() {
        when(validadorProducto.validar(request)).thenReturn(producto);
        when(payloadMapper.prepararEnvio(request, producto)).thenReturn(envio);
        when(transaccionPagoService.reservarSaldo(CLIENTE_ID, null, request, envio))
                .thenReturn(new TransaccionPagoService.ReservaSaldo(transaccion, false));
        when(payloadMapper.toPayload(transaccion)).thenReturn(payload);
    }

    @Test
    void testProcesarPago_Codigo01_AplicaResultadoYDevuelveExitosa() {
        reservaExitosa();
        ResultadoSendTx aprobado = ResultadoSendTx.aprobada(
                new SendTxRespuesta("01", "Operacion realizada con exito", "100024988", "291437188", null, null, "<xml/>"));
        when(gestoPagoTxGateway.enviar(payload)).thenReturn(aprobado);

        TransaccionPago exitosa = TransaccionPago.builder().id(TX_ID).estado(EstadoTransaccion.EXITOSA).build();
        when(transaccionPagoService.aplicarResultado(TX_ID, aprobado, OrigenRespuesta.SEND_TX)).thenReturn(exitosa);
        when(transaccionMapper.toResponse(exitosa))
                .thenReturn(PagoServicioResponseDto.builder().estado(EstadoTransaccion.EXITOSA).build());

        PagoServicioResponseDto respuesta = pagoServicioService.procesarPago(CLIENTE_ID, null, request);

        assertEquals(EstadoTransaccion.EXITOSA, respuesta.getEstado());
        verify(transaccionPagoService).aplicarResultado(TX_ID, aprobado, OrigenRespuesta.SEND_TX);
        verify(transaccionPagoService, never()).liberarNoEnviada(anyLong(), any());
    }

    @Test
    void testProcesarPago_Timeout_SeDelegaComoIndeterminadaSinLiberar() {
        reservaExitosa();
        ResultadoSendTx timeout = ResultadoSendTx.indeterminada("TIMEOUT", "Sin respuesta del proveedor");
        when(gestoPagoTxGateway.enviar(payload)).thenReturn(timeout);
        when(transaccionPagoService.aplicarResultado(TX_ID, timeout, OrigenRespuesta.SEND_TX)).thenReturn(transaccion);

        pagoServicioService.procesarPago(CLIENTE_ID, null, request);

        verify(transaccionPagoService).aplicarResultado(TX_ID, timeout, OrigenRespuesta.SEND_TX);
        verify(transaccionPagoService, never()).liberarNoEnviada(anyLong(), any());
    }

    @Test
    void testProcesarPago_NoEnviado_LiberaComoErrorYResponde503() {
        reservaExitosa();
        when(gestoPagoTxGateway.enviar(payload)).thenThrow(new EnvioNoRealizadoException("sin token"));

        assertThrows(ProveedorNoDisponibleException.class,
                () -> pagoServicioService.procesarPago(CLIENTE_ID, null, request));

        verify(transaccionPagoService).liberarNoEnviada(eq(TX_ID), any());
        verify(transaccionPagoService, never()).aplicarResultado(anyLong(), any(), any());
    }

    @Test
    void testProcesarPago_SaldoInsuficiente_NoLlamaAlProveedor() {
        when(validadorProducto.validar(request)).thenReturn(producto);
        when(payloadMapper.prepararEnvio(request, producto)).thenReturn(envio);
        when(transaccionPagoService.reservarSaldo(CLIENTE_ID, null, request, envio))
                .thenThrow(new SaldoInsuficienteException());

        assertThrows(SaldoInsuficienteException.class,
                () -> pagoServicioService.procesarPago(CLIENTE_ID, null, request));

        verifyNoInteractions(gestoPagoTxGateway);
    }

    @Test
    void testProcesarPago_IdempotencyKeyRepetida_DevuelveResultadoPrevioSinReenviar() {
        when(validadorProducto.validar(request)).thenReturn(producto);
        when(payloadMapper.prepararEnvio(request, producto)).thenReturn(envio);
        when(transaccionPagoService.reservarSaldo(CLIENTE_ID, "key-1", request, envio))
                .thenReturn(new TransaccionPagoService.ReservaSaldo(transaccion, true));
        PagoServicioResponseDto dto = PagoServicioResponseDto.builder().folio("folio-1").build();
        when(transaccionMapper.toResponse(transaccion)).thenReturn(dto);

        assertSame(dto, pagoServicioService.procesarPago(CLIENTE_ID, "key-1", request));
        verifyNoInteractions(gestoPagoTxGateway);
    }

    @Test
    void testConciliarPendientes_ConfirmTxAplicada_ReenviaMismoPayloadYResuelve() {
        when(transaccionPagoService.listarPendientesConfirmables()).thenReturn(List.of(transaccion));
        when(payloadMapper.toPayload(transaccion)).thenReturn(payload);
        ResultadoSendTx aplicada = ResultadoSendTx.aprobada(
                new SendTxRespuesta("06", "La transaccion fue aplicada exitosamente", "100024988", "291437188", null, null, "<xml/>"));
        when(gestoPagoTxGateway.confirmar(payload)).thenReturn(aplicada);
        when(transaccionPagoService.aplicarResultado(TX_ID, aplicada, OrigenRespuesta.CONFIRM_TX))
                .thenReturn(TransaccionPago.builder().id(TX_ID).estado(EstadoTransaccion.EXITOSA).build());

        assertEquals(1, pagoServicioService.conciliarPendientes());
    }

    @Test
    void testConciliarPendientes_ConfirmTxNoDisponible_NoLiberaRetencion() {
        when(transaccionPagoService.listarPendientesConfirmables()).thenReturn(List.of(transaccion));
        when(payloadMapper.toPayload(transaccion)).thenReturn(payload);
        when(gestoPagoTxGateway.confirmar(payload)).thenThrow(new EnvioNoRealizadoException("sin conexión"));

        assertEquals(0, pagoServicioService.conciliarPendientes());
        verify(transaccionPagoService, never()).aplicarResultado(anyLong(), any(), any());
        verify(transaccionPagoService, never()).liberarNoEnviada(anyLong(), any());
    }
}
