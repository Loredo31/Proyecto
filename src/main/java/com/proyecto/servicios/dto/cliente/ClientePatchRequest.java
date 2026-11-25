package com.proyecto.servicios.dto.cliente;

import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.EstadoRepublica;
import com.proyecto.servicios.enums.Sexo;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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

    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "El nombre solo debe contener letras y espacios")
    private String nombre;
    @Size(max = 50, message = "El segundo nombre debe tener máximo 50 caracteres")
    @Pattern(regexp = "^$|^[\\p{L}\\s]+$", message = "El segundo nombre solo debe contener letras y espacios")
    private String segundoNombre;
    @Size(min = 2, max = 50, message = "El apellido paterno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "El apellido paterno solo debe contener letras y espacios")
    private String apellidoPaterno;
    @Size(min = 2, max = 50, message = "El apellido materno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "El apellido materno solo debe contener letras y espacios")
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
    @Size(min = 3, max = 150, message = "La calle debe tener entre 3 y 150 caracteres")
    @Pattern(regexp = "^[\\p{L}0-9\\s.,#-]+$", message = "La calle no debe contener caracteres especiales inválidos")
    private String calle;
    private Integer numeroExterior;
    private Integer numeroInterior;
    @Size(min = 3, max = 100, message = "La colonia debe tener entre 3 y 100 caracteres")
    @Pattern(regexp = "^[\\p{L}0-9\\s.,#-]+$", message = "La colonia no debe contener caracteres especiales inválidos")
    private String colonia;
    @Size(max = 100, message = "El municipio debe tener máximo 100 caracteres")
    @Pattern(regexp = "^[\\p{L}0-9\\s.,]+$", message = "El municipio solo debe contener letras, números, espacios, puntos y comas")
    private String municipio;
    private EstadoRepublica estado;
    private String codigoPostal;
    private String pais;
    @Size(min = 2, max = 100, message = "La ocupación debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[\\p{L}0-9\\s.,-]+$", message = "La ocupación no debe contener caracteres especiales")
    private String ocupacion;
    @Size(min = 2, max = 100, message = "La empresa debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[\\p{L}0-9\\s.,&'-]+$", message = "La empresa no debe contener caracteres especiales")
    private String empresa;
    @Digits(integer = 15, fraction = 2, message = "El ingreso mensual admite máximo 15 dígitos enteros y 2 decimales")
    private BigDecimal ingresoMensual;
    private Boolean activoLogin;
}

