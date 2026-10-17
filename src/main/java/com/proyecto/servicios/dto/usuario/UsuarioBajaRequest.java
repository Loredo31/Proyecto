package com.proyecto.servicios.dto.usuario;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class UsuarioBajaRequest {

    @NotNull(message = "El ID del usuario es obligatorio para la baja lógica")
    @Positive(message = "El ID del usuario debe ser un número positivo")
    private Long id;
}
