package com.proyecto.servicios.seguridad;

import com.proyecto.servicios.config.SeguridadProperties;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.enums.Plataforma;
import com.proyecto.servicios.service.SesionService;
import com.proyecto.servicios.util.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SesionWebSocketHandlerTest {

    private JwtTokenService jwt;
    private SesionService sesiones;
    private SesionWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        jwt = new JwtTokenService("secreto-de-pruebas-con-longitud-suficiente-0123456789", new SeguridadProperties());
        sesiones = mock(SesionService.class);
        handler = new SesionWebSocketHandler(jwt, sesiones);
    }

    private static WebSocketSession conexion(String id) {
        WebSocketSession ws = mock(WebSocketSession.class);
        Map<String, Object> atributos = new HashMap<>();
        when(ws.getId()).thenReturn(id);
        when(ws.getAttributes()).thenReturn(atributos);
        when(ws.isOpen()).thenReturn(true);
        return ws;
    }

    private String token(String sesionId) {
        Usuario u = Usuario.builder().id(1L).correo("a@example.com")
                .cliente(Cliente.builder().id(1L).build()).build();
        return jwt.generarAccessToken(u, sesionId);
    }

    private static List<String> enviados(WebSocketSession ws) throws Exception {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<WebSocketMessage<?>> captor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(ws, atLeastOnce()).sendMessage(captor.capture());
        return captor.getAllValues().stream().map(m -> m.getPayload().toString()).toList();
    }

    @Test
    void autenticaYRecibeAvisos() throws Exception {
        when(sesiones.estaActiva("S-1")).thenReturn(true);
        WebSocketSession ws = conexion("ws1");
        handler.afterConnectionEstablished(ws);

        handler.handleTextMessage(ws, new TextMessage("{\"tipo\":\"AUTH\",\"token\":\"" + token("S-1") + "\"}"));
        assertTrue(handler.estaConectada("S-1"));

        handler.handleTextMessage(ws, new TextMessage("{\"tipo\":\"PING\"}"));
        handler.notificarSolicitudCierre("S-1", "SOL-1", Plataforma.WEB, "Chrome", 120);
        handler.notificarSolicitudCierre("OTRA", "SOL-2", Plataforma.WEB, null, 120);
        handler.notificarSesionCerrada("S-1", "Cerrada desde la web");

        List<String> mensajes = enviados(ws);
        assertTrue(mensajes.get(0).contains("AUTENTICADO"));
        assertTrue(mensajes.get(1).contains("PONG"));
        assertTrue(mensajes.get(2).contains("SOLICITUD_CIERRE") && mensajes.get(2).contains("SOL-1"));
        assertTrue(mensajes.get(3).contains("SESION_CERRADA"));
        assertFalse(handler.estaConectada("S-1"));
        verify(ws).close(any(CloseStatus.class));
    }

    @Test
    void rechazaTokenInvalidoOSesionInactiva() throws Exception {
        when(sesiones.estaActiva("S-2")).thenReturn(false);
        WebSocketSession invalido = conexion("ws1");
        WebSocketSession inactiva = conexion("ws2");
        WebSocketSession basura = conexion("ws3");

        handler.handleTextMessage(invalido, new TextMessage("{\"tipo\":\"AUTH\",\"token\":\"x\"}"));
        handler.handleTextMessage(inactiva, new TextMessage("{\"tipo\":\"AUTH\",\"token\":\"" + token("S-2") + "\"}"));
        handler.handleTextMessage(basura, new TextMessage("no es json"));

        verify(invalido).close(any(CloseStatus.class));
        verify(inactiva).close(any(CloseStatus.class));
        verify(basura).close(any(CloseStatus.class));
        assertFalse(handler.estaConectada("S-2"));
    }

    @Test
    void reemplazaConexionAnteriorYLimpiaAlCerrar() throws Exception {
        when(sesiones.estaActiva("S-1")).thenReturn(true);
        WebSocketSession primera = conexion("ws1");
        WebSocketSession segunda = conexion("ws2");
        TextMessage auth = new TextMessage("{\"tipo\":\"AUTH\",\"token\":\"" + token("S-1") + "\"}");

        handler.handleTextMessage(primera, auth);
        handler.handleTextMessage(segunda, auth);
        verify(primera).close(any(CloseStatus.class));

        handler.afterConnectionClosed(primera, CloseStatus.NORMAL);
        assertTrue(handler.estaConectada("S-1"));
        handler.afterConnectionClosed(segunda, CloseStatus.NORMAL);
        assertFalse(handler.estaConectada("S-1"));
    }

    @Test
    void cierraConexionesQueNoSeAutentican() throws Exception {
        WebSocketSession lenta = conexion("ws1");
        WebSocketSession reciente = conexion("ws2");
        handler.afterConnectionEstablished(lenta);
        handler.afterConnectionEstablished(reciente);
        lenta.getAttributes().put("conectadoEn", Instant.now().minusSeconds(60));

        handler.cerrarNoAutenticadas();

        verify(lenta).close(any(CloseStatus.class));
        verify(reciente, never()).close(any(CloseStatus.class));
        handler.afterConnectionClosed(conexion("sin-sesion"), CloseStatus.NORMAL);
    }

    @Test
    void errorAlEnviarNoInterrumpe() throws Exception {
        when(sesiones.estaActiva("S-1")).thenReturn(true);
        WebSocketSession ws = conexion("ws1");
        org.mockito.Mockito.doThrow(new java.io.IOException("roto")).when(ws).sendMessage(any());

        handler.handleTextMessage(ws, new TextMessage("{\"tipo\":\"AUTH\",\"token\":\"" + token("S-1") + "\"}"));
        handler.notificarSesionCerrada("S-1", "x");

        assertFalse(handler.estaConectada("S-1"));
    }
}
