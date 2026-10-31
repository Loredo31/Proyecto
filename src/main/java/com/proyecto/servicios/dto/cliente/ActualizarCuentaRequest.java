package com.proyecto.servicios.dto.cliente;

import com.proyecto.servicios.enums.EstadoCuenta;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarCuentaRequest {

    @NotNull(message = "El número de cuenta es obligatorio")
    @Min(value = 1000000000000000L, message = "El número de cuenta debe tener 16 dígitos")
    @Max(value = 9999999999999999L, message = "El número de cuenta debe tener 16 dígitos")
    private Long numeroCuenta;

    private EstadoCuenta estatus;

    @DecimalMin(value = "0.00", inclusive = true, message = "El saldo no puede ser negativo")
    private BigDecimal saldo;
}

