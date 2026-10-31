package com.proyecto.servicios.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.GestoPagoTxProperties;
import com.proyecto.servicios.dto.ProductDto;
import com.proyecto.servicios.dto.pago.PagoServicioRequest;
import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.model.gestopago.DatosEnvioTx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SendTxPayloadMapperTest {

    private SendTxPayloadMapper mapper;

    @BeforeEach
    void setUp() {
        GestoPagoTxProperties properties = new GestoPagoTxProperties();
        properties.setUnidad("083");
        // 2022-01-02T05:59:00Z = 2022-01-01 23:59:00 en America/Mexico_City
        Clock clock = Clock.fixed(Instant.parse("2022-01-02T05:59:00Z"), ZoneOffset.UTC);
        mapper = new SendTxPayloadMapper(properties, clock);
    }

    private static PagoServicioRequest request(boolean conReferencia, String referencia) {
        return PagoServicioRequest.builder()
                .idServicio(76).idProducto(205).montoPago(new BigDecimal("601.99"))
                .referencia(referencia).bandera(conReferencia).build();
    }

    private static TransaccionPago transaccion(PagoServicioRequest request, DatosEnvioTx envio) {
        return TransaccionPago.builder()
                .idServicio(request.getIdServicio()).idProducto(request.getIdProducto())
                .referencia(request.getReferencia()).conReferencia(request.getBandera())
                .monto(request.getMontoPago()).upc(envio.upc()).horaLocal(envio.horaLocal())
                .telefonoEnvio(envio.telefono()).unidad(envio.unidad()).build();
    }

    @Test
    void testPrepararEnvio_GeneraUpcValidoYHoraLocalDelProveedor() {
        DatosEnvioTx envio = mapper.prepararEnvio(request(false, "5555555551"), null);
        DatosEnvioTx otro = mapper.prepararEnvio(request(false, "5555555551"), null);

        assertEquals("20220101235900", envio.horaLocal());
        assertEquals(15, envio.upc().length());
        assertTrue(envio.upc().matches("[A-Z0-9]+"));
        assertNotEquals(envio.upc(), otro.upc());
        assertEquals("083", envio.unidad());
    }

    @Test
    void testRecarga_TelefonoDelClienteSinReferenciaNiMonto() throws Exception {
        PagoServicioRequest req = request(false, "5555555551");
        DatosEnvioTx envio = mapper.prepararEnvio(req, null);

        String json = new ObjectMapper().writeValueAsString(mapper.toPayload(transaccion(req, envio)));

        assertEquals("{\"idProducto\":205,\"idServicio\":76,\"telefono\":\"5555555551\",\"horaLocal\":\"20220101235900\","
                + "\"upc\":\"" + envio.upc() + "\",\"unidad\":\"083\"}", json);
    }

    @Test
    void testPagoServicio_TelefonoFijoConReferenciaYMonto() throws Exception {
        PagoServicioRequest req = request(true, "25720381");
        DatosEnvioTx envio = mapper.prepararEnvio(req, null);

        String json = new ObjectMapper().writeValueAsString(mapper.toPayload(transaccion(req, envio)));

        assertEquals("{\"idProducto\":205,\"idServicio\":76,\"telefono\":\"1111111111\",\"horaLocal\":\"20220101235900\","
                + "\"referencia\":\"25720381\",\"montoPago\":601.99,\"upc\":\"" + envio.upc() + "\",\"unidad\":\"083\"}", json);
    }

    @Test
    void testGiftCard_TelefonoFijoConAlmohadilla() {
        ProductDto giftCard = ProductDto.builder().id("7030").idCatTipoServicio(11).tipoFront(1).build();

        DatosEnvioTx envio = mapper.prepararEnvio(request(false, "5555555551"), giftCard);

        assertEquals("#1111111111", envio.telefono());
    }
}
