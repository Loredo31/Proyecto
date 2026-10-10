package com.proyecto.servicios.dto.cliente;

import com.proyecto.servicios.enums.EstadoCuenta;
import jakarta.validation.constraints.DecimalMin;
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

    private EstadoCuenta estatus;

    @DecimalMin(value = "0.00", inclusive = true, message = "El saldo no puede ser negativo")
    private BigDecimal saldo;
}

