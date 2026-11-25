package com.proyecto.servicios.enums;

import java.math.BigDecimal;

/** Rango de ingreso mensual que elige el cliente en el registro (lista en los frontends). */
public enum RangoIngreso {
    HASTA_5000("5,000 o menos"),
    DE_5000_A_10000("5,000 - 10,000"),
    DE_10000_A_20000("10,000 - 20,000"),
    DE_20000_A_30000("20,000 - 30,000"),
    MAS_DE_30000("30,000 o más");

    private final String texto;

    RangoIngreso(String texto) {
        this.texto = texto;
    }

    public String getTexto() {
        return texto;
    }

    /** Rango que corresponde a un ingreso exacto (clientes registrados antes de usar la lista). */
    public static RangoIngreso desde(BigDecimal ingreso) {
        if (ingreso == null) {
            return null;
        }
        if (ingreso.compareTo(BigDecimal.valueOf(5_000)) <= 0) return HASTA_5000;
        if (ingreso.compareTo(BigDecimal.valueOf(10_000)) <= 0) return DE_5000_A_10000;
        if (ingreso.compareTo(BigDecimal.valueOf(20_000)) <= 0) return DE_10000_A_20000;
        if (ingreso.compareTo(BigDecimal.valueOf(30_000)) <= 0) return DE_20000_A_30000;
        return MAS_DE_30000;
    }
}
