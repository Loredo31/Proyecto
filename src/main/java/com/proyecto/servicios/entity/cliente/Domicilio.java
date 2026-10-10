package com.proyecto.servicios.entity.cliente;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.proyecto.servicios.enums.EstadoRepublica;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "domicilios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Domicilio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    @JsonBackReference
    private Cliente cliente;

    @Column(name = "calle", nullable = false)
    private String calle;

    @Column(name = "num_exterior", nullable = false)
    private Integer numeroExterior;

    @Column(name = "num_interior")
    private Integer numeroInterior;

    @Column(name = "colonia", nullable = false)
    private String colonia;

    @Column(name = "municipio", nullable = false)
    private String municipio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoRepublica estado;

    @Column(name = "codigo_postal", length = 5, nullable = false)
    private String codigoPostal;

    @Column(name = "pais", nullable = false)
    private String pais;
}

