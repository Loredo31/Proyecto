package com.proyecto.servicios.entity.cliente;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.Sexo;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", length = 50, nullable = false)
    private String nombre;

    @Column(name = "segundo_nombre", length = 50)
    private String segundoNombre;

    @Column(name = "apellido_paterno", length = 50, nullable = false)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", length = 50, nullable = false)
    private String apellidoMaterno;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "curp", length = 18, unique = true, nullable = false)
    private String curp;

    @Column(name = "rfc", length = 13, unique = true, nullable = false)
    private String rfc;

    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", nullable = false)
    private Sexo sexo;

    @Column(name = "nacionalidad", length = 50, nullable = false)
    private String nacionalidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_civil", nullable = false)
    private EstadoCivil estadoCivil;

    @Column(name = "correo_electronico", length = 100, unique = true, nullable = false)
    private String correoElectronico;

    @Column(name = "telefono_movil", length = 10, unique = true, nullable = false)
    private String telefonoMovil;

    @Column(name = "telefono_alternativo", length = 10)
    private String telefonoAlternativo;

    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Domicilio domicilio;

    @Embedded
    private InformacionLaboral informacionLaboral;

    @Embedded
    @Builder.Default
    private BiometriaInfo biometriaInfo = new BiometriaInfo();

    @Builder.Default
    @Column(name = "activo_login", nullable = false)
    private Boolean activoLogin = true;

    @Builder.Default
    @Column(name = "eliminado_logico", nullable = false)
    private Boolean eliminadoLogico = false;

    @Builder.Default
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<CuentaBancaria> cuentas = new ArrayList<>();

    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Usuario usuario;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    public void prePersist() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
        if (this.activoLogin == null) this.activoLogin = true;
        if (this.eliminadoLogico == null) this.eliminadoLogico = false;
        if (this.biometriaInfo == null) this.biometriaInfo = new BiometriaInfo();
        if (this.cuentas == null) this.cuentas = new ArrayList<>();
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}

