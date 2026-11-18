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

    /** j***o@gmail.com: permite al usuario reconocer su correo sin exponerlo completo. */
    public static String enmascararCorreo(String correo) {
        if (correo == null) {
            return null;
        }
        int arroba = correo.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        String usuario = correo.substring(0, arroba);
        String visible = usuario.length() <= 2
                ? usuario.substring(0, 1) + "***"
                : usuario.charAt(0) + "***" + usuario.charAt(usuario.length() - 1);
        return visible + correo.substring(arroba);
    }
}
