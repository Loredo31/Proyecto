package com.proyecto.servicios.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** La app móvil envía el refresh token en el cuerpo; la web lo envía en la cookie HttpOnly. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshRequest {

    @Size(max = 100, message = "El refresh token no es válido")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String refreshToken;
}
