package com.proyecto.servicios.dto.cliente;

import com.proyecto.servicios.enums.EstadoRepublica;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomicilioDto {

    @NotBlank(message = "La calle es obligatoria")
    @Size(max = 150, message = "La calle debe tener máximo 150 caracteres")
    @Pattern(regexp = "^[\\p{L}0-9\\s.,#-]+$", message = "La calle no debe contener caracteres especiales inválidos")
    private String calle;

    @NotNull(message = "El número exterior es obligatorio")
    private Integer numeroExterior;

    private Integer numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    @Size(max = 100, message = "La colonia debe tener máximo 100 caracteres")
    @Pattern(regexp = "^[\\p{L}0-9\\s.,#-]+$", message = "La colonia no debe contener caracteres especiales inválidos")
    private String colonia;

    @NotBlank(message = "El municipio es obligatorio")
    @Size(max = 100, message = "El municipio debe tener máximo 100 caracteres")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "El municipio solo debe contener letras y espacios")
    private String municipio;

    @NotNull(message = "El estado de la república es obligatorio")
    private EstadoRepublica estado;

    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "^[0-9]{5}$", message = "El código postal debe contener exactamente 5 dígitos numéricos")
    private String codigoPostal;

    @NotBlank(message = "El país es obligatorio")
    @Size(max = 50, message = "El país debe tener máximo 50 caracteres")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "El país solo debe contener letras y espacios")
    private String pais;
}

