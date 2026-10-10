package com.proyecto.servicios.dto.cliente;

import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.Sexo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatosPersonalesDto {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "El nombre solo debe contener letras y espacios, sin números ni caracteres especiales")
    private String nombre;

    @Size(max = 50, message = "El segundo nombre debe tener máximo 50 caracteres")
    @Pattern(regexp = "^$|^[\\p{L}\\s]+$", message = "El segundo nombre solo debe contener letras y espacios, sin números ni caracteres especiales")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido paterno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "El apellido paterno solo debe contener letras y espacios, sin números ni caracteres especiales")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido materno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "El apellido materno solo debe contener letras y espacios, sin números ni caracteres especiales")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "La CURP es obligatoria")
    @Pattern(regexp = "^[A-Z]{4}[0-9]{6}[HM][A-Z]{2}[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9][0-9]$", message = "La CURP debe tener 18 caracteres alfanuméricos válidos, sin caracteres especiales")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Pattern(regexp = "^[A-Z&\\u00D1]{3,4}[0-9]{6}[A-Z0-9]{3}$", message = "El RFC debe tener 12 o 13 caracteres alfanuméricos válidos, sin caracteres especiales")
    private String rfc;

    @NotNull(message = "El sexo es obligatorio")
    private Sexo sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "La nacionalidad solo debe contener letras y espacios, sin números ni caracteres especiales")
    private String nacionalidad;

    @NotNull(message = "El estado civil es obligatorio")
    private EstadoCivil estadoCivil;
}

