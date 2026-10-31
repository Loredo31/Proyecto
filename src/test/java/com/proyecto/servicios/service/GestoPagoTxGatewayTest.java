package com.proyecto.servicios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.config.GestoPagoTxProperties;
import com.proyecto.servicios.exception.EnvioNoRealizadoException;
import com.proyecto.servicios.model.gestopago.ResultadoSendTx;
import com.proyecto.servicios.model.gestopago.SendTxPayload;
import com.proyecto.servicios.service.Impl.GestoPagoTxGatewayImpl;
import com.proyecto.servicios.util.GestoPagoAesCipher;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Las respuestas XML son las publicadas en la documentación de GestoPago. */
@ExtendWith(MockitoExtension.class)
class GestoPagoTxGatewayTest {

    private static final Request REQUEST = Request.create(Request.HttpMethod.POST, "https://gp/sendTx.do",
            Collections.emptyMap(), null, StandardCharsets.UTF_8, null);

    private static final String XML_EXITO = """
            <?xml version='1.0' encoding='UTF-8'?>
            <RESPONSE>
                <ID_TX>291437188</ID_TX>
                <NUM_AUTORIZACION>100024988</NUM_AUTORIZACION>
                <SALDO>157731.83</SALDO>
                <MONTO>10.0</MONTO>
                <MENSAJE>
                    <CODIGO>01</CODIGO>
                    <TEXTO>Operacion realizada con exito</TEXTO>
                    <REFERENCIA></REFERENCIA>
                </MENSAJE>
            </RESPONSE>""";

    private static final String XML_GIFT_CARD = """
            <?xml version='1.0' encoding='UTF-8'?>
            <RESPONSE>
                <ID_TX>291676393</ID_TX>
                <NUM_AUTORIZACION>65765544</NUM_AUTORIZACION>
                <MENSAJE>
                    <CODIGO>01</CODIGO>
                    <PIN>40919958</PIN>
                    <legend><![CDATA[Visita www.netflix.com/codigo]]></legend>
                    <TEXTO>Operacion realizada con exito</TEXTO>
                    <REFERENCIA></REFERENCIA>
                </MENSAJE>
            </RESPONSE>""";

    private static String xmlCodigo(String codigo, String texto, String autorizacion) {
        return "<?xml version='1.0' encoding='UTF-8'?><RESPONSE><NUM_AUTORIZACION>" + autorizacion
                + "</NUM_AUTORIZACION><MENSAJE><CODIGO>" + codigo + "</CODIGO><TEXTO>" + texto
                + "</TEXTO></MENSAJE></RESPONSE>";
    }

    @Mock
    private GestoPagoTxClient client;

    @Mock
    private GestoPagoTokenService tokenService;

    private GestoPagoTxGatewayImpl gateway;

    @BeforeEach
    void setUp() {
        GestoPagoTxProperties properties = new GestoPagoTxProperties();
        properties.getAes().setKey("HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA=");
        properties.getAes().setIv("p5ldmBPdd/9pjC0bDC/nSg==");
        ObjectMapper objectMapper = new ObjectMapper();
        gateway = new GestoPagoTxGatewayImpl(client, tokenService, new GestoPagoAesCipher(properties),
                new SendTxResponseParser(objectMapper), properties, objectMapper);
        when(tokenService.obtenerTokenVigente()).thenReturn("tkn");
    }

    private static Response respuesta(int status, String cuerpo) {
        return Response.builder().request(REQUEST).status(status).headers(Collections.emptyMap())
                .body(cuerpo, StandardCharsets.UTF_8).build();
    }

