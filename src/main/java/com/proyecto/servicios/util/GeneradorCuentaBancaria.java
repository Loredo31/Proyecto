package com.proyecto.servicios.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class GeneradorCuentaBancaria {

    private static final String BIN_PREFIJO = "415231";
    private final SecureRandom random = new SecureRandom();

    public String generarNumeroCuenta() {
        StringBuilder builder = new StringBuilder(BIN_PREFIJO);
        for (int i = 0; i < 9; i++) {
            builder.append(random.nextInt(10));
        }
        int checkDigit = calcularDigitoLuhn(builder.toString());
        builder.append(checkDigit);
        return builder.toString();
    }

    public static boolean esValidoLuhn(String numero) {
        if (numero == null || numero.length() != 16) {
            return false;
        }
        int nDigits = numero.length();
        int nSum = 0;
        boolean isSecond = false;
        for (int i = nDigits - 1; i >= 0; i--) {
            int d = numero.charAt(i) - '0';
            if (d < 0 || d > 9) {
                return false;
            }
            if (isSecond) {
                d = d * 2;
            }
            nSum += d / 10;
            nSum += d % 10;
            isSecond = !isSecond;
        }
        return (nSum % 10 == 0);
    }

    private int calcularDigitoLuhn(String base15) {
        int nSum = 0;
        boolean isSecond = true;
        for (int i = base15.length() - 1; i >= 0; i--) {
            int d = base15.charAt(i) - '0';
            if (isSecond) {
                d = d * 2;
            }
            nSum += d / 10;
            nSum += d % 10;
            isSecond = !isSecond;
        }
        return (10 - (nSum % 10)) % 10;
    }
}

