package com.proyecto.servicios.entity.seguridad;

import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.enums.EstadoSolicitudSesion;
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

import java.time.LocalDateTime;

/**
 * Petición de un dispositivo nuevo para iniciar sesión mientras otra sesión sigue activa.
 * El dispositivo con la sesión activa debe aprobarla antes de que se emitan los tokens nuevos.
 */
@Entity
@Table(name = "solicitudes_sesion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudSesion {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "sesion_actual_id", nullable = false, length = 36)
    private String sesionActualId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plataforma", nullable = false, length = 10)
    private Plataforma plataforma;

    @Column(name = "dispositivo", length = 150)
    private String dispositivo;

    /** SHA-256 del secreto entregado solo al dispositivo solicitante. */
    @Column(name = "secreto_hash", nullable = false, length = 64)
    private String secretoHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoSolicitudSesion estado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    public boolean estaVencida(LocalDateTime ahora) {
        return !ahora.isBefore(expiraEn);
    }
}
