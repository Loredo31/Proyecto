package com.proyecto.servicios.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cierra la sesión del otro dispositivo confirmando con el código enviado por correo. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmarCierreCorreoRequest {

    @NotBlank(message = "El id de la solicitud es obligatorio")
    @Size(max = 36, message = "El id de la solicitud no es válido")
    private String solicitudId;

    @NotBlank(message = "El secreto de la solicitud es obligatorio")
    @Size(max = 100, message = "El secreto de la solicitud no es válido")
    private String secreto;

    @NotBlank(message = "El ticket es obligatorio")
    @Size(max = 100, message = "El ticket no es válido")
    private String ticket;

    @NotBlank(message = "El código es obligatorio")
    @Pattern(regexp = "[0-9]{6}", message = "El código debe tener 6 dígitos")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String codigo;
}
