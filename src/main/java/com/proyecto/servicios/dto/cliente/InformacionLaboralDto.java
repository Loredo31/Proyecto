package com.proyecto.servicios.dto.cliente;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "La ocupación es obligatoria")
    @Size(max = 100, message = "La ocupación debe tener máximo 100 caracteres")
    @Pattern(regexp = "^[\\p{L}0-9\\s.,-]+$", message = "La ocupación no debe contener caracteres especiales")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    @Size(max = 100, message = "La empresa debe tener máximo 100 caracteres")
    @Pattern(regexp = "^[\\p{L}0-9\\s.,&'-]+$", message = "La empresa no debe contener caracteres especiales")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @PositiveOrZero(message = "El ingreso mensual debe ser mayor o igual a cero")
    private BigDecimal ingresoMensual;
}

