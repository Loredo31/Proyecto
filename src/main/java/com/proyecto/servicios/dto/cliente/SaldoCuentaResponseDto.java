package com.proyecto.servicios.dto.cliente;

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
public class SaldoCuentaResponseDto {
    private String numeroCuenta;
    private BigDecimal saldo;
}

