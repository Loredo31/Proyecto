package com.proyecto.servicios.config;

import com.proyecto.servicios.seguridad.SesionWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final SesionWebSocketHandler sesionWebSocketHandler;
    private final SeguridadProperties properties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(sesionWebSocketHandler, "/ws/sesion")
                .setAllowedOriginPatterns(properties.getCorsOrigenes().toArray(String[]::new));
    }
}
