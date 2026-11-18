package com.proyecto.servicios.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tokens emitidos al iniciar sesión o renovarla. En la plataforma WEB el refresh token
 * viaja solo en una cookie HttpOnly, por lo que {@code refreshToken} se omite del cuerpo.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponseDto {

    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiraEnSegundos;
    private String refreshToken;
    private Long refreshExpiraEnSegundos;
    private String correo;
    private Long clienteId;
}
