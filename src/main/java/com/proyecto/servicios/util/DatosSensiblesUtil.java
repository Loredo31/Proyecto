package com.proyecto.servicios.util;

public final class DatosSensiblesUtil {

    private static final int DIGITOS_VISIBLES = 4;

    private DatosSensiblesUtil() {
    }

    public static String enmascarar(String valor) {
        if (valor == null) {
            return null;
        }
        if (valor.length() <= DIGITOS_VISIBLES) {
            return "*".repeat(valor.length());
        }
        return "*".repeat(valor.length() - DIGITOS_VISIBLES) + valor.substring(valor.length() - DIGITOS_VISIBLES);
    }
}
