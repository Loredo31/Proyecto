package com.proyecto.servicios.dto.cliente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Petición de POST /clientes/correo/disponible (registro). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CorreoDisponibleRequest {

    @NotBlank(message = "El correo es obligatorio")
    @Size(max = 100, message = "El correo debe tener máximo 100 caracteres")
    private String correo;
}
