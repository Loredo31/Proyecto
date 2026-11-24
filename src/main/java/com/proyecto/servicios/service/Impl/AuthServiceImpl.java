package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.SeguridadProperties;
import com.proyecto.servicios.dto.auth.ConfirmarCierreCorreoRequest;
import com.proyecto.servicios.dto.auth.ConfirmarRecuperacionRequest;
import com.proyecto.servicios.dto.auth.ConsultaSolicitudRequest;
import com.proyecto.servicios.dto.auth.LoginRequest;
import com.proyecto.servicios.dto.auth.LoginResponseDto;
import com.proyecto.servicios.dto.auth.RespuestaSolicitudRequest;
import com.proyecto.servicios.dto.auth.SegundoFactorDto;
import com.proyecto.servicios.dto.auth.SolicitudSesionDto;
import com.proyecto.servicios.dto.auth.VerificarCodigoRequest;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.entity.seguridad.CodigoVerificacion;
import com.proyecto.servicios.entity.seguridad.Sesion;
import com.proyecto.servicios.entity.seguridad.SolicitudSesion;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.enums.EstadoSesion;
import com.proyecto.servicios.enums.EstadoSolicitudSesion;
import com.proyecto.servicios.enums.Plataforma;
import com.proyecto.servicios.enums.PropositoCodigo;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.exception.ContrasenaInvalidaException;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.CuentaBloqueadaTemporalException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import com.proyecto.servicios.repository.SolicitudSesionRepository;
import com.proyecto.servicios.repository.UsuarioRepository;
import com.proyecto.servicios.seguridad.NotificadorSesion;
import com.proyecto.servicios.seguridad.TokenSeguro;
import com.proyecto.servicios.seguridad.UsuarioAutenticado;
import com.proyecto.servicios.service.AuthService;
import com.proyecto.servicios.service.CodigoVerificacionService;
import com.proyecto.servicios.service.CodigoVerificacionService.Emision;
import com.proyecto.servicios.service.CorreoService;
import com.proyecto.servicios.service.SesionService;
import com.proyecto.servicios.service.SesionService.TokensSesion;
import com.proyecto.servicios.util.DatosSensiblesUtil;
import com.proyecto.servicios.util.ValidadorContrasena;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();
    private static final String MENSAJE_CREDENCIALES = "Correo o contraseña incorrectos";

    private final UsuarioRepository usuarioRepository;
    private final SolicitudSesionRepository solicitudRepository;
    private final PasswordEncoder passwordEncoder;
    private final SesionService sesionService;
    private final CodigoVerificacionService codigoService;
    private final CorreoService correoService;
    private final NotificadorSesion notificador;
    private final SeguridadProperties properties;

    /** Hash ficticio para que un correo inexistente tarde lo mismo que una contraseña incorrecta. */
    private final String hashFicticio;

    @SuppressWarnings("java:S107")
    public AuthServiceImpl(UsuarioRepository usuarioRepository, SolicitudSesionRepository solicitudRepository,
                           PasswordEncoder passwordEncoder, SesionService sesionService,
                           CodigoVerificacionService codigoService, CorreoService correoService,
                           NotificadorSesion notificador, SeguridadProperties properties) {
        this.usuarioRepository = usuarioRepository;
        this.solicitudRepository = solicitudRepository;
        this.passwordEncoder = passwordEncoder;
        this.sesionService = sesionService;
        this.codigoService = codigoService;
        this.correoService = correoService;
        this.notificador = notificador;
        this.properties = properties;
        this.hashFicticio = passwordEncoder.encode(TokenSeguro.generar());
    }

    // ------------------------------------------------------------------ Login con 2FA

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public SegundoFactorDto login(LoginRequest request) {
        Optional<Usuario> encontrado = usuarioRepository.findByCorreo(request.getCorreo().trim());
        if (encontrado.isEmpty()) {
            passwordEncoder.matches(request.getPassword(), hashFicticio);
            log.warn("Intento de login con correo no registrado");
            throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES);
        }

        Usuario usuario = encontrado.get();
        LocalDateTime ahora = LocalDateTime.now(ZONE_ID);
        exigirNoBloqueado(usuario, ahora);
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            registrarIntentoFallido(usuario, ahora, restantes -> new CredencialesInvalidasException(MENSAJE_CREDENCIALES));
        }
        validarUsuarioHabilitado(usuario);

        // Los intentos fallidos NO se reinician aquí: solo al completar el segundo factor.
        Emision emision = codigoService.emitir(usuario, PropositoCodigo.LOGIN, request.getPlataforma(),
                limpiar(request.getDispositivo()), null);
        return aSegundoFactor(emision);
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public ResultadoLogin verificarSegundoFactor(VerificarCodigoRequest request) {
        CodigoVerificacion codigo = codigoService.buscarVigente(request.getTicket(), PropositoCodigo.LOGIN);
        Usuario usuario = codigo.getUsuario();
        LocalDateTime ahora = LocalDateTime.now(ZONE_ID);
        verificarCodigo(usuario, codigo, request.getCodigo(), ahora);
        validarUsuarioHabilitado(usuario);

        Optional<Sesion> vigente = sesionService.buscarVigente(usuario.getId());
        if (vigente.isPresent()) {
            return new ResultadoLogin(null,
                    crearSolicitud(usuario, vigente.get(), codigo.getPlataforma(), codigo.getDispositivo(), ahora),
                    codigo.getPlataforma());
        }
        TokensSesion tokens = sesionService.crear(usuario, codigo.getPlataforma(), codigo.getDispositivo());
        log.info("Login con segundo factor completado para usuario={}", usuario.getId());
        return new ResultadoLogin(aRespuesta(tokens), null, tokens.plataforma());
    }

    @Override
    public SegundoFactorDto reenviarCodigo(String ticket) {
        return aSegundoFactor(codigoService.reenviar(ticket));
    }

    // ------------------------------------------------------------------ Sesión única

    private SolicitudSesionDto crearSolicitud(Usuario usuario, Sesion actual, Plataforma plataforma,
                                              String dispositivo, LocalDateTime ahora) {
        solicitudRepository.findByUsuarioIdAndEstado(usuario.getId(), EstadoSolicitudSesion.PENDIENTE)
                .forEach(s -> s.setEstado(EstadoSolicitudSesion.EXPIRADA));

        String secreto = TokenSeguro.generar();
        Duration vigencia = properties.getSesion().getVigenciaSolicitud();
        SolicitudSesion solicitud = solicitudRepository.save(SolicitudSesion.builder()
                .id(UUID.randomUUID().toString())
                .usuario(usuario)
                .sesionActualId(actual.getId())
                .plataforma(plataforma)
                .dispositivo(dispositivo)
                .secretoHash(TokenSeguro.huella(secreto))
                .estado(EstadoSolicitudSesion.PENDIENTE)
                .fechaCreacion(ahora)
                .expiraEn(ahora.plus(vigencia))
                .build());

        String sesionId = actual.getId();
        SesionServiceImpl.despuesDelCommit(() -> notificador.notificarSolicitudCierre(sesionId, solicitud.getId(),
                plataforma, dispositivo, vigencia.toSeconds()));
        log.info("Usuario={} ya tiene sesión activa en {}; se solicitó su cierre", usuario.getId(), actual.getPlataforma());

        return SolicitudSesionDto.builder()
                .solicitudId(solicitud.getId())
                .secreto(secreto)
                .estado(EstadoSolicitudSesion.PENDIENTE)
                .expiraEnSegundos(vigencia.toSeconds())
                .plataformaSesionActiva(actual.getPlataforma())
                .dispositivoSesionActiva(actual.getDispositivo())
                .build();
    }

    @Override
    @Transactional
    public ResultadoLogin consultarSolicitud(ConsultaSolicitudRequest request) {
        SolicitudSesion solicitud = solicitudConSecreto(request.getSolicitudId(), request.getSecreto());
        LocalDateTime ahora = LocalDateTime.now(ZONE_ID);
        if (solicitud.getEstado() == EstadoSolicitudSesion.PENDIENTE && solicitud.estaVencida(ahora)) {
            solicitud.setEstado(EstadoSolicitudSesion.EXPIRADA);
        }
        if (solicitud.getEstado() == EstadoSolicitudSesion.APROBADA) {
            return emitirDesdeSolicitud(solicitud, ahora);
        }
        return new ResultadoLogin(null, estado(solicitud, ahora), solicitud.getPlataforma());
    }

    /** La solicitud aprobada se consume una sola vez para emitir los tokens del dispositivo nuevo. */
    private ResultadoLogin emitirDesdeSolicitud(SolicitudSesion solicitud, LocalDateTime ahora) {
        Usuario usuario = solicitud.getUsuario();
        if (sesionService.buscarVigente(usuario.getId()).isPresent()) {
            // Otro dispositivo inició sesión mientras tanto: el usuario debe intentarlo de nuevo.
            solicitud.setEstado(EstadoSolicitudSesion.EXPIRADA);
            return new ResultadoLogin(null, estado(solicitud, ahora), solicitud.getPlataforma());
        }
        solicitud.setEstado(EstadoSolicitudSesion.CONSUMIDA);
        TokensSesion tokens = sesionService.crear(usuario, solicitud.getPlataforma(), solicitud.getDispositivo());
        log.info("Solicitud {} aprobada; sesión emitida para usuario={}", solicitud.getId(), usuario.getId());
        return new ResultadoLogin(aRespuesta(tokens), estado(solicitud, ahora), tokens.plataforma());
    }

    @Override
    @Transactional
    public SolicitudSesionDto responderSolicitud(UsuarioAutenticado usuario, RespuestaSolicitudRequest request) {
        LocalDateTime ahora = LocalDateTime.now(ZONE_ID);
        SolicitudSesion solicitud = solicitudRepository.findByIdForUpdate(request.getSolicitudId())
                .filter(s -> s.getUsuario().getId().equals(usuario.usuarioId()))
                .filter(s -> s.getSesionActualId().equals(usuario.sesionId()))
                .filter(s -> s.getEstado() == EstadoSolicitudSesion.PENDIENTE && !s.estaVencida(ahora))
                .orElseThrow(() -> new BusinessException(ApiResponseEnum.SOLICITUD_SESION_INVALIDA));

        solicitud.setFechaRespuesta(ahora);
        if (Boolean.TRUE.equals(request.getAprobar())) {
            solicitud.setEstado(EstadoSolicitudSesion.APROBADA);
            sesionService.cerrar(usuario.sesionId(), EstadoSesion.REVOCADA,
                    "La sesión se cerró para iniciar sesión en otro dispositivo (" + solicitud.getPlataforma() + ")");
        } else {
            solicitud.setEstado(EstadoSolicitudSesion.RECHAZADA);
            log.info("Usuario={} rechazó cerrar su sesión", usuario.usuarioId());
        }
        return estado(solicitud, ahora);
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public SegundoFactorDto solicitarCierrePorCorreo(ConsultaSolicitudRequest request) {
        SolicitudSesion solicitud = solicitudPendiente(request.getSolicitudId(), request.getSecreto());
        Emision emision = codigoService.emitir(solicitud.getUsuario(), PropositoCodigo.CIERRE_SESION,
                solicitud.getPlataforma(), solicitud.getDispositivo(), solicitud.getId());
        // Se da al usuario el mismo tiempo que dura el código para completar el cierre.
        LocalDateTime limite = LocalDateTime.now(ZONE_ID).plus(properties.getCodigo().getVigencia());
        if (solicitud.getExpiraEn().isBefore(limite)) {
            solicitud.setExpiraEn(limite);
        }
        return aSegundoFactor(emision);
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public ResultadoLogin confirmarCierrePorCorreo(ConfirmarCierreCorreoRequest request) {
        SolicitudSesion solicitud = solicitudPendiente(request.getSolicitudId(), request.getSecreto());
        CodigoVerificacion codigo = codigoService.buscarVigente(request.getTicket(), PropositoCodigo.CIERRE_SESION);
        if (!solicitud.getId().equals(codigo.getReferencia())) {
            throw new BusinessException(ApiResponseEnum.CODIGO_INVALIDO);
        }
        LocalDateTime ahora = LocalDateTime.now(ZONE_ID);
        verificarCodigo(solicitud.getUsuario(), codigo, request.getCodigo(), ahora);

        solicitud.setEstado(EstadoSolicitudSesion.APROBADA);
        solicitud.setFechaRespuesta(ahora);
        sesionService.cerrar(solicitud.getSesionActualId(), EstadoSesion.REVOCADA,
                "La sesión se cerró con verificación por correo para iniciar en otro dispositivo");
        return emitirDesdeSolicitud(solicitud, ahora);
    }

    private SolicitudSesion solicitudConSecreto(String solicitudId, String secreto) {
        return solicitudRepository.findByIdForUpdate(solicitudId)
                .filter(s -> TokenSeguro.coincide(secreto, s.getSecretoHash()))
                .orElseThrow(() -> new BusinessException(ApiResponseEnum.SOLICITUD_SESION_INVALIDA));
    }

    private SolicitudSesion solicitudPendiente(String solicitudId, String secreto) {
        SolicitudSesion solicitud = solicitudConSecreto(solicitudId, secreto);
        if (solicitud.getEstado() != EstadoSolicitudSesion.PENDIENTE
                || solicitud.estaVencida(LocalDateTime.now(ZONE_ID))) {
            throw new BusinessException(ApiResponseEnum.SOLICITUD_SESION_INVALIDA);
        }
        return solicitud;
    }

    // ------------------------------------------------------------------ Recuperación de contraseña

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public SegundoFactorDto solicitarRecuperacion(String correo) {
        String limpio = correo.trim();
        LocalDateTime ahora = LocalDateTime.now(ZONE_ID);
        Optional<Usuario> usuario = usuarioRepository.findByCorreo(limpio)
                .filter(u -> Boolean.TRUE.equals(u.getActivo()) && !u.estaBloqueado(ahora));
        if (usuario.isPresent()) {
            return aSegundoFactor(codigoService.emitir(usuario.get(), PropositoCodigo.RECUPERACION, null, null, null));
        }
        // Respuesta indistinguible para no revelar si el correo está registrado.
        log.info("Solicitud de recuperación para un correo no registrado, inactivo o bloqueado");
        return SegundoFactorDto.builder()
                .ticket(TokenSeguro.generar())
                .correoEnmascarado(DatosSensiblesUtil.enmascararCorreo(limpio))
                .expiraEnSegundos(properties.getCodigo().getVigencia().toSeconds())
                .build();
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public void confirmarRecuperacion(ConfirmarRecuperacionRequest request) {
        if (!ValidadorContrasena.cumpleReglas(request.getNuevaPassword())) {
            throw new ContrasenaInvalidaException(ValidadorContrasena.MENSAJE_REGLAS);
        }
        CodigoVerificacion codigo = codigoService.buscarVigente(request.getTicket(), PropositoCodigo.RECUPERACION);
        Usuario usuario = codigo.getUsuario();
        verificarCodigo(usuario, codigo, request.getCodigo(), LocalDateTime.now(ZONE_ID));

        usuario.setPassword(passwordEncoder.encode(request.getNuevaPassword()));
        usuario.setFechaActualizacion(LocalDateTime.now(ZONE_ID));
        usuarioRepository.save(usuario);
        sesionService.cerrarTodas(usuario.getId(), "La contraseña se cambió; inicie sesión nuevamente");
        log.info("Contraseña restablecida para usuario={}", usuario.getId());

        String destino = usuario.getCorreo();
        SesionServiceImpl.despuesDelCommit(() -> avisarCambioContrasena(destino));
    }

    private void avisarCambioContrasena(String destino) {
        try {
            correoService.notificarCambioContrasena(destino);
        } catch (RuntimeException ex) {
            log.warn("No se pudo enviar el aviso de cambio de contraseña: {}", ex.getMessage());
        }
    }

    // ------------------------------------------------------------------ Tokens

    @Override
    public ResultadoLogin refrescar(String refreshToken) {
        TokensSesion tokens = sesionService.refrescar(refreshToken);
        return new ResultadoLogin(aRespuesta(tokens), null, tokens.plataforma());
    }

    @Override
    public void logout(UsuarioAutenticado usuario) {
        sesionService.cerrar(usuario.sesionId(), EstadoSesion.CERRADA, "El usuario cerró sesión");
    }

    // ------------------------------------------------------------------ Bloqueo por intentos

    private static void exigirNoBloqueado(Usuario usuario, LocalDateTime ahora) {
        if (usuario.estaBloqueado(ahora)) {
            throw new CuentaBloqueadaTemporalException(Duration.between(ahora, usuario.getBloqueadoHasta()).toMinutes() + 1);
        }
    }

    /**
     * Valida un código contra la cuenta. Contraseñas y códigos incorrectos comparten el mismo contador:
     * al llegar al máximo se bloquea toda la cuenta.
     */
    private void verificarCodigo(Usuario usuario, CodigoVerificacion codigo, String valor, LocalDateTime ahora) {
        exigirNoBloqueado(usuario, ahora);
        if (!codigoService.verificar(codigo, valor)) {
            registrarIntentoFallido(usuario, ahora, restantes -> new BusinessException(ApiResponseEnum.CODIGO_INVALIDO,
                    "El código es incorrecto. Intentos restantes antes de bloquear la cuenta: " + restantes));
        }
        reiniciarIntentos(usuario);
    }

    /** Cuenta el intento fallido; al llegar al máximo bloquea la cuenta. Siempre lanza excepción. */
    private void registrarIntentoFallido(Usuario usuario, LocalDateTime ahora, IntFunction<BusinessException> error) {
        int intentos = usuario.getIntentosFallidos() + 1;
        SeguridadProperties.Login reglas = properties.getLogin();
        if (intentos >= reglas.getMaxIntentos()) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(ahora.plus(reglas.getBloqueo()));
            usuarioRepository.save(usuario);
            sesionService.cerrarTodas(usuario.getId(), "La cuenta se bloqueó por intentos fallidos");
            log.warn("Usuario={} bloqueado por {} intentos fallidos", usuario.getId(), intentos);
            throw new CuentaBloqueadaTemporalException(reglas.getBloqueo().toMinutes());
        }
        usuario.setIntentosFallidos(intentos);
        usuarioRepository.save(usuario);
        log.warn("Intento fallido para usuario={} ({} de {})", usuario.getId(), intentos, reglas.getMaxIntentos());
        throw error.apply(reglas.getMaxIntentos() - intentos);
    }

    private void reiniciarIntentos(Usuario usuario) {
        if (usuario.getIntentosFallidos() > 0 || usuario.getBloqueadoHasta() != null) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
            usuarioRepository.save(usuario);
        }
    }

    private static void validarUsuarioHabilitado(Usuario usuario) {
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new UsuarioInactivoException("El usuario se encuentra inactivo");
        }
        if (usuario.getCliente() == null || Boolean.TRUE.equals(usuario.getCliente().getEliminadoLogico())) {
            throw new UsuarioInactivoException("El usuario no está asociado a un cliente activo");
        }
    }

    // ------------------------------------------------------------------ Mapeos

    private static String limpiar(String dispositivo) {
        if (dispositivo == null || dispositivo.isBlank()) {
            return null;
        }
        String limpio = dispositivo.replaceAll("[\\p{Cntrl}<>]", "").trim();
        return limpio.length() > 150 ? limpio.substring(0, 150) : limpio;
    }

    private static SegundoFactorDto aSegundoFactor(Emision emision) {
        return SegundoFactorDto.builder()
                .ticket(emision.ticket())
                .correoEnmascarado(emision.correoEnmascarado())
                .expiraEnSegundos(emision.expiraEnSegundos())
                .build();
    }

    private static SolicitudSesionDto estado(SolicitudSesion solicitud, LocalDateTime ahora) {
        return SolicitudSesionDto.builder()
                .solicitudId(solicitud.getId())
                .estado(solicitud.getEstado())
                .expiraEnSegundos(Math.max(0, Duration.between(ahora, solicitud.getExpiraEn()).toSeconds()))
                .build();
    }

    private static LoginResponseDto aRespuesta(TokensSesion tokens) {
        Usuario usuario = tokens.usuario();
        return LoginResponseDto.builder()
                .accessToken(tokens.accessToken())
                .expiraEnSegundos(tokens.accessSegundos())
                .refreshToken(tokens.refreshToken())
                .refreshExpiraEnSegundos(tokens.refreshSegundos())
                .correo(usuario.getCorreo())
                .nombre(nombreCompleto(usuario.getCliente()))
                .clienteId(usuario.getCliente().getId())
                .build();
    }

    private static String nombreCompleto(Cliente cliente) {
        return Stream.of(cliente.getNombre(), cliente.getSegundoNombre(),
                        cliente.getApellidoPaterno(), cliente.getApellidoMaterno())
                .filter(parte -> parte != null && !parte.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
    }
}
