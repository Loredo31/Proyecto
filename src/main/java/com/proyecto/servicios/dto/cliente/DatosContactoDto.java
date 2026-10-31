package com.proyecto.servicios.dto.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class DatosContactoDto {

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Formato de correo electrónico inválido")
    @Size(max = 100, message = "El correo electrónico debe tener máximo 100 caracteres")
    private String correoElectronico;

    @NotNull(message = "El teléfono móvil es obligatorio")
    @Min(value = 1000000000L, message = "El teléfono móvil debe tener exactamente 10 dígitos")
    @Max(value = 9999999999L, message = "El teléfono móvil debe tener exactamente 10 dígitos")
    private Long telefonoMovil;

    @Min(value = 1000000000L, message = "El teléfono alternativo debe tener exactamente 10 dígitos")
    @Max(value = 9999999999L, message = "El teléfono alternativo debe tener exactamente 10 dígitos")
    private Long telefonoAlternativo;
}