    private static SendTxPayload payload() {
        return SendTxPayload.builder().idProducto(582).idServicio(133).telefono("5555555551")
                .horaLocal("20220101235900").upc("GPABC123").unidad("083").build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testEnviar_Codigo01_AprobadaConFormularioRandomYSigned() {
        when(client.sendTx(eq("Bearer tkn"), isNull(), anyMap())).thenReturn(respuesta(200, XML_EXITO));

        ResultadoSendTx resultado = gateway.enviar(payload());

        assertEquals(ResultadoSendTx.Tipo.APROBADA, resultado.tipo());
        assertEquals("100024988", resultado.respuesta().numeroAutorizacion());
        assertEquals("291437188", resultado.respuesta().idTx());

        ArgumentCaptor<Map<String, ?>> form = ArgumentCaptor.forClass(Map.class);
        verify(client).sendTx(eq("Bearer tkn"), isNull(), form.capture());
        assertTrue(form.getValue().containsKey("random"));
        assertTrue(form.getValue().containsKey("signed"));
    }

    @Test
    void testEnviar_GiftCard_DevuelveCodigoDeCanje() {
        when(client.sendTx(anyString(), any(), anyMap())).thenReturn(respuesta(200, XML_GIFT_CARD));

        ResultadoSendTx resultado = gateway.enviar(payload());

        assertEquals(ResultadoSendTx.Tipo.APROBADA, resultado.tipo());
        assertEquals("40919958", resultado.respuesta().codigoCanje());
        assertEquals("Visita www.netflix.com/codigo", resultado.respuesta().leyenda());
    }

    @Test
    void testEnviar_Codigo02SaldoInsuficienteProveedor_Rechazada() {
        when(client.sendTx(anyString(), any(), anyMap()))
                .thenReturn(respuesta(200, xmlCodigo("02", "Saldo insuficiente", "-1")));

        ResultadoSendTx resultado = gateway.enviar(payload());

        assertEquals(ResultadoSendTx.Tipo.RECHAZADA, resultado.tipo());
        assertNull(resultado.respuesta().numeroAutorizacion());
    }

    @Test
    void testEnviar_Codigo82TimeoutProveedor_Indeterminada() {
        when(client.sendTx(anyString(), any(), anyMap())).thenReturn(respuesta(200,
                xmlCodigo("82", "Tiempo de espera alcanzado, la transaccion no ha sido registrada", "-1")));

        assertEquals(ResultadoSendTx.Tipo.INDETERMINADA, gateway.enviar(payload()).tipo());
    }

    @Test
    void testEnviar_Codigo06Duplicado_Indeterminada() {
        when(client.sendTx(anyString(), any(), anyMap())).thenReturn(respuesta(200,
                xmlCodigo("06", "Transaccion aplicada anteriormente", "100024988")));

        assertEquals(ResultadoSendTx.Tipo.INDETERMINADA, gateway.enviar(payload()).tipo());
    }

    @Test
    void testEnviar_ReadTimeout_Indeterminada() {
        when(client.sendTx(anyString(), any(), anyMap())).thenThrow(new RetryableException(-1, "Read timed out",
                Request.HttpMethod.POST, new SocketTimeoutException("Read timed out"), (Long) null, REQUEST));

        assertEquals(ResultadoSendTx.Tipo.INDETERMINADA, gateway.enviar(payload()).tipo());
    }

    @Test
    void testEnviar_ConexionRechazada_NoEnviado() {
        when(client.sendTx(anyString(), any(), anyMap())).thenThrow(new RetryableException(-1, "Connection refused",
                Request.HttpMethod.POST, new ConnectException("Connection refused"), (Long) null, REQUEST));

        assertThrows(EnvioNoRealizadoException.class, () -> gateway.enviar(payload()));
    }

    @Test
    void testEnviar_403TokenExpirado_RenuevaTokenYReintentaUnaVez() {
        when(client.sendTx(anyString(), any(), anyMap()))
                .thenReturn(respuesta(403, "{\"success\":false,\"token\":\"EXPIRED\",\"message\":\"Your token is expired\"}"))
                .thenReturn(respuesta(200, XML_EXITO));

        assertEquals(ResultadoSendTx.Tipo.APROBADA, gateway.enviar(payload()).tipo());
        verify(tokenService).invalidarToken();
    }

    @Test
    void testConfirmar_Codigo06_Aplicada() {
        when(client.confirmTx(anyString(), any(), anyMap())).thenReturn(respuesta(200,
                "<RESPONSE><NUM_AUTORIZACION>100024988</NUM_AUTORIZACION><MENSAJE><CODIGO>06</CODIGO>"
                        + "<TEXTO>La transaccion fue aplicada exitosamente.</TEXTO><ID_TX>291437188</ID_TX></MENSAJE></RESPONSE>"));

        ResultadoSendTx resultado = gateway.confirmar(payload());

        assertEquals(ResultadoSendTx.Tipo.APROBADA, resultado.tipo());
        assertEquals("100024988", resultado.respuesta().numeroAutorizacion());
    }

    @Test
    void testConfirmar_Codigo70_NoAplicada() {
        when(client.confirmTx(anyString(), any(), anyMap())).thenReturn(respuesta(200,
                "<RESPONSE><NUM_AUTORIZACION>0</NUM_AUTORIZACION><MENSAJE><CODIGO>70</CODIGO>"
                        + "<TEXTO>La transaccion no fue aplicada</TEXTO><ID_TX>-1</ID_TX></MENSAJE></RESPONSE>"));

        assertEquals(ResultadoSendTx.Tipo.RECHAZADA, gateway.confirmar(payload()).tipo());
    }

    @Test
    void testConfirmar_Http400_SigueIndeterminadaSinLiberar() {
        when(client.confirmTx(anyString(), any(), anyMap())).thenReturn(respuesta(400, "Bad request"));

        assertEquals(ResultadoSendTx.Tipo.INDETERMINADA, gateway.confirmar(payload()).tipo());
    }
}
