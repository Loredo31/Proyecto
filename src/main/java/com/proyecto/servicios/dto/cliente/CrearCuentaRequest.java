package com.proyecto.servicios.dto.cliente;

import jakarta.validation.constraints.DecimalMin;
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
public class CrearCuentaRequest {

    @NotNull(message = "El ID del cliente es obligatorio")
    private Long clienteId;

    @Builder.Default
    @DecimalMin(value = "0.00", inclusive = true, message = "El saldo inicial no puede ser negativo")
    private BigDecimal saldoInicial = BigDecimal.ZERO;
}

