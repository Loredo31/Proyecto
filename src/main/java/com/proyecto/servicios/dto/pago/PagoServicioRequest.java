package com.proyecto.servicios.dto.pago;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoServicioRequest {

    private static final String PATRON_TELEFONO = "\\d{10}";
    private static final String PATRON_REFERENCIA = "[A-Za-z0-9-]{1,100}";

    @NotNull(message = "El número de cuenta es obligatorio")
    @Min(value = 1000000000000000L, message = "El número de cuenta debe tener 16 dígitos")
    @Max(value = 9999999999999999L, message = "El número de cuenta debe tener 16 dígitos")
    private Long numCuenta;

    @NotNull(message = "El idServicio es obligatorio")
    @Positive(message = "El idServicio debe ser positivo")
    private Integer idServicio;

    @NotNull(message = "El idProducto es obligatorio")
    @Positive(message = "El idProducto debe ser positivo")
    private Integer idProducto;

    @NotNull(message = "El monto de pago es obligatorio")
    @Positive(message = "El monto de pago debe ser mayor a cero")
    @Digits(integer = 13, fraction = 2, message = "El monto admite máximo 2 decimales")
    @JsonAlias("precio")
    private BigDecimal montoPago;

    @NotNull(message = "El idDistribuidor es obligatorio")
    @Positive(message = "El idDistribuidor debe ser positivo")
    private Integer idDistribuidor;

    @NotBlank(message = "El teléfono o referencia es obligatorio")
    @Size(max = 100, message = "La referencia admite máximo 100 caracteres")
    @JsonAlias("telefono")
    private String referencia;

    @NotNull(message = "La bandera (con/sin referencia) es obligatoria")
    private Boolean bandera;

    @NotBlank(message = "El PIN de seguridad es obligatorio")
    @Pattern(regexp = "\\d{4,6}", message = "El PIN debe tener entre 4 y 6 dígitos")
    @JsonAlias("passSeguro")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String pin;

    @Size(min = 1, max = 64, message = "La idempotencyKey debe tener entre 1 y 64 caracteres")
    private String idempotencyKey;

    @JsonIgnore
    @AssertTrue(message = "Con bandera=false la referencia debe ser un teléfono de 10 dígitos; "
            + "con bandera=true debe ser alfanumérica (máx. 100)")
    public boolean isReferenciaValidaParaTipoDePago() {
        if (bandera == null || referencia == null) {
            return true;
        }
        return referencia.matches(Boolean.TRUE.equals(bandera) ? PATRON_REFERENCIA : PATRON_TELEFONO);
    }

    @JsonIgnore
    public boolean esConReferencia() {
        return Boolean.TRUE.equals(bandera);
    }
}
