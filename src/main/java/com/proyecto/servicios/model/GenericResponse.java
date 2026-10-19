package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericResponse<T> {

    private Integer codigo;
    private String mensaje;
    private T datos;

    public GenericResponse(Integer codigo, String mensaje) {
        this.codigo = codigo;
        this.mensaje = mensaje;
    }

    public static <T> GenericResponse<T> exito(String mensaje, T datos) {
        return GenericResponse.<T>builder()
                .codigo(0)
                .mensaje(mensaje)
                .datos(datos)
                .build();
    }

    public static <T> GenericResponse<T> exito(String mensaje) {
        return GenericResponse.<T>builder()
                .codigo(0)
                .mensaje(mensaje)
                .build();
    }

    public static <T> GenericResponse<T> error(Integer codigo, String mensaje) {
        return GenericResponse.<T>builder()
                .codigo(codigo)
                .mensaje(mensaje)
                .build();
    }

    public static <T> GenericResponse<T> error(Integer codigo, String mensaje, T datos) {
        return GenericResponse.<T>builder()
                .codigo(codigo)
                .mensaje(mensaje)
                .datos(datos)
                .build();
    }
}
