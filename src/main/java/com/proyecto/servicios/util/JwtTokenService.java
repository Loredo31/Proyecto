package com.proyecto.servicios.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.proyecto.servicios.config.SeguridadProperties;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.seguridad.UsuarioAutenticado;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Emite y valida los access tokens (JWT firmados con HMAC-SHA256).
 * El token incluye el id de la sesión ({@code sid}); su validez final la decide la sesión en el servidor.
 */
@Slf4j
@Service
public class JwtTokenService {

    private static final String EMISOR = "servicios-pagos";
    private static final String TIPO_ACCESS = "access";
    private static final String CLAIM_TIPO = "typ";
    private static final String CLAIM_USUARIO = "uid";
    private static final String CLAIM_CLIENTE = "cid";
    private static final String CLAIM_SESION = "sid";
    private static final int LONGITUD_SECRETO_ALEATORIO = 64;

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final Duration accessTtl;

    public JwtTokenService(@Value("${jwt.secret:}") String secret, SeguridadProperties properties) {
        String efectivo = secret;
        if (efectivo == null || efectivo.isBlank()) {
            log.warn("jwt.secret (JWT_SECRET) no está configurado; se generó un secreto aleatorio. "
                    + "Los tokens emitidos dejarán de ser válidos al reiniciar la aplicación");
            efectivo = generarSecretoAleatorio();
        }
        this.algorithm = Algorithm.HMAC256(efectivo);
        this.verifier = JWT.require(algorithm).withIssuer(EMISOR).withClaim(CLAIM_TIPO, TIPO_ACCESS).build();
        this.accessTtl = properties.getAccessTokenTtl();
    }

    private static String generarSecretoAleatorio() {
        byte[] bytes = new byte[LONGITUD_SECRETO_ALEATORIO];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public String generarAccessToken(Usuario usuario, String sesionId) {
        Instant ahora = Instant.now();
        Long clienteId = usuario.getCliente().getId();
        return JWT.create()
                .withIssuer(EMISOR)
                .withSubject(usuario.getCorreo())
                .withClaim(CLAIM_TIPO, TIPO_ACCESS)
                .withClaim(CLAIM_USUARIO, usuario.getId())
                .withClaim(CLAIM_CLIENTE, clienteId)
                .withClaim(CLAIM_SESION, sesionId)
                .withIssuedAt(ahora)
                .withExpiresAt(ahora.plus(accessTtl))
                .sign(algorithm);
    }

    /** Verifica firma, emisor, tipo y expiración. No consulta la sesión (eso lo hace el filtro). */
    public Optional<UsuarioAutenticado> validar(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            DecodedJWT jwt = verifier.verify(token);
            String sesionId = jwt.getClaim(CLAIM_SESION).asString();
            Long usuarioId = jwt.getClaim(CLAIM_USUARIO).asLong();
            Long clienteId = jwt.getClaim(CLAIM_CLIENTE).asLong();
            if (sesionId == null || usuarioId == null || clienteId == null) {
                return Optional.empty();
            }
            return Optional.of(new UsuarioAutenticado(usuarioId, clienteId, jwt.getSubject(), sesionId));
        } catch (JWTVerificationException ex) {
            return Optional.empty();
        }
    }

    public long getAccessTokenSegundos() {
        return accessTtl.toSeconds();
    }
}
