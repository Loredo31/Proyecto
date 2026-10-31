package com.proyecto.servicios.dto.cliente;

import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.EstadoRepublica;
import com.proyecto.servicios.enums.Sexo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    @NotNull(message = "El ID del cliente es obligatorio")
    @Positive(message = "El ID del cliente debe ser un número positivo")
    private Long id;

    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private Sexo sexo;
    private String nacionalidad;
    private EstadoCivil estadoCivil;
    private String correoElectronico;
    @Min(value = 1000000000L, message = "El teléfono móvil debe tener exactamente 10 dígitos")
    @Max(value = 9999999999L, message = "El teléfono móvil debe tener exactamente 10 dígitos")
    private Long telefonoMovil;
    @Min(value = 1000000000L, message = "El teléfono alternativo debe tener exactamente 10 dígitos")
    @Max(value = 9999999999L, message = "El teléfono alternativo debe tener exactamente 10 dígitos")
    private Long telefonoAlternativo;
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

