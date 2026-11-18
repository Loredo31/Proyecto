package com.proyecto.servicios.dto.cliente;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "El número de cuenta es obligatorio")
    @Min(value = 1000000000000000L, message = "El número de cuenta debe tener 16 dígitos")
    @Max(value = 9999999999999999L, message = "El número de cuenta debe tener 16 dígitos")
    private Long numeroCuenta;

    /** PIN vigente, tomado del header X-Auth-Pin (solo si la cuenta ya tiene PIN). */
    @JsonIgnore
    private String pinActual;

    @NotBlank(message = "El nuevo PIN es obligatorio")
    @Pattern(regexp = "\\d{4,6}", message = "El nuevo PIN debe tener entre 4 y 6 dígitos")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String pinNuevo;
}
