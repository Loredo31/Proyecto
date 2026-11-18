package com.proyecto.servicios.seguridad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.enums.Plataforma;
import com.proyecto.servicios.service.SesionService;
import com.proyecto.servicios.util.JwtTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Canal en tiempo real por sesión ({@code /ws/sesion}).
 * <p>
 * Protocolo: el cliente se conecta y envía {@code {"tipo":"AUTH","token":"<access token>"}} como primer mensaje
 * (el token no viaja en la URL para no quedar en logs). Después recibe:
 * <ul>
 *   <li>{@code SOLICITUD_CIERRE}: otro dispositivo pide iniciar sesión; se responde por REST.</li>
 *   <li>{@code SESION_CERRADA}: la sesión terminó; la app debe volver al login.</li>
 * </ul>
 */
@Slf4j
@Component
public class SesionWebSocketHandler extends TextWebSocketHandler implements NotificadorSesion {

    static final Duration TIEMPO_AUTENTICACION = Duration.ofSeconds(10);
    private static final int TIEMPO_ENVIO_MS = 5000;
    private static final int BUFFER_ENVIO = 64 * 1024;
    private static final String ATRIBUTO_SESION = "sesionId";
    private static final String ATRIBUTO_CONEXION = "conectadoEn";

    private final JwtTokenService jwtTokenService;
    private final SesionService sesionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** sesionId de negocio -> conexión WebSocket autenticada. */
    private final Map<String, WebSocketSession> conexiones = new ConcurrentHashMap<>();
    /** Conexiones abiertas que aún no envían su token. */
    private final Map<String, WebSocketSession> pendientes = new ConcurrentHashMap<>();

    public SesionWebSocketHandler(JwtTokenService jwtTokenService, @Lazy SesionService sesionService) {
        this.jwtTokenService = jwtTokenService;
        this.sesionService = sesionService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        session.getAttributes().put(ATRIBUTO_CONEXION, Instant.now());
        pendientes.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        if (session.getAttributes().containsKey(ATRIBUTO_SESION)) {
            enviar(session, Map.of("tipo", "PONG"));
            return;
        }
        Optional<UsuarioAutenticado> usuario = leerToken(message.getPayload())
                .flatMap(jwtTokenService::validar)
                .filter(u -> sesionService.estaActiva(u.sesionId()));
        pendientes.remove(session.getId());
        if (usuario.isEmpty()) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Token inválido o sesión finalizada"));
            return;
        }

        String sesionId = usuario.get().sesionId();
        session.getAttributes().put(ATRIBUTO_SESION, sesionId);
        WebSocketSession segura = new ConcurrentWebSocketSessionDecorator(session, TIEMPO_ENVIO_MS, BUFFER_ENVIO);
        WebSocketSession anterior = conexiones.put(sesionId, segura);
        if (anterior != null && anterior.isOpen()) {
            anterior.close(CloseStatus.NORMAL.withReason("Reemplazada por una conexión nueva"));
        }
        enviar(segura, Map.of("tipo", "AUTENTICADO"));
    }

    private Optional<String> leerToken(String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            if (json != null && "AUTH".equals(json.path("tipo").asText()) && json.hasNonNull("token")) {
                return Optional.of(json.get("token").asText());
            }
        } catch (IOException ex) {
            log.debug("Mensaje WebSocket con formato inválido");
        }
        return Optional.empty();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        pendientes.remove(session.getId());
        Object sesionId = session.getAttributes().get(ATRIBUTO_SESION);
        if (sesionId != null) {
            conexiones.computeIfPresent(sesionId.toString(),
                    (k, actual) -> actual.getId().equals(session.getId()) ? null : actual);
        }
    }

    /** Cierra las conexiones que no se autenticaron a tiempo. */
    @Scheduled(fixedDelay = 5000)
    public void cerrarNoAutenticadas() {
        Instant limite = Instant.now().minus(TIEMPO_AUTENTICACION);
        pendientes.values().removeIf(s -> {
            Instant conectado = (Instant) s.getAttributes().get(ATRIBUTO_CONEXION);
            if (conectado != null && conectado.isBefore(limite)) {
                cerrarSilencioso(s, CloseStatus.POLICY_VIOLATION.withReason("No se recibió el token a tiempo"));
                return true;
            }
            return false;
        });
    }

    @Override
    public void notificarSolicitudCierre(String sesionId, String solicitudId, Plataforma plataforma,
                                         String dispositivo, long expiraEnSegundos) {
        Map<String, Object> aviso = new LinkedHashMap<>();
        aviso.put("tipo", "SOLICITUD_CIERRE");
        aviso.put("solicitudId", solicitudId);
        aviso.put("plataforma", plataforma.name());
        aviso.put("dispositivo", dispositivo);
        aviso.put("expiraEnSegundos", expiraEnSegundos);
        Optional.ofNullable(conexiones.get(sesionId)).ifPresent(s -> enviar(s, aviso));
    }

    @Override
    public void notificarSesionCerrada(String sesionId, String motivo) {
        WebSocketSession conexion = conexiones.remove(sesionId);
        if (conexion != null) {
            Map<String, Object> aviso = new LinkedHashMap<>();
            aviso.put("tipo", "SESION_CERRADA");
            aviso.put("motivo", motivo);
            enviar(conexion, aviso);
            cerrarSilencioso(conexion, CloseStatus.NORMAL.withReason("Sesión finalizada"));
        }
    }

    boolean estaConectada(String sesionId) {
        return conexiones.containsKey(sesionId);
    }

    private void enviar(WebSocketSession session, Map<String, ?> contenido) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(contenido)));
            }
        } catch (IOException | IllegalStateException ex) {
            log.warn("No se pudo enviar el aviso por WebSocket: {}", ex.getMessage());
        }
    }

    private static void cerrarSilencioso(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException ex) {
            log.debug("Error al cerrar la conexión WebSocket: {}", ex.getMessage());
        }
    }
}
