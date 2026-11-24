package com.proyecto.servicios.entity.gestopago;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Producto del catálogo de GestoPago guardado en PostgreSQL (tabla {@code catalogo_productos}). */
@Entity
@Table(name = "catalogo_productos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogoProducto {

    @Id
    @Column(name = "id_producto")
    private Integer idProducto;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "servicio", length = 150)
    private String servicio;

    @Column(name = "nombre_producto", nullable = false)
    private String nombreProducto;

    @Column(name = "id_cat_tipo_servicio")
    private Integer idCatTipoServicio;

    @Column(name = "tipo_front")
    private Integer tipoFront;

    @Column(name = "precio", precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "tipo_referencia", length = 10)
    private String tipoReferencia;

    @Column(name = "legend", columnDefinition = "TEXT")
    private String legend;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = Boolean.TRUE;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;
}
