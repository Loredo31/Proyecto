package com.proyecto.servicios.entity.cliente;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correo", length = 100, unique = true, nullable = false)
    private String correo;

    @Column(name = "password", nullable = false)
    private String password;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @OneToOne
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    @JsonBackReference
    private Cliente cliente;

    @Builder.Default
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Builder.Default
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion = LocalDateTime.now();

    public String getUsername() {
        return correo;
    }

    public void setUsername(String username) {
        this.correo = username;
    }
}

