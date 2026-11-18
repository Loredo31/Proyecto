package com.proyecto.servicios.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Se envió un código al correo del usuario. El {@code ticket} identifica este proceso y debe enviarse
 * junto con el código; solo lo recibe el dispositivo que inició el proceso.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SegundoFactorDto {

    private String ticket;
    private String correoEnmascarado;
    private Long expiraEnSegundos;
}
