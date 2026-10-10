package com.proyecto.servicios.entity.cliente;

import com.proyecto.servicios.enums.BiometriaTipo;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometriaInfo {

    @Builder.Default
    @Column(name = "biometria_enrolada", nullable = false)
    private Boolean biometriaEnrolada = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "biometria_tipo")
    private BiometriaTipo biometriaTipo;

    @Column(name = "biometria_token")
    private String biometriaToken;

    @Column(name = "biometria_fecha_registro")
    private LocalDateTime biometriaFechaRegistro;
}

