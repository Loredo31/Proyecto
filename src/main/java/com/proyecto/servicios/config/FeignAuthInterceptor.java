package com.proyecto.servicios.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import feign.RequestInterceptor;
import feign.RequestTemplate;

@Slf4j
@Component
public class FeignAuthInterceptor implements RequestInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${catalog.external.token:}")
    private String token;

    @Override
    public void apply(RequestTemplate template) {

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

