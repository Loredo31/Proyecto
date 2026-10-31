package com.proyecto.servicios.dto.pago;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proyecto.servicios.enums.EstadoTransaccion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagoServicioResponseDto {

    private String folio;
    private EstadoTransaccion estado;
    private String codigoRespuesta;
    private String mensajeRespuesta;
    private String numeroAutorizacion;
    private String upc;
    private String codigoCanje;
    private String leyenda;
    private String numCuenta;
    private Integer idServicio;
    private Integer idProducto;
    private String referencia;
    private BigDecimal montoPago;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
