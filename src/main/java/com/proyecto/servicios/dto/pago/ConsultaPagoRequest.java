package com.proyecto.servicios.dto.pago;

import jakarta.validation.constraints.NotBlank;
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
public class ConsultaPagoRequest {

    @NotBlank(message = "El folio es obligatorio")
    private String folio;
}
