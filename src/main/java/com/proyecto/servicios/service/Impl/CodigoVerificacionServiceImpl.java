package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.SeguridadProperties;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.entity.seguridad.CodigoVerificacion;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.enums.EstadoCodigo;
import com.proyecto.servicios.enums.Plataforma;
import com.proyecto.servicios.enums.PropositoCodigo;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.repository.CodigoVerificacionRepository;
import com.proyecto.servicios.seguridad.TokenSeguro;
import com.proyecto.servicios.service.CodigoVerificacionService;
import com.proyecto.servicios.service.CorreoService;
import com.proyecto.servicios.util.DatosSensiblesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodigoVerificacionServiceImpl implements CodigoVerificacionService {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int LIMITE_CODIGO = 1_000_000;
    private static final int MAX_INTENTOS_POR_CODIGO = 5;

    private final CodigoVerificacionRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final CorreoService correoService;
    private final SeguridadProperties properties;

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public Emision emitir(Usuario usuario, PropositoCodigo proposito, Plataforma plataforma, String dispositivo,
                          String referencia) {
        LocalDateTime ahora = LocalDateTime.now(ZONE_ID);
        SeguridadProperties.Codigo reglas = properties.getCodigo();

        if (repository.countByUsuarioIdAndPropositoAndFechaCreacionAfter(usuario.getId(), proposito,
                ahora.minusHours(1)) >= reglas.getMaxPorHora()) {
            throw new BusinessException(ApiResponseEnum.DEMASIADAS_SOLICITUDES,
                    "Se alcanzó el máximo de códigos por hora; intente más tarde");
        }
        for (CodigoVerificacion anterior : repository.findByUsuarioIdAndPropositoAndEstado(
                usuario.getId(), proposito, EstadoCodigo.PENDIENTE)) {
            exigirEspera(anterior, ahora);
            anterior.setEstado(EstadoCodigo.ANULADO);
        }

        String ticket = TokenSeguro.generar();
        String codigo = generarCodigo();
        repository.save(CodigoVerificacion.builder()
                .id(UUID.randomUUID().toString())
                .usuario(usuario)
                .proposito(proposito)
                .codigoHash(passwordEncoder.encode(codigo))
                .ticketHash(TokenSeguro.huella(ticket))
                .referencia(referencia)
                .plataforma(plataforma)
                .dispositivo(dispositivo)
                .estado(EstadoCodigo.PENDIENTE)
                .fechaCreacion(ahora)
                .fechaEnvio(ahora)
                .expiraEn(ahora.plus(reglas.getVigencia()))
                .build());

        correoService.enviarCodigo(usuario.getCorreo(), proposito, codigo, reglas.getVigencia().toMinutes());
        log.info("Código {} enviado al usuario={}", proposito, usuario.getId());
        return new Emision(ticket, DatosSensiblesUtil.enmascararCorreo(usuario.getCorreo()), reglas.getVigencia().toSeconds());
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public Emision reenviar(String ticket) {
        CodigoVerificacion codigo = buscar(ticket);
        LocalDateTime ahora = LocalDateTime.now(ZONE_ID);
        SeguridadProperties.Codigo reglas = properties.getCodigo();
        if (codigo.getEstado() != EstadoCodigo.PENDIENTE) {
            throw new BusinessException(ApiResponseEnum.CODIGO_INVALIDO);
        }
        exigirEspera(codigo, ahora);
        if (codigo.getReenvios() >= reglas.getMaxReenvios()) {
            throw new BusinessException(ApiResponseEnum.DEMASIADAS_SOLICITUDES,
                    "Se alcanzó el máximo de reenvíos; inicie el proceso de nuevo");
        }

        String nuevo = generarCodigo();
        codigo.setCodigoHash(passwordEncoder.encode(nuevo));
        codigo.setReenvios(codigo.getReenvios() + 1);
        codigo.setIntentos(0);
        codigo.setFechaEnvio(ahora);
        codigo.setExpiraEn(ahora.plus(reglas.getVigencia()));

        Usuario usuario = codigo.getUsuario();
        correoService.enviarCodigo(usuario.getCorreo(), codigo.getProposito(), nuevo, reglas.getVigencia().toMinutes());
        return new Emision(ticket, DatosSensiblesUtil.enmascararCorreo(usuario.getCorreo()), reglas.getVigencia().toSeconds());
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public CodigoVerificacion buscarVigente(String ticket, PropositoCodigo proposito) {
        CodigoVerificacion codigo = buscar(ticket);
        if (codigo.getProposito() != proposito) {
            throw new BusinessException(ApiResponseEnum.CODIGO_INVALIDO);
        }
        LocalDateTime ahora = LocalDateTime.now(ZONE_ID);
        if (!codigo.estaVigente(ahora)) {
            if (codigo.getEstado() == EstadoCodigo.PENDIENTE) {
                codigo.setEstado(EstadoCodigo.EXPIRADO);
                throw new BusinessException(ApiResponseEnum.CODIGO_INVALIDO, "El código expiró; solicite uno nuevo");
            }
            throw new BusinessException(ApiResponseEnum.CODIGO_INVALIDO);
        }
        return codigo;
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public boolean verificar(CodigoVerificacion codigo, String valor) {
        if (valor != null && passwordEncoder.matches(valor, codigo.getCodigoHash())) {
            codigo.setEstado(EstadoCodigo.USADO);
            codigo.setFechaUso(LocalDateTime.now(ZONE_ID));
            repository.save(codigo);
            return true;
        }
        codigo.setIntentos(codigo.getIntentos() + 1);
        if (codigo.getIntentos() >= MAX_INTENTOS_POR_CODIGO) {
            codigo.setEstado(EstadoCodigo.ANULADO);
        }
        repository.save(codigo);
        return false;
    }

    private CodigoVerificacion buscar(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new BusinessException(ApiResponseEnum.CODIGO_INVALIDO);
        }
        return repository.findByTicketHashForUpdate(TokenSeguro.huella(ticket))
                .orElseThrow(() -> new BusinessException(ApiResponseEnum.CODIGO_INVALIDO));
    }

    private void exigirEspera(CodigoVerificacion codigo, LocalDateTime ahora) {
        LocalDateTime disponible = codigo.getFechaEnvio().plus(properties.getCodigo().getEsperaReenvio());
        if (ahora.isBefore(disponible)) {
            long segundos = Math.max(1, Duration.between(ahora, disponible).toSeconds());
            throw new BusinessException(ApiResponseEnum.DEMASIADAS_SOLICITUDES,
                    "Espere " + segundos + " segundos antes de solicitar otro código");
        }
    }

    private static String generarCodigo() {
        return String.format("%06d", RANDOM.nextInt(LIMITE_CODIGO));
    }
}
