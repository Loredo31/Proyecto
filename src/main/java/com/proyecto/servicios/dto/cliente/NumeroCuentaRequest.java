package com.proyecto.servicios.dto.cliente;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class NumeroCuentaRequest {

    @NotNull(message = "El número de cuenta es obligatorio")
    @Min(value = 1000000000000000L, message = "El número de cuenta debe tener 16 dígitos")
    @Max(value = 9999999999999999L, message = "El número de cuenta debe tener 16 dígitos")
    private Long numeroCuenta;
}
