package com.proyecto.servicios.dto.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    @NotBlank(message = "El teléfono móvil es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono móvil solo debe contener exactamente 10 dígitos numéricos, sin letras ni caracteres especiales")
    private String telefonoMovil;

    @Pattern(regexp = "^$|^[0-9]{10}$", message = "El teléfono alternativo solo debe contener exactamente 10 dígitos numéricos, sin letras ni caracteres especiales")
    private String telefonoAlternativo;
}

