package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.CorreoProperties;
import com.proyecto.servicios.enums.PropositoCodigo;
import com.proyecto.servicios.exception.CorreoNoEnviadoException;
import com.proyecto.servicios.service.CorreoService;
import com.proyecto.servicios.util.DatosSensiblesUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Envía correos con la API transaccional de Brevo ({@code POST /smtp/email}) por HTTPS.
 * Se usa la API HTTP y no SMTP porque los planes gratuitos de hosting suelen bloquear los puertos SMTP.
 */
@Slf4j
@Service
public class BrevoCorreoService implements CorreoService {

    private static final String RUTA_ENVIO = "/smtp/email";

    private final CorreoProperties properties;
    private final RestClient restClient;

    /**
     * Usa el cliente HTTP estándar de Java (no el builder autoconfigurado de Spring), con timeouts propios.
     */
    @Autowired
    public BrevoCorreoService(CorreoProperties properties) {
        this(properties, RestClient.builder().requestFactory(conTimeouts(properties)).baseUrl(properties.getBrevoUrl()).build());
    }

    /** Permite inyectar el cliente HTTP (p. ej. uno simulado en pruebas). */
    public BrevoCorreoService(CorreoProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
        if (!properties.configurado()) {
            log.warn("BREVO_API_KEY o CORREO_REMITENTE no están configurados; no se podrán enviar correos");
        }
    }

    private static SimpleClientHttpRequestFactory conTimeouts(CorreoProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getTimeout());
        factory.setReadTimeout(properties.getTimeout());
        return factory;
    }

    @Override
    public void enviarCodigo(String destino, PropositoCodigo proposito, String codigo, long minutosVigencia) {
        String accion = switch (proposito) {
            case LOGIN -> "iniciar sesión";
            case RECUPERACION -> "restablecer tu contraseña";
            case CIERRE_SESION -> "cerrar tu sesión en el otro dispositivo e iniciar una nueva";
        };
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;color:#1f2937">
                  <h2 style="color:#0f766e">Código de verificación</h2>
                  <p>Usa este código para %s:</p>
                  <p style="font-size:32px;font-weight:bold;letter-spacing:8px;text-align:center">%s</p>
                  <p>Vence en %d minutos y solo puede usarse una vez.</p>
                  <p style="color:#6b7280;font-size:13px">Si no fuiste tú, ignora este correo y cambia tu contraseña.
                  Nunca compartas este código: nadie de nuestro equipo te lo pedirá.</p>
                </div>""".formatted(accion, codigo, minutosVigencia);
        enviar(destino, "Tu código de verificación", html);
    }

    @Override
    public void notificarCambioContrasena(String destino) {
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;color:#1f2937">
                  <h2 style="color:#0f766e">Tu contraseña fue cambiada</h2>
                  <p>La contraseña de tu cuenta se restableció correctamente y se cerraron tus sesiones activas.</p>
                  <p style="color:#6b7280;font-size:13px">Si no fuiste tú, contacta a soporte de inmediato.</p>
                </div>""";
        enviar(destino, "Tu contraseña fue cambiada", html);
    }

    private void enviar(String destino, String asunto, String html) {
        if (!properties.configurado()) {
            throw new CorreoNoEnviadoException("El servicio de correo no está configurado");
        }
        Map<String, Object> cuerpo = Map.of(
                "sender", Map.of("name", properties.getNombreRemitente(), "email", properties.getRemitente()),
                "to", List.of(Map.of("email", destino)),
                "subject", asunto,
                "htmlContent", html);
        try {
            restClient.post()
                    .uri(RUTA_ENVIO)
                    .header("api-key", properties.getBrevoApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Correo enviado a {}", DatosSensiblesUtil.enmascararCorreo(destino));
        } catch (RestClientException ex) {
            log.error("Brevo rechazó o no respondió el envío a {}: {}",
                    DatosSensiblesUtil.enmascararCorreo(destino), ex.getClass().getSimpleName());
            throw new CorreoNoEnviadoException("No fue posible enviar el correo", ex);
        }
    }
}
