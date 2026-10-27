package com.proyecto.servicios.util;

public final class DatosSensiblesUtil {

    private static final int DIGITOS_VISIBLES = 4;

    private DatosSensiblesUtil() {
    }

    /** "4152319999888877" -> "************8877". Pensado para logs y respuestas. */
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
