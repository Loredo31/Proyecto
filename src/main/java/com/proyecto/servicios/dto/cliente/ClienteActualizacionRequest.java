package com.proyecto.servicios.dto.cliente;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
public class ClienteActualizacionRequest {

    @Valid
    @NotNull(message = "Los datos personales son obligatorios")
    private DatosPersonalesActualizacionDto datosPersonales;

    @Valid
    @NotNull(message = "Los datos de contacto son obligatorios")
    private DatosContactoDto datosContacto;

    @Valid
    @NotNull(message = "El domicilio es obligatorio")
    private DomicilioDto domicilio;

    @Valid
    @NotNull(message = "La información laboral es obligatoria")
    private InformacionLaboralDto informacionLaboral;
}

