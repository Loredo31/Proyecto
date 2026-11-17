package com.proyecto.servicios.util;

/**
 * Reglas de complejidad de contraseña: al menos 8 caracteres, una minúscula,
 * una mayúscula, un dígito y un carácter especial. Se implementa sin expresiones
 * regulares para evitar backtracking excesivo.
 */
public final class ValidadorContrasena {

    public static final int LONGITUD_MINIMA = 8;
    public static final String MENSAJE_REGLAS =
            "La contraseña debe contener al menos 8 caracteres, una letra mayúscula, una minúscula, un número y un carácter especial";

    private static final String ESPECIALES = "!@#$%^&*()_+-=[]{};':\"\\|,.<>/?";

    private ValidadorContrasena() {
    }

    public static boolean cumpleReglas(String valor) {
        if (valor == null || valor.length() < LONGITUD_MINIMA) {
            return false;
        }
        boolean minuscula = false;
        boolean mayuscula = false;
        boolean digito = false;
        boolean especial = false;
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (c == '\n' || c == '\r') {
                return false;
            }
            if (c >= 'a' && c <= 'z') {
                minuscula = true;
            } else if (c >= 'A' && c <= 'Z') {
                mayuscula = true;
            } else if (c >= '0' && c <= '9') {
                digito = true;
            } else if (ESPECIALES.indexOf(c) >= 0) {
                especial = true;
            }
        }
        return minuscula && mayuscula && digito && especial;
    }
}
