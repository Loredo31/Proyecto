package com.proyecto.servicios.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Envío de correos transaccionales con la API HTTP de Brevo (prefijo {@code correo}). */
@Getter
@Setter
@ConfigurationProperties(prefix = "correo")
public class CorreoProperties {

    private String brevoUrl = "https://api.brevo.com/v3";
    private String brevoApiKey;
    private String remitente;
    private String nombreRemitente = "UTNG";
    private Duration timeout = Duration.ofSeconds(10);

    public boolean configurado() {
        return brevoApiKey != null && !brevoApiKey.isBlank() && remitente != null && !remitente.isBlank();
    }

    @Override
    public String toString() {
        return "CorreoProperties[remitente=" + remitente + ", brevoApiKey=****]";
    }
}
