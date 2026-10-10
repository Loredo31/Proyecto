package com.proyecto.servicios.dto.cliente;

import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.EstadoRepublica;
import com.proyecto.servicios.enums.Sexo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientePatchRequest {

    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private Sexo sexo;
    private String nacionalidad;
    private EstadoCivil estadoCivil;
    private String correoElectronico;
    private String telefonoMovil;
    private String telefonoAlternativo;
    private String calle;
    private Integer numeroExterior;
    private Integer numeroInterior;
    private String colonia;
    private String municipio;
    private EstadoRepublica estado;
    private String codigoPostal;
    private String pais;
    private String ocupacion;
    private String empresa;
    private BigDecimal ingresoMensual;
    private Boolean activoLogin;
}

