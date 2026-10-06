package com.proyecto.servicios.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Interceptor de Feign que inyecta el Bearer Token de autenticación
 * en las peticiones salientes hacia el catálogo externo.
 * El token se lee desde application.properties (catalog.external.token)
 * y nunca se hardcodea en el código fuente.
 */
@Slf4j
@Component
public class FeignAuthInterceptor implements RequestInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${catalog.external.token:}")
    private String token;

    @Override
    public void apply(RequestTemplate template) {
        // No interceptar las llamadas al servicio de autenticación de GestoPago
        if (template.feignTarget() != null && "gestoPagoAuth".equalsIgnoreCase(template.feignTarget().name())) {
            return;
        }

        if (token == null || token.isBlank()) {
            log.warn("El token de autenticación del catálogo (catalog.external.token) no se encuentra configurado");
            throw new IllegalStateException(
                    "El token de autenticación del catálogo no está configurado (catalog.external.token)");
        }

        log.debug("Inyectando Bearer Token de autorización en petición Feign");
        template.header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token);
    }
}

