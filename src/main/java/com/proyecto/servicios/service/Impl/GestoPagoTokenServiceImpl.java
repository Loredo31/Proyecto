package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class GestoPagoTokenServiceImpl implements GestoPagoTokenService {

    private static final Duration MARGEN_EXPIRACION = Duration.ofMinutes(5);

    private final GestoPagoAuthClient gestoPagoAuthClient;
    private final GestoPagoTokenRepository tokenRepository;
    private final GestoPagoTokenMapper tokenMapper;

    private final AtomicReference<TokenEnMemoria> tokenEnMemoria = new AtomicReference<>();
    private final Object candadoRenovacion = new Object();

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.password}")
    private String password;

    @Value("${gestopago.auth.token-ttl:PT24H}")
    private Duration tokenTtl;

    public GestoPagoTokenServiceImpl(GestoPagoAuthClient gestoPagoAuthClient,
                                     GestoPagoTokenRepository tokenRepository,
                                     GestoPagoTokenMapper tokenMapper) {
        this.gestoPagoAuthClient = gestoPagoAuthClient;
        this.tokenRepository = tokenRepository;
        this.tokenMapper = tokenMapper;
    }

    @Override
    @Scheduled(fixedRateString = "${gestopago.auth.refresh-rate-ms:82800000}", initialDelay = 0)
    public void renovarToken() {
        try {
            autenticarYPersistir();
        } catch (Exception e) {
            log.error("Error al renovar token GestoPago: {}", e.getMessage());
        }
    }

    @Override
    public Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidor, String codigoDispositivo) {
        return tokenRepository.findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo);
    }

    @Override
    public String obtenerTokenVigente() {
        TokenEnMemoria actual = tokenEnMemoria.get();
        if (actual != null && actual.vigente()) {
            return actual.token();
        }

        synchronized (candadoRenovacion) {
            actual = tokenEnMemoria.get();
            if (actual != null && actual.vigente()) {
                return actual.token();
            }

            Optional<TokenEnMemoria> desdeBd = obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                    .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                    .map(this::aMemoria)
                    .filter(TokenEnMemoria::vigente);
            if (desdeBd.isPresent()) {
                tokenEnMemoria.set(desdeBd.get());
                return desdeBd.get().token();
            }

            log.info("No hay token GestoPago vigente en memoria ni en BD; autenticando dispositivo");
            return autenticarYPersistir().token();
        }
    }

    @Override
    public void invalidarToken() {
        tokenEnMemoria.set(null);
        obtenerTokenActivo(idDistribuidor, codigoDispositivo).ifPresent(token -> {
            token.setActivo(false);
            tokenRepository.save(token);
        });
        log.warn("Token GestoPago invalidado; se solicitará uno nuevo en la siguiente transacción");
    }

    private TokenEnMemoria autenticarYPersistir() {
        log.info("Renovando token GestoPago para distribuidor={}", idDistribuidor);
        GestoPagoAuthResponse response = gestoPagoAuthClient.authenticate(idDistribuidor, codigoDispositivo, password);

        if (response == null || response.getToken() == null || response.getToken().isBlank()) {
            throw new IllegalStateException("La respuesta de autenticación de GestoPago no contiene token");
        }

        GestoPagoToken tokenEntity = tokenRepository
                .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                .map(existing -> {
                    tokenMapper.updateEntity(response, existing);
                    existing.setActivo(true);
                    return existing;
                })
                .orElseGet(() -> {
                    GestoPagoToken nuevo = tokenMapper.toEntity(response);
                    nuevo.setIdDistribuidor(idDistribuidor);
                    nuevo.setCodigoDispositivo(codigoDispositivo);
                    nuevo.setActivo(true);
                    return nuevo;
                });

        GestoPagoToken guardado = tokenRepository.save(tokenEntity);
        TokenEnMemoria enMemoria = aMemoria(guardado);
        tokenEnMemoria.set(enMemoria);
        log.info("Token GestoPago renovado correctamente; vigente hasta {}", enMemoria.expiraEn());
        return enMemoria;
    }

    private TokenEnMemoria aMemoria(GestoPagoToken token) {
        Duration vigencia = tokenTtl;
        if (token.getExpiresIn() != null && token.getExpiresIn() > 0) {
            Duration delProveedor = Duration.ofSeconds(token.getExpiresIn());
            if (delProveedor.compareTo(vigencia) < 0) {
                vigencia = delProveedor;
            }
        }
        LocalDateTime emitido = token.getFechaActualizacion() != null ? token.getFechaActualizacion() : LocalDateTime.now();
        return new TokenEnMemoria(token.getToken(), emitido.plus(vigencia).minus(MARGEN_EXPIRACION));
    }

    private record TokenEnMemoria(String token, LocalDateTime expiraEn) {
        boolean vigente() {
            return LocalDateTime.now().isBefore(expiraEn);
        }

        @Override
        public String toString() {
            return "TokenEnMemoria[token=****, expiraEn=" + expiraEn + "]";
        }
    }
}
