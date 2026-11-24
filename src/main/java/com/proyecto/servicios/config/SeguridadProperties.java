package com.proyecto.servicios.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Parámetros de seguridad (prefijo {@code seguridad}). Los valores sensibles llegan por variables de entorno.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "seguridad")
public class SeguridadProperties {

    /** Vida del access token (JWT) que se envía en cada petición. */
    private Duration accessTokenTtl = Duration.ofMinutes(15);

    private Sesion sesion = new Sesion();
    private Login login = new Login();
    private Cookie cookie = new Cookie();
    private Codigo codigo = new Codigo();

    /** Orígenes permitidos para CORS (patrones, p. ej. https://*.vercel.app o https://*.up.railway.app). */
    private List<String> corsOrigenes = new ArrayList<>(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://*.up.railway.app",
            "https://*.railway.app"
    ));

    @Getter
    @Setter
    public static class Sesion {
        /** Tiempo sin peticiones tras el cual la sesión se cierra por inactividad (web y móvil). */
        private Duration inactividadMaxima = Duration.ofMinutes(2);
        /** Duración máxima absoluta de una sesión. */
        private Duration duracionMaxima = Duration.ofHours(12);
        /** Tiempo que tiene el otro dispositivo para aprobar una solicitud de cierre. */
        private Duration vigenciaSolicitud = Duration.ofMinutes(2);
    }

    @Getter
    @Setter
    public static class Login {
        private int maxIntentos = 5;
        private Duration bloqueo = Duration.ofHours(1);
    }

    @Getter
    @Setter
    public static class Cookie {
        private String nombre = "refresh_token";
        private String path = "/auth";
        private boolean secure = true;
        private String sameSite = "None";
    }

    @Getter
    @Setter
    public static class Codigo {
        /** Vida de un código enviado por correo. */
        private Duration vigencia = Duration.ofMinutes(3);
        /** Tiempo mínimo entre dos envíos al mismo usuario. */
        private Duration esperaReenvio = Duration.ofSeconds(60);
        private int maxReenvios = 3;
        /** Máximo de códigos por usuario y propósito en una hora. */
        private int maxPorHora = 5;
    }
}
