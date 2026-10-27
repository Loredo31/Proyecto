package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Payload en claro de sendTx.do / confirmTx.do. Se serializa a JSON y se cifra con AES-256 antes de
 * enviarse; nunca debe registrarse en logs.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"idProducto", "idServicio", "telefono", "horaLocal", "referencia", "montoPago", "upc", "unidad"})
public class SendTxPayload {

    private Integer idProducto;
    private Integer idServicio;
    /** Teléfono en recargas; "1111111111" en pago de servicios; "#1111111111" en gift cards. */
    private String telefono;
    /** yyyyMMddHHmmss */
    private String horaLocal;
    /** Solo en pago de servicios. */
    private String referencia;
    /** Solo en pago de servicios. */
    private BigDecimal montoPago;
    private String upc;
    private String unidad;
}
