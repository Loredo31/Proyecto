package com.proyecto.servicios.dto.cliente;

import jakarta.validation.Valid;
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
public class ClienteRegistroRequest {

    @Valid
    @NotNull(message = "Los datos personales son obligatorios")
    private DatosPersonalesDto datosPersonales;

    @Valid
    @NotNull(message = "Los datos de contacto son obligatorios")
    private DatosContactoDto datosContacto;

    @Valid
    @NotNull(message = "El domicilio es obligatorio")
    private DomicilioDto domicilio;

    @Valid
    @NotNull(message = "La información laboral es obligatoria")
    private InformacionLaboralDto informacionLaboral;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 50, message = "La contraseña debe tener entre 6 y 50 caracteres")
    private String password;
}

