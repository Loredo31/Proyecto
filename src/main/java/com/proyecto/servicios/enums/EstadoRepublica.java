package com.proyecto.servicios.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EstadoRepublica {
    AGUASCALIENTES("Aguascalientes", "20"),
    BAJA_CALIFORNIA("Baja California", "21", "22"),
    BAJA_CALIFORNIA_SUR("Baja California Sur", "23"),
    CAMPECHE("Campeche", "24"),
    COAHUILA("Coahuila", "25", "26", "27"),
    COLIMA("Colima", "28"),
    CHIAPAS("Chiapas", "29", "30"),
    CHIHUAHUA("Chihuahua", "31", "32", "33"),
    CIUDAD_DE_MEXICO("Ciudad de México", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16"),
    DURANGO("Durango", "34", "35"),
    GUANAJUATO("Guanajuato", "36", "37", "38"),
    GUERRERO("Guerrero", "39"),
    HIDALGO("Hidalgo", "40", "41", "42", "43"),
    JALISCO("Jalisco", "44", "45", "46", "47", "48", "49"),
    ESTADO_DE_MEXICO("Estado de México", "50", "51", "52", "53", "54", "55", "56", "57"),
    MICHOACAN("Michoacán", "58", "59", "60", "61"),
    MORELOS("Morelos", "62"),
    NAYARIT("Nayarit", "63"),
    NUEVO_LEON("Nuevo León", "64", "65", "66", "67"),
    OAXACA("Oaxaca", "68", "69", "70", "71"),
    PUEBLA("Puebla", "72", "73", "74", "75"),
    QUERETARO("Querétaro", "76"),
    QUINTANA_ROO("Quintana Roo", "77"),
    SAN_LUIS_POTOSI("San Luis Potosí", "78", "79"),
    SINALOA("Sinaloa", "80", "81", "82"),
    SONORA("Sonora", "83", "84", "85"),
    TABASCO("Tabasco", "86"),
    TAMAULIPAS("Tamaulipas", "87", "88", "89"),
    TLAXCALA("Tlaxcala", "90"),
    VERACRUZ("Veracruz", "91", "92", "93", "94", "95", "96"),
    YUCATAN("Yucatán", "97"),
    ZACATECAS("Zacatecas", "98", "99");

    private final String nombreOficial;
    private final String[] prefijosCp;

    EstadoRepublica(String nombreOficial, String... prefijosCp) {
        this.nombreOficial = nombreOficial;
        this.prefijosCp = prefijosCp;
    }

    public String getNombreOficial() {
        return nombreOficial;
    }

    public String[] getPrefijosCp() {
        return prefijosCp;
    }

    public boolean esCodigoPostalValido(String cp) {
        if (cp == null || cp.length() < 2) {
            return false;
        }
        String prefijo = cp.substring(0, 2);
        for (String p : prefijosCp) {
            if (p.equals(prefijo)) {
                return true;
            }
        }
        return false;
    }

    @JsonCreator
    public static EstadoRepublica fromString(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = normalizar(valor);
        for (EstadoRepublica e : values()) {
            if (normalizar(e.name()).equals(limpio) || normalizar(e.nombreOficial).equals(limpio)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Estado no válido: " + valor);
    }

    private static String normalizar(String texto) {
        return texto.toUpperCase()
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
                .replace("_", " ")
                .trim();
    }
}

