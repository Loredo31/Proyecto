package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.PaginaDto;
import com.proyecto.servicios.dto.pago.PagoServicioResponseDto;
import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.mapper.SendTxPayloadMapper;
import com.proyecto.servicios.mapper.TransaccionPagoMapper;
import com.proyecto.servicios.model.gestopago.ResultadoSendTx;
import com.proyecto.servicios.model.gestopago.SendTxPayload;
import com.proyecto.servicios.service.Impl.PagoServicioServiceImpl;
import com.proyecto.servicios.service.TransaccionPagoService.OrigenRespuesta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistorialPagosTest {

    @Mock
    private ValidadorProductoPago validadorProducto;
    @Mock
    private TransaccionPagoService transaccionPagoService;
    @Mock
    private GestoPagoTxGateway gateway;
    @Mock
    private SendTxPayloadMapper payloadMapper;

    private PagoServicioServiceImpl service;

    @BeforeEach
    void setUp() {
        TransaccionPagoMapper mapper = Mappers.getMapper(TransaccionPagoMapper.class);
        service = new PagoServicioServiceImpl(validadorProducto, transaccionPagoService, gateway, payloadMapper, mapper);
    }

    private static TransaccionPago tx(String folio, EstadoTransaccion estado) {
        return TransaccionPago.builder().id(1L).folio(folio).estado(estado).clienteId(7L)
                .numeroCuenta("4152319999888877").idServicio(79).idProducto(582).nombreProducto("Recarga Telcel 100")
                .monto(new BigDecimal("100.00")).fechaCreacion(LocalDateTime.now()).build();
    }

    @Test
    void historial_SoloExitosasPaginadasConProductoYLogo() {
        when(transaccionPagoService.listar(7L, EnumSet.of(EstadoTransaccion.EXITOSA), 0, 10))
                .thenReturn(new PageImpl<>(List.of(tx("F1", EstadoTransaccion.EXITOSA)), PageRequest.of(0, 10), 11));

        PaginaDto<PagoServicioResponseDto> pagina = service.historial(7L, 0, 10);

        assertEquals(11, pagina.totalElementos());
        assertEquals(2, pagina.totalPaginas());
        PagoServicioResponseDto p = pagina.contenido().get(0);
        assertEquals("Recarga Telcel 100", p.getNombreProducto());
        assertEquals("************8877", p.getNumCuenta());
        assertTrue(p.getLogoUrl().endsWith("/servicios/79.png"));
    }

    @Test
    void pendientes_ProcesandoYEnConciliacion() {
        when(transaccionPagoService.listar(7L,
                EnumSet.of(EstadoTransaccion.PROCESANDO, EstadoTransaccion.PENDIENTE_CONCILIACION), 0, 20))
                .thenReturn(new PageImpl<>(List.of()));

        assertTrue(service.pendientes(7L, 0, 20).contenido().isEmpty());
    }

    @Test
    void rechazadas_RechazadasYErrorDelProveedor() {
        when(transaccionPagoService.listar(7L, EnumSet.of(EstadoTransaccion.RECHAZADA, EstadoTransaccion.ERROR), 0, 10))
                .thenReturn(new PageImpl<>(List.of(tx("F9", EstadoTransaccion.RECHAZADA)), PageRequest.of(0, 10), 1));

        PaginaDto<PagoServicioResponseDto> pagina = service.rechazadas(7L, 0, 10);

        assertEquals(1, pagina.totalElementos());
        assertEquals("F9", pagina.contenido().get(0).getFolio());
        assertEquals(EstadoTransaccion.RECHAZADA, pagina.contenido().get(0).getEstado());
    }

    @Test
    void verificar_PendienteConsultaAGestoPago() {
        TransaccionPago pendiente = tx("F1", EstadoTransaccion.PENDIENTE_CONCILIACION);
        SendTxPayload payload = SendTxPayload.builder().build();
        ResultadoSendTx aprobada = ResultadoSendTx.indeterminada("01", "ok");
        when(transaccionPagoService.consultar(7L, "F1")).thenReturn(pendiente);
        when(payloadMapper.toPayload(pendiente)).thenReturn(payload);
        when(gateway.confirmar(payload)).thenReturn(aprobada);
        when(transaccionPagoService.aplicarResultado(1L, aprobada, OrigenRespuesta.CONFIRM_TX))
                .thenReturn(tx("F1", EstadoTransaccion.EXITOSA));

        assertEquals(EstadoTransaccion.EXITOSA, service.verificarPago(7L, "F1").getEstado());
    }

    @Test
    void verificar_FinalOEnProcesoNoConsultaAlProveedor() {
        when(transaccionPagoService.consultar(7L, "F1")).thenReturn(tx("F1", EstadoTransaccion.EXITOSA));
        when(transaccionPagoService.consultar(7L, "F2")).thenReturn(tx("F2", EstadoTransaccion.PROCESANDO));

        assertEquals(EstadoTransaccion.EXITOSA, service.verificarPago(7L, "F1").getEstado());
        assertEquals(EstadoTransaccion.PROCESANDO, service.verificarPago(7L, "F2").getEstado());
        verify(gateway, never()).confirmar(any());
    }

    @Test
    void verificar_ProveedorCaido() {
        TransaccionPago pendiente = tx("F1", EstadoTransaccion.PENDIENTE_CONCILIACION);
        when(transaccionPagoService.consultar(7L, "F1")).thenReturn(pendiente);
        when(gateway.confirmar(any())).thenThrow(new IllegalStateException("timeout"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.verificarPago(7L, "F1"));
        assertEquals(ApiResponseEnum.PROVEEDOR_PAGOS_NO_DISPONIBLE, ex.getResponseStatus());
        verify(transaccionPagoService, never()).aplicarResultado(any(), any(), any());
    }

    @Test
    void paginaDto_DeLista() {
        PaginaDto<Integer> p = PaginaDto.deLista(List.of(1, 2, 3, 4, 5), 1, 2);
        assertEquals(List.of(3, 4), p.contenido());
        assertEquals(3, p.totalPaginas());
        assertEquals(false, p.esUltima());
        assertTrue(PaginaDto.deLista(List.of(1, 2, 3, 4, 5), 2, 2).esUltima());
        assertTrue(PaginaDto.deLista(List.<Integer>of(), 0, 2).esUltima());
    }
}
