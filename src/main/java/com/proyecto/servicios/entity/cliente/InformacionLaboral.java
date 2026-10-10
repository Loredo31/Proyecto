package com.proyecto.servicios.entity.cliente;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InformacionLaboral {

    @Column(name = "laboral_ocupacion", nullable = false)
    private String ocupacion;

    @Column(name = "laboral_empresa", nullable = false)
    private String empresa;

    @Column(name = "laboral_ingreso_mensual", precision = 15, scale = 2, nullable = false)
    private BigDecimal ingresoMensual;
}

