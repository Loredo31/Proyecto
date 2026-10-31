package com.proyecto.servicios.entity.pago;

import com.proyecto.servicios.enums.EstadoTransaccion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transacciones_pago")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador público (UUID) expuesto al cliente; el id interno nunca sale de la API. */
    @Column(name = "folio", length = 36, nullable = false, unique = true, updatable = false)
    private String folio;

    @Column(name = "idempotency_key", length = 64, unique = true, updatable = false)
    private String idempotencyKey;

    @Column(name = "cuenta_id", nullable = false, updatable = false)
    private Long cuentaId;

    @Column(name = "numero_cuenta", length = 16, nullable = false, updatable = false)
    private String numeroCuenta;

    @Column(name = "cliente_id", nullable = false, updatable = false)
    private Long clienteId;

    @Column(name = "id_servicio", nullable = false, updatable = false)
    private Integer idServicio;

    @Column(name = "id_producto", nullable = false, updatable = false)
    private Integer idProducto;

    @Column(name = "referencia", length = 100, nullable = false, updatable = false)
    private String referencia;

    @Column(name = "con_referencia", nullable = false, updatable = false)
    private Boolean conReferencia;

    @Column(name = "monto", precision = 15, scale = 2, nullable = false, updatable = false)
    private BigDecimal monto;

    // Parámetros exactos de sendTx: confirmTx exige reenviar los mismos (AES con IV fijo es determinista).

    /** Unique Payment Code: identifica la operación ante GestoPago y evita duplicados de su lado. */
    @Column(name = "upc", length = 15, nullable = false, unique = true, updatable = false)
    private String upc;

    @Column(name = "hora_local", length = 14, updatable = false)
    private String horaLocal;

    @Column(name = "telefono_envio", length = 20, updatable = false)
    private String telefonoEnvio;

    @Column(name = "unidad", length = 25, updatable = false)
    private String unidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 30, nullable = false)
    private EstadoTransaccion estado;

    @Column(name = "codigo_respuesta", length = 20)
    private String codigoRespuesta;

    @Column(name = "mensaje_respuesta", length = 500)
    private String mensajeRespuesta;

    @Column(name = "numero_autorizacion", length = 100)
    private String numeroAutorizacion;

    @Column(name = "id_tx_proveedor", length = 30)
    private String idTxProveedor;

    /** PIN o folio de canje de gift cards (puede ser una URL de descarga). */
    @Column(name = "codigo_canje", length = 250)
    private String codigoCanje;

    @Column(name = "leyenda", columnDefinition = "TEXT")
    private String leyenda;

    /** Evidencia: cuerpo crudo de la respuesta de sendTx.do. */
    @Column(name = "respuesta_proveedor", columnDefinition = "TEXT")
    private String respuestaProveedor;

    /** Evidencia: cuerpo crudo de la última respuesta de confirmTx.do. */
    @Column(name = "respuesta_confirmacion", columnDefinition = "TEXT")
    private String respuestaConfirmacion;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = fechaCreacion;
    }

    @PreUpdate
    void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
