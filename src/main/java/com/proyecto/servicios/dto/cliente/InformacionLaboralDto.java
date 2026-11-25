package com.proyecto.servicios.dto.cliente;

import jakarta.validation.constraints.Digits;
import com.proyecto.servicios.enums.RangoIngreso;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
public class InformacionLaboralDto {

    /** Opcional: vacío o de 2 a 100 caracteres. */
    @Size(max = 100, message = "La ocupación debe tener máximo 100 caracteres")
    @Pattern(regexp = "^$|^[\\p{L}0-9\\s.,-]{2,100}$", message = "La ocupación debe tener al menos 2 caracteres, sin caracteres especiales")
    private String ocupacion;

    /** Opcional: vacío o de 2 a 100 caracteres. */
    @Size(max = 100, message = "La empresa debe tener máximo 100 caracteres")
    @Pattern(regexp = "^$|^[\\p{L}0-9\\s.,&'-]{2,100}$", message = "La empresa debe tener al menos 2 caracteres, sin caracteres especiales")
    private String empresa;

    /** Opcional (clientes anteriores). El registro usa rangoIngreso. */
    @PositiveOrZero(message = "El ingreso mensual debe ser mayor o igual a cero")
    @Digits(integer = 15, fraction = 2, message = "El ingreso mensual admite máximo 15 dígitos enteros y 2 decimales")
    private BigDecimal ingresoMensual;

    @NotNull(message = "El ingreso mensual es obligatorio")
    private RangoIngreso rangoIngreso;
}

