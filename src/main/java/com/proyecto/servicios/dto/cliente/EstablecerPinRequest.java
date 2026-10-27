package com.proyecto.servicios.dto.cliente;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class EstablecerPinRequest {

    /** Obligatorio solo si la cuenta ya tiene un PIN configurado. */
    @Pattern(regexp = "\\d{4,6}", message = "El PIN actual debe tener entre 4 y 6 dígitos")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String pinActual;

    @NotBlank(message = "El nuevo PIN es obligatorio")
    @Pattern(regexp = "\\d{4,6}", message = "El nuevo PIN debe tener entre 4 y 6 dígitos")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String pinNuevo;
}
