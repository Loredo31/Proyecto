package com.proyecto.servicios.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Service
public class JwtTokenService {

    private static final long EXPIRATION_TIME_MS = 86400000L;
    private static final int LONGITUD_SECRETO_ALEATORIO = 64;
    private static final String BEARER = "Bearer ";

    private final Algorithm algorithm;

    public JwtTokenService(@Value("${jwt.secret:}") String secret) {
        String efectivo = secret;
        if (efectivo == null || efectivo.isBlank()) {
            log.warn("jwt.secret (JWT_SECRET) no está configurado; se generó un secreto aleatorio. "
                    + "Los tokens emitidos dejarán de ser válidos al reiniciar la aplicación");
            efectivo = generarSecretoAleatorio();
        }
        this.algorithm = Algorithm.HMAC256(efectivo);
    }

    private static String generarSecretoAleatorio() {
        byte[] bytes = new byte[LONGITUD_SECRETO_ALEATORIO];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public String generarToken(String correo, Long clienteId) {
        return JWT.create()
                .withSubject(correo)
                .withClaim("clienteId", clienteId)
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusMillis(EXPIRATION_TIME_MS))
                .sign(algorithm);
    }

    public boolean validarToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            JWT.require(algorithm).build().verify(token);
            return true;
        } catch (JWTVerificationException e) {
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

    public Long obtenerClienteIdDesdeHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER)) {
            return null;
        }
        return obtenerClienteId(authorizationHeader.substring(BEARER.length()));
    }

    public long getExpirationSeconds() {
        return EXPIRATION_TIME_MS / 1000;
    }
}
