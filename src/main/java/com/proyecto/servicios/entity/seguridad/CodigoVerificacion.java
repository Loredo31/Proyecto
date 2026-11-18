package com.proyecto.servicios.entity.seguridad;

import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.enums.EstadoCodigo;
import com.proyecto.servicios.enums.Plataforma;
import com.proyecto.servicios.enums.PropositoCodigo;
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
 * Código de un solo uso enviado por correo. El código se guarda con BCrypt y el ticket con SHA-256:
 * en la base de datos nunca queda el valor en claro.
 * <p>
 * El ticket es un secreto que solo recibe el dispositivo que inició el proceso; sin él, el código
 * por sí solo no sirve, lo que evita que alguien lo use desde otro lugar.
 */
@Entity
@Table(name = "codigos_verificacion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodigoVerificacion {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposito", nullable = false, length = 30)
    private PropositoCodigo proposito;

    @Column(name = "codigo_hash", nullable = false, length = 100)
    private String codigoHash;

    @Column(name = "ticket_hash", nullable = false, unique = true, length = 64)
    private String ticketHash;

    /** Id relacionado (p. ej. la solicitud de cierre de sesión). */
    @Column(name = "referencia", length = 36)
    private String referencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "plataforma", length = 10)
    private Plataforma plataforma;

    @Column(name = "dispositivo", length = 150)
    private String dispositivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoCodigo estado;

    @Builder.Default
    @Column(name = "intentos", nullable = false)
    private Integer intentos = 0;

    @Builder.Default
    @Column(name = "reenvios", nullable = false)
    private Integer reenvios = 0;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "fecha_uso")
    private LocalDateTime fechaUso;

    public boolean estaVigente(LocalDateTime ahora) {
        return estado == EstadoCodigo.PENDIENTE && ahora.isBefore(expiraEn);
    }
}
