package com.proyecto.servicios.entity.seguridad;

import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.enums.EstadoSesion;
import com.proyecto.servicios.enums.Plataforma;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Sesión de acceso respaldada por el servidor. Solo puede existir una ACTIVA por usuario;
 * el access token (JWT) lleva el id de la sesión y se rechaza si la sesión deja de estar activa.
 */
@Entity
@Table(name = "sesiones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sesion {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "plataforma", nullable = false, length = 10)
    private Plataforma plataforma;

    @Column(name = "dispositivo", length = 150)
    private String dispositivo;

    /** SHA-256 del refresh token vigente; el token en claro nunca se guarda. */
    @Column(name = "refresh_hash", nullable = false, unique = true, length = 64)
    private String refreshHash;

    /** SHA-256 del refresh token anterior, para detectar su reutilización (posible robo). */
    @Column(name = "refresh_hash_anterior", length = 64)
    private String refreshHashAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoSesion estado;

    @Column(name = "motivo_cierre", length = 150)
    private String motivoCierre;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "ultima_actividad", nullable = false)
    private LocalDateTime ultimaActividad;

    /** Límite absoluto de la sesión, sin importar la actividad. */
    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    public boolean estaVigente(LocalDateTime ahora, Duration inactividadMaxima) {
        return estado == EstadoSesion.ACTIVA
                && ahora.isBefore(expiraEn)
                && ahora.isBefore(ultimaActividad.plus(inactividadMaxima));
    }

    public void cerrar(EstadoSesion estadoFinal, String motivo, LocalDateTime ahora) {
        this.estado = estadoFinal;
        this.motivoCierre = motivo;
        this.fechaCierre = ahora;
    }
}
