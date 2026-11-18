package com.proyecto.servicios.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proyecto.servicios.enums.EstadoSolicitudSesion;
import com.proyecto.servicios.enums.Plataforma;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Estado de una solicitud para cerrar la sesión activa en otro dispositivo.
 * El {@code secreto} solo se entrega una vez al dispositivo que la creó y se usa para consultar su estado.
 * Cuando el otro dispositivo la aprueba, la consulta devuelve los tokens en {@code login}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SolicitudSesionDto {

    private String solicitudId;
    private String secreto;
    private EstadoSolicitudSesion estado;
    private Long expiraEnSegundos;
    private Plataforma plataformaSesionActiva;
    private String dispositivoSesionActiva;
    private LoginResponseDto login;
}
