package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.SeguridadProperties;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.entity.seguridad.Sesion;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.enums.EstadoSesion;
import com.proyecto.servicios.enums.Plataforma;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.repository.SesionRepository;
import com.proyecto.servicios.seguridad.NotificadorSesion;
import com.proyecto.servicios.seguridad.TokenSeguro;
import com.proyecto.servicios.service.SesionService;
import com.proyecto.servicios.util.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SesionServiceImpl implements SesionService {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();
    private static final int MAX_DISPOSITIVO = 150;

    private final SesionRepository sesionRepository;
    private final JwtTokenService jwtTokenService;
    private final SeguridadProperties properties;
    private final NotificadorSesion notificador;

    @Override
    @Transactional
    public TokensSesion crear(Usuario usuario, Plataforma plataforma, String dispositivo) {
        LocalDateTime ahora = LocalDateTime.now(ZONE_ID);
        String refreshToken = TokenSeguro.generar();
        Sesion sesion = sesionRepository.save(Sesion.builder()
                .id(UUID.randomUUID().toString())
                .usuario(usuario)
                .plataforma(plataforma)
                .dispositivo(limpiarDispositivo(dispositivo))
                .refreshHash(TokenSeguro.huella(refreshToken))
                .estado(EstadoSesion.ACTIVA)
                .fechaCreacion(ahora)
                .ultimaActividad(ahora)
                .expiraEn(ahora.plus(properties.getSesion().getDuracionMaxima()))
                .build());
        log.info("Sesión creada para usuario={} plataforma={}", usuario.getId(), plataforma);
        return emitir(sesion, refreshToken, ahora);
    }

    @Override
    @Transactional
    public Optional<Sesion> buscarVigente(Long usuarioId) {
        LocalDateTime ahora = LocalDateTime.now(ZONE_ID);
        Sesion vigente = null;
        for (Sesion sesion : sesionRepository.findByUsuarioIdAndEstado(usuarioId, EstadoSesion.ACTIVA)) {
            if (vigente == null && sesion.estaVigente(ahora, inactividad())) {
                vigente = sesion;
            } else {
                sesion.cerrar(EstadoSesion.EXPIRADA, "Sesión caducada por inactividad o duración máxima", ahora);
            }
        }
        return Optional.ofNullable(vigente);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean estaActiva(String sesionId) {
        LocalDateTime ahora = LocalDateTime.now(ZONE_ID);
        return sesionId != null && sesionRepository.findById(sesionId)
                .map(s -> s.estaVigente(ahora, inactividad()))
                .orElse(false);
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public TokensSesion refrescar(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ApiResponseEnum.REFRESH_INVALIDO);
        }
        String huella = TokenSeguro.huella(refreshToken);
        LocalDateTime ahora = LocalDateTime.now(ZONE_ID);

        Optional<Sesion> encontrada = sesionRepository.findByRefreshHashForUpdate(huella);
        if (encontrada.isEmpty()) {
            detectarReutilizacion(huella);
            throw new BusinessException(ApiResponseEnum.REFRESH_INVALIDO);
        }

        Sesion sesion = encontrada.get();
        if (!sesion.estaVigente(ahora, inactividad())) {
            if (sesion.getEstado() == EstadoSesion.ACTIVA) {
                sesion.cerrar(EstadoSesion.EXPIRADA, "Sesión caducada por inactividad o duración máxima", ahora);
            }
            throw new BusinessException(ApiResponseEnum.REFRESH_INVALIDO);
        }

        String nuevoRefresh = TokenSeguro.generar();
        sesion.setRefreshHashAnterior(huella);
        sesion.setRefreshHash(TokenSeguro.huella(nuevoRefresh));
        sesion.setUltimaActividad(ahora);
        return emitir(sesion, nuevoRefresh, ahora);
    }

    private void detectarReutilizacion(String huella) {
        sesionRepository.findByRefreshHashAnterior(huella)
                .filter(s -> s.getEstado() == EstadoSesion.ACTIVA)
                .ifPresent(s -> {
                    log.warn("Refresh token reutilizado en la sesión {}; se revoca por seguridad", s.getId());
                    cerrarSesion(s, EstadoSesion.REVOCADA, "Se detectó el uso de un token anterior; inicie sesión nuevamente");
                });
    }

    @Override
    @Transactional
    public void cerrar(String sesionId, EstadoSesion estadoFinal, String motivo) {
        sesionRepository.findById(sesionId)
                .filter(s -> s.getEstado() == EstadoSesion.ACTIVA)
                .ifPresent(s -> cerrarSesion(s, estadoFinal, motivo));
    }

    @Override
    @Transactional
    public void cerrarTodas(Long usuarioId, String motivo) {
        sesionRepository.findByUsuarioIdAndEstado(usuarioId, EstadoSesion.ACTIVA)
                .forEach(s -> cerrarSesion(s, EstadoSesion.REVOCADA, motivo));
    }

    private void cerrarSesion(Sesion sesion, EstadoSesion estadoFinal, String motivo) {
        sesion.cerrar(estadoFinal, motivo, LocalDateTime.now(ZONE_ID));
        sesionRepository.save(sesion);
        log.info("Sesión {} finalizada como {}", sesion.getId(), estadoFinal);
        String id = sesion.getId();
        despuesDelCommit(() -> notificador.notificarSesionCerrada(id, motivo));
    }

    private TokensSesion emitir(Sesion sesion, String refreshToken, LocalDateTime ahora) {
        LocalDateTime finRefresh = sesion.getUltimaActividad().plus(inactividad());
        if (sesion.getExpiraEn().isBefore(finRefresh)) {
            finRefresh = sesion.getExpiraEn();
        }
        return new TokensSesion(sesion.getUsuario(), sesion.getPlataforma(),
                jwtTokenService.generarAccessToken(sesion.getUsuario(), sesion.getId()),
                jwtTokenService.getAccessTokenSegundos(),
                refreshToken,
                Math.max(0, Duration.between(ahora, finRefresh).toSeconds()));
    }

    private Duration inactividad() {
        return properties.getSesion().getInactividadMaxima();
    }

    private static String limpiarDispositivo(String dispositivo) {
        if (dispositivo == null || dispositivo.isBlank()) {
            return null;
        }
        String limpio = dispositivo.replaceAll("[\\p{Cntrl}<>]", "").trim();
        return limpio.length() > MAX_DISPOSITIVO ? limpio.substring(0, MAX_DISPOSITIVO) : limpio;
    }

    /** Las notificaciones se envían solo si la transacción se confirma. */
    static void despuesDelCommit(Runnable accion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    accion.run();
                }
            });
        } else {
            accion.run();
        }
    }
}
