package com.proyecto.servicios.util;

import com.proyecto.servicios.config.GestoPagoTxProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class GestoPagoAesCipher {

    private static final int IV_LENGTH = 16;

    private final GestoPagoTxProperties properties;

    public String cifrar(String textoPlano) {
        try {
            Cipher cipher = inicializar(Cipher.ENCRYPT_MODE);
            return Base64.getEncoder().encodeToString(cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No fue posible cifrar el payload de GestoPago", ex);
        }
    }

    public String descifrar(String textoCifradoBase64) {
        try {
            Cipher cipher = inicializar(Cipher.DECRYPT_MODE);
            return new String(cipher.doFinal(Base64.getDecoder().decode(textoCifradoBase64)), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No fue posible descifrar el payload de GestoPago", ex);
        }
    }

    private Cipher inicializar(int modo) throws GeneralSecurityException {
        GestoPagoTxProperties.Aes aes = properties.getAes();
        if (aes.getKey() == null || aes.getKey().isBlank()) {
            throw new IllegalStateException("La clave AES de GestoPago no está configurada (GESTOPAGO_AES_KEY)");
        }

        byte[] key = decodificar(aes.getKey(), aes.getEncoding());
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new IllegalStateException("La clave AES de GestoPago debe medir 16, 24 o 32 bytes");
        }

        Cipher cipher = Cipher.getInstance(aes.getTransformation());
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

        if (aes.getTransformation().toUpperCase(Locale.ROOT).contains("/ECB/")) {
            cipher.init(modo, keySpec);
            return cipher;
        }

        if (aes.getIv() == null || aes.getIv().isBlank()) {
            throw new IllegalStateException("El IV AES de GestoPago no está configurado (GESTOPAGO_AES_IV)");
        }
        byte[] iv = decodificar(aes.getIv(), aes.getEncoding());
        if (iv.length != IV_LENGTH) {
            throw new IllegalStateException("El IV AES de GestoPago debe medir 16 bytes");
        }
        cipher.init(modo, keySpec, new IvParameterSpec(iv));
        return cipher;
    }

    private static byte[] decodificar(String valor, String encoding) {
        return switch (encoding == null ? "RAW" : encoding.toUpperCase(Locale.ROOT)) {
            case "BASE64" -> Base64.getDecoder().decode(valor);
            case "HEX" -> HexFormat.of().parseHex(valor);
            default -> valor.getBytes(StandardCharsets.UTF_8);
        };
    }
}
