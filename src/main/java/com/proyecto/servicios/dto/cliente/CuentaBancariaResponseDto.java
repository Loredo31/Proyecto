package com.proyecto.servicios.dto.cliente;

import com.proyecto.servicios.enums.EstadoCuenta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaBancariaResponseDto {

    private String numeroCuenta;
    private BigDecimal saldo;
    private EstadoCuenta estado;
    private LocalDateTime fechaCreacion;
}

