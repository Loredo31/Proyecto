package com.proyecto.servicios.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class RespuestaSolicitudRequest {

    @NotBlank(message = "El id de la solicitud es obligatorio")
    @Size(max = 36, message = "El id de la solicitud no es válido")
    private String solicitudId;

    @NotNull(message = "Indique si aprueba (true) o rechaza (false) el cierre de la sesión")
    private Boolean aprobar;
}
