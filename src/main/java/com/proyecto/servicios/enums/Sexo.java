package com.proyecto.servicios.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Sexo {
    MASCULINO,
    FEMENINO,
    OTRO;

    @JsonCreator
    public static Sexo fromString(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.toUpperCase().trim();
        if (limpio.equals("M")) return MASCULINO;
        if (limpio.equals("F")) return FEMENINO;
        for (Sexo s : values()) {
            if (s.name().equals(limpio)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Sexo no válido: " + valor);
    }
}

