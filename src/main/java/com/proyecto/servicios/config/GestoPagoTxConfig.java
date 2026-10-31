package com.proyecto.servicios.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GestoPagoTxProperties.class)
public class GestoPagoTxConfig {
}
