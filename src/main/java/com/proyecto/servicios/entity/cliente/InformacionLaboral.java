package com.proyecto.servicios.entity.cliente;

import com.proyecto.servicios.enums.RangoIngreso;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    /** Opcional. */
    @Column(name = "laboral_ocupacion")
    private String ocupacion;

    /** Opcional. */
    @Column(name = "laboral_empresa")
    private String empresa;

    /** Ingreso exacto (clientes anteriores); ahora se captura el rango. */
    @Column(name = "laboral_ingreso_mensual", precision = 17, scale = 2)
    private BigDecimal ingresoMensual;

    @Enumerated(EnumType.STRING)
    @Column(name = "laboral_rango_ingreso", length = 30)
    private RangoIngreso rangoIngreso;
}

