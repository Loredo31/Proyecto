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

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificarCodigoRequest {

    @NotBlank(message = "El ticket es obligatorio")
    @Size(max = 100, message = "El ticket no es válido")
    private String ticket;

    @NotBlank(message = "El código es obligatorio")
    @Pattern(regexp = "[0-9]{6}", message = "El código debe tener 6 dígitos")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String codigo;
}
