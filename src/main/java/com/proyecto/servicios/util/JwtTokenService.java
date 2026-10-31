package com.proyecto.servicios.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtTokenService {

    private static final String DEFAULT_SECRET = "BancoFintechSeguridadTokenClaveUltraSecreta2026";
    private static final long EXPIRATION_TIME_MS = 86400000L;

    @Value("${jwt.secret:BancoFintechSeguridadTokenClaveUltraSecreta2026}")
    private String secret;

    public String generarToken(String correo, Long clienteId) {
        String effectiveSecret = (secret != null && !secret.isBlank()) ? secret : DEFAULT_SECRET;
        Algorithm algorithm = Algorithm.HMAC256(effectiveSecret);

        return JWT.create()
                .withSubject(correo)
                .withClaim("clienteId", clienteId)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME_MS))
                .sign(algorithm);
    }

    public boolean validarToken(String token) {
        try {
            String effectiveSecret = (secret != null && !secret.isBlank()) ? secret : DEFAULT_SECRET;
            Algorithm algorithm = Algorithm.HMAC256(effectiveSecret);
            JWT.require(algorithm).build().verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String obtenerCorreo(String token) {
        DecodedJWT decoded = JWT.decode(token);
        return decoded.getSubject();
    }

    public Long obtenerClienteId(String token) {
        DecodedJWT decoded = JWT.decode(token);
        return decoded.getClaim("clienteId").asLong();
    }

    /**
     * Extrae el clienteId de un header "Authorization: Bearer ...". El token ya fue validado por
     * {@code JwtAuthInterceptor}; aquí solo se decodifica.
     */
    public Long obtenerClienteIdDesdeHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return obtenerClienteId(authorizationHeader.substring(7));
    }

    public long getExpirationSeconds() {
        return EXPIRATION_TIME_MS / 1000;
    }
}

