package com.proyecto.servicios.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EstadoCivil {
    SOLTERO,
    CASADO,
    DIVORCIADO,
    VIUDO,
    UNION_LIBRE;

    @JsonCreator
    public static EstadoCivil fromString(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.toUpperCase().replace(" ", "_").trim();
        for (EstadoCivil e : values()) {
            if (e.name().equals(limpio)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Estado civil no válido: " + valor);
    }
}

