package com.proyecto.servicios.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generación de secretos aleatorios (refresh tokens, secretos de solicitud) y su huella SHA-256.
 * Solo la huella se guarda en base de datos; el valor en claro se entrega una única vez al cliente.
 */
public final class TokenSeguro {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int BYTES_TOKEN = 32;

    private TokenSeguro() {
    }

    public static String generar() {
        byte[] bytes = new byte[BYTES_TOKEN];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String huella(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("El valor a resumir no puede ser nulo");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    /** Comparación en tiempo constante para evitar ataques de temporización. */
    public static boolean coincide(String valorEnClaro, String huellaGuardada) {
        if (valorEnClaro == null || huellaGuardada == null) {
            return false;
        }
        return MessageDigest.isEqual(
                huella(valorEnClaro).getBytes(StandardCharsets.UTF_8),
                huellaGuardada.getBytes(StandardCharsets.UTF_8));
    }
}
