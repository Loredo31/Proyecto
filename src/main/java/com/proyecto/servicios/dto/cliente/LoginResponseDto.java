package com.proyecto.servicios.dto.cliente;

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
public class LoginResponseDto {

    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private String correo;
    private Long clienteId;
    private Long expiraEnSegundos;
}

