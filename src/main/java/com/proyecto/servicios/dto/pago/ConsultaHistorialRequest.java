package com.proyecto.servicios.dto.pago;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class ConsultaHistorialRequest {

    @Builder.Default
    @Min(value = 0, message = "La página no puede ser negativa")
    private Integer pagina = 0;

    @Builder.Default
    @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
    @Max(value = 50, message = "El tamaño de página no puede exceder 50")
    private Integer tamano = 20;
}
