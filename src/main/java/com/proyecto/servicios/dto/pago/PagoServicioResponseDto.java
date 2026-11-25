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
    /** Número de autorización tal como lo responde GestoPago (en rechazos suele ser "-1"). */
    private String numeroAutorizacion;
    /** UPC generado para la petición sendTx. */
    private String upc;
    /** Fecha y hora (Date / horaLocal) que se envió en la petición sendTx, hora de México. */
    private LocalDateTime fechaEnvio;
    private String codigoCanje;
    private String leyenda;
    private String numCuenta;
    private Integer idServicio;
    private Integer idProducto;
    private String nombreProducto;
    /** Descripción del producto en el catálogo (solo en el comprobante y si el producto la tiene). */
    private String descripcionProducto;
    private String logoUrl;
    private String referencia;
    private BigDecimal montoPago;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
