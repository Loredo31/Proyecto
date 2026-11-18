package com.proyecto.servicios.service;

import com.proyecto.servicios.config.SeguridadProperties;
import com.proyecto.servicios.dto.auth.ConfirmarCierreCorreoRequest;
import com.proyecto.servicios.dto.auth.ConfirmarRecuperacionRequest;
import com.proyecto.servicios.dto.auth.ConsultaSolicitudRequest;
import com.proyecto.servicios.dto.auth.LoginRequest;
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
import com.proyecto.servicios.service.AuthService.ResultadoLogin;
import com.proyecto.servicios.service.CodigoVerificacionService.Emision;
import com.proyecto.servicios.service.Impl.AuthServiceImpl;
import com.proyecto.servicios.service.SesionService.TokensSesion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String CORREO = "juan@example.com";
    private static final String CLAVE = "Password123!";
    private static final Emision EMISION = new Emision("ticket-1", "j***n@example.com", 180);

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private SolicitudSesionRepository solicitudRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SesionService sesionService;
    @Mock
    private CodigoVerificacionService codigoService;
    @Mock
    private CorreoService correoService;
    @Mock
    private NotificadorSesion notificador;

    private AuthServiceImpl authService;
    private Usuario usuario;
    private LoginRequest request;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenReturn("$hash-ficticio");
        authService = new AuthServiceImpl(usuarioRepository, solicitudRepository, passwordEncoder,
                sesionService, codigoService, correoService, notificador, new SeguridadProperties());

        usuario = Usuario.builder().id(5L).correo(CORREO).password("$hash").activo(true)
                .cliente(Cliente.builder().id(1L).eliminadoLogico(false).build()).build();
        request = LoginRequest.builder().correo(CORREO).password(CLAVE).plataforma(Plataforma.MOVIL)
                .dispositivo("Pixel <7>").build();
    }

    private TokensSesion tokens(Plataforma plataforma) {
        return new TokensSesion(usuario, plataforma, "access", 900, "refresh", 1800);
    }

    private CodigoVerificacion codigo(PropositoCodigo proposito, String referencia) {
        return CodigoVerificacion.builder().id("C1").usuario(usuario).proposito(proposito)
                .plataforma(Plataforma.MOVIL).dispositivo("Pixel 7").referencia(referencia).build();
    }

    // ---------- Paso 1: contraseña ----------

    @Test
    void login_ContrasenaCorrectaEnviaCodigoSinEmitirTokens() {
        usuario.setIntentosFallidos(2);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(CLAVE, "$hash")).thenReturn(true);
        when(codigoService.emitir(usuario, PropositoCodigo.LOGIN, Plataforma.MOVIL, "Pixel 7", null)).thenReturn(EMISION);

        SegundoFactorDto dto = authService.login(request);

        assertEquals("ticket-1", dto.getTicket());
        assertEquals("j***n@example.com", dto.getCorreoEnmascarado());
        assertEquals(2, usuario.getIntentosFallidos(), "los intentos se reinician solo al completar el 2FA");
        verify(sesionService, never()).crear(any(), any(), any());
    }

    @Test
    void login_CorreoInexistenteRespondeGenerico() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(request));
        verify(passwordEncoder).matches(CLAVE, "$hash-ficticio");
    }

    @Test
    void login_ContrasenaIncorrectaSumaIntento() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(CLAVE, "$hash")).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(request));
        assertEquals(1, usuario.getIntentosFallidos());
        verify(codigoService, never()).emitir(any(), any(), any(), any(), any());
    }

    @Test
    void login_QuintoIntentoBloqueaUnaHoraYCierraSesiones() {
        usuario.setIntentosFallidos(4);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(CLAVE, "$hash")).thenReturn(false);

        assertThrows(CuentaBloqueadaTemporalException.class, () -> authService.login(request));
        assertTrue(usuario.getBloqueadoHasta().isAfter(LocalDateTime.now().plusMinutes(59)));
        verify(sesionService).cerrarTodas(eq(5L), anyString());
    }

    @Test
    void login_CuentaBloqueadaRechazaAunConContrasenaCorrecta() {
        usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(30));
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));

        assertThrows(CuentaBloqueadaTemporalException.class, () -> authService.login(request));
        verify(passwordEncoder, never()).matches(CLAVE, "$hash");
    }

    @Test
    void login_UsuarioInactivoOClienteEliminado() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(CLAVE, "$hash")).thenReturn(true);

        usuario.setActivo(false);
        assertThrows(UsuarioInactivoException.class, () -> authService.login(request));
        usuario.setActivo(true);
        usuario.getCliente().setEliminadoLogico(true);
        assertThrows(UsuarioInactivoException.class, () -> authService.login(request));
    }

    // ---------- Paso 2: código ----------

    @Test
    void verificar_CodigoCorrectoSinSesionPreviaEmiteTokens() {
        usuario.setIntentosFallidos(3);
        CodigoVerificacion c = codigo(PropositoCodigo.LOGIN, null);
        when(codigoService.buscarVigente("ticket-1", PropositoCodigo.LOGIN)).thenReturn(c);
        when(codigoService.verificar(c, "123456")).thenReturn(true);
        when(sesionService.buscarVigente(5L)).thenReturn(Optional.empty());
        when(sesionService.crear(usuario, Plataforma.MOVIL, "Pixel 7")).thenReturn(tokens(Plataforma.MOVIL));

        ResultadoLogin r = authService.verificarSegundoFactor(new VerificarCodigoRequest("ticket-1", "123456"));

        assertTrue(r.autenticado());
        assertEquals("access", r.login().getAccessToken());
        assertEquals(1L, r.login().getClienteId());
        assertEquals(0, usuario.getIntentosFallidos());
    }

    @Test
    void verificar_CodigoIncorrectoSumaIntentoDeLaCuenta() {
        CodigoVerificacion c = codigo(PropositoCodigo.LOGIN, null);
        when(codigoService.buscarVigente("ticket-1", PropositoCodigo.LOGIN)).thenReturn(c);
        when(codigoService.verificar(c, "000000")).thenReturn(false);
        VerificarCodigoRequest req = new VerificarCodigoRequest("ticket-1", "000000");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.verificarSegundoFactor(req));

        assertEquals(ApiResponseEnum.CODIGO_INVALIDO, ex.getResponseStatus());
        assertTrue(ex.getMessage().contains("4"));
        assertEquals(1, usuario.getIntentosFallidos());
    }

    @Test
    void verificar_CuentaBloqueadaNoValidaElCodigo() {
        usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(10));
        when(codigoService.buscarVigente("ticket-1", PropositoCodigo.LOGIN)).thenReturn(codigo(PropositoCodigo.LOGIN, null));
        VerificarCodigoRequest req = new VerificarCodigoRequest("ticket-1", "123456");

        assertThrows(CuentaBloqueadaTemporalException.class, () -> authService.verificarSegundoFactor(req));
        verify(codigoService, never()).verificar(any(), any());
    }

    @Test
    void verificar_ConSesionActivaCreaSolicitudYNotifica() {
        CodigoVerificacion c = codigo(PropositoCodigo.LOGIN, null);
        when(codigoService.buscarVigente("ticket-1", PropositoCodigo.LOGIN)).thenReturn(c);
        when(codigoService.verificar(c, "123456")).thenReturn(true);
        Sesion actual = Sesion.builder().id("S-1").plataforma(Plataforma.WEB).dispositivo("Chrome").build();
        SolicitudSesion anterior = SolicitudSesion.builder().estado(EstadoSolicitudSesion.PENDIENTE).build();
        when(sesionService.buscarVigente(5L)).thenReturn(Optional.of(actual));
        when(solicitudRepository.findByUsuarioIdAndEstado(5L, EstadoSolicitudSesion.PENDIENTE)).thenReturn(List.of(anterior));
        when(solicitudRepository.save(any(SolicitudSesion.class))).thenAnswer(i -> i.getArgument(0));

        ResultadoLogin r = authService.verificarSegundoFactor(new VerificarCodigoRequest("ticket-1", "123456"));

        assertFalse(r.autenticado());
        SolicitudSesionDto solicitud = r.solicitud();
        assertEquals(Plataforma.WEB, solicitud.getPlataformaSesionActiva());
        assertEquals(EstadoSolicitudSesion.EXPIRADA, anterior.getEstado());
        verify(notificador).notificarSolicitudCierre(eq("S-1"), anyString(), eq(Plataforma.MOVIL), eq("Pixel 7"), eq(120L));
        ArgumentCaptor<SolicitudSesion> guardada = ArgumentCaptor.forClass(SolicitudSesion.class);
        verify(solicitudRepository).save(guardada.capture());
        assertTrue(TokenSeguro.coincide(solicitud.getSecreto(), guardada.getValue().getSecretoHash()));
    }

    @Test
    void reenviarCodigo_DelegaEnElServicio() {
        when(codigoService.reenviar("ticket-1")).thenReturn(EMISION);
        assertEquals("ticket-1", authService.reenviarCodigo("ticket-1").getTicket());
    }

    // ---------- Solicitudes de cierre ----------

    private SolicitudSesion solicitud(EstadoSolicitudSesion estado, LocalDateTime expira) {
        return SolicitudSesion.builder().id("SOL-1").usuario(usuario).sesionActualId("S-1")
                .plataforma(Plataforma.WEB).dispositivo("Chrome").secretoHash(TokenSeguro.huella("sec"))
                .estado(estado).expiraEn(expira).build();
    }

    @Test
    void consultarSolicitud_AprobadaEmiteTokensUnaSolaVez() {
        SolicitudSesion s = solicitud(EstadoSolicitudSesion.APROBADA, LocalDateTime.now().plusMinutes(1));
        when(solicitudRepository.findByIdForUpdate("SOL-1")).thenReturn(Optional.of(s));
        when(sesionService.buscarVigente(5L)).thenReturn(Optional.empty());
        when(sesionService.crear(usuario, Plataforma.WEB, "Chrome")).thenReturn(tokens(Plataforma.WEB));

        ResultadoLogin r = authService.consultarSolicitud(new ConsultaSolicitudRequest("SOL-1", "sec"));

        assertTrue(r.autenticado());
        assertEquals(EstadoSolicitudSesion.CONSUMIDA, s.getEstado());
    }

    @Test
    void consultarSolicitud_PendienteVencidaOEnEspera() {
        SolicitudSesion vencida = solicitud(EstadoSolicitudSesion.PENDIENTE, LocalDateTime.now().minusSeconds(1));
        when(solicitudRepository.findByIdForUpdate("SOL-1")).thenReturn(Optional.of(vencida));
        assertEquals(EstadoSolicitudSesion.EXPIRADA,
                authService.consultarSolicitud(new ConsultaSolicitudRequest("SOL-1", "sec")).solicitud().getEstado());

        SolicitudSesion espera = solicitud(EstadoSolicitudSesion.PENDIENTE, LocalDateTime.now().plusMinutes(1));
        when(solicitudRepository.findByIdForUpdate("SOL-1")).thenReturn(Optional.of(espera));
        assertEquals(EstadoSolicitudSesion.PENDIENTE,
                authService.consultarSolicitud(new ConsultaSolicitudRequest("SOL-1", "sec")).solicitud().getEstado());
    }

    @Test
    void consultarSolicitud_AprobadaPeroOtraSesionYaInicio() {
        SolicitudSesion s = solicitud(EstadoSolicitudSesion.APROBADA, LocalDateTime.now().plusMinutes(1));
        when(solicitudRepository.findByIdForUpdate("SOL-1")).thenReturn(Optional.of(s));
        when(sesionService.buscarVigente(5L)).thenReturn(Optional.of(new Sesion()));

        assertFalse(authService.consultarSolicitud(new ConsultaSolicitudRequest("SOL-1", "sec")).autenticado());
        assertEquals(EstadoSolicitudSesion.EXPIRADA, s.getEstado());
    }

    @Test
    void consultarSolicitud_SecretoIncorrecto() {
        when(solicitudRepository.findByIdForUpdate("SOL-1"))
                .thenReturn(Optional.of(solicitud(EstadoSolicitudSesion.APROBADA, LocalDateTime.now().plusMinutes(1))));
        ConsultaSolicitudRequest req = new ConsultaSolicitudRequest("SOL-1", "otro");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.consultarSolicitud(req));
        assertEquals(ApiResponseEnum.SOLICITUD_SESION_INVALIDA, ex.getResponseStatus());
    }

    private UsuarioAutenticado dueno() {
        return new UsuarioAutenticado(5L, 1L, CORREO, "S-1");
    }

    @Test
    void responderSolicitud_AprobarORechazar() {
        when(solicitudRepository.findByIdForUpdate("SOL-1"))
                .thenReturn(Optional.of(solicitud(EstadoSolicitudSesion.PENDIENTE, LocalDateTime.now().plusMinutes(1))));
        assertEquals(EstadoSolicitudSesion.APROBADA,
                authService.responderSolicitud(dueno(), new RespuestaSolicitudRequest("SOL-1", true)).getEstado());
        verify(sesionService).cerrar(eq("S-1"), eq(EstadoSesion.REVOCADA), anyString());

        when(solicitudRepository.findByIdForUpdate("SOL-1"))
                .thenReturn(Optional.of(solicitud(EstadoSolicitudSesion.PENDIENTE, LocalDateTime.now().plusMinutes(1))));
        assertEquals(EstadoSolicitudSesion.RECHAZADA,
                authService.responderSolicitud(dueno(), new RespuestaSolicitudRequest("SOL-1", false)).getEstado());
    }

    @Test
    void responderSolicitud_DesdeOtraSesionNoEsValido() {
        when(solicitudRepository.findByIdForUpdate("SOL-1"))
                .thenReturn(Optional.of(solicitud(EstadoSolicitudSesion.PENDIENTE, LocalDateTime.now().plusMinutes(1))));
        UsuarioAutenticado otra = new UsuarioAutenticado(5L, 1L, CORREO, "S-2");
        RespuestaSolicitudRequest req = new RespuestaSolicitudRequest("SOL-1", true);

        assertThrows(BusinessException.class, () -> authService.responderSolicitud(otra, req));
        verify(sesionService, never()).cerrar(any(), any(), any());
    }

    // ---------- Cierre por correo ----------

    @Test
    void cierrePorCorreo_EnviaCodigoYExtiendeLaSolicitud() {
        SolicitudSesion s = solicitud(EstadoSolicitudSesion.PENDIENTE, LocalDateTime.now().plusSeconds(30));
        when(solicitudRepository.findByIdForUpdate("SOL-1")).thenReturn(Optional.of(s));
        when(codigoService.emitir(usuario, PropositoCodigo.CIERRE_SESION, Plataforma.WEB, "Chrome", "SOL-1")).thenReturn(EMISION);

        assertEquals("ticket-1", authService.solicitarCierrePorCorreo(new ConsultaSolicitudRequest("SOL-1", "sec")).getTicket());
        assertTrue(s.getExpiraEn().isAfter(LocalDateTime.now().plusMinutes(2)));
    }

    @Test
    void cierrePorCorreo_ConfirmarCierraLaSesionYEmiteTokens() {
        SolicitudSesion s = solicitud(EstadoSolicitudSesion.PENDIENTE, LocalDateTime.now().plusMinutes(2));
        CodigoVerificacion c = codigo(PropositoCodigo.CIERRE_SESION, "SOL-1");
        when(solicitudRepository.findByIdForUpdate("SOL-1")).thenReturn(Optional.of(s));
        when(codigoService.buscarVigente("ticket-1", PropositoCodigo.CIERRE_SESION)).thenReturn(c);
        when(codigoService.verificar(c, "123456")).thenReturn(true);
        when(sesionService.buscarVigente(5L)).thenReturn(Optional.empty());
        when(sesionService.crear(usuario, Plataforma.WEB, "Chrome")).thenReturn(tokens(Plataforma.WEB));

        ResultadoLogin r = authService.confirmarCierrePorCorreo(
                new ConfirmarCierreCorreoRequest("SOL-1", "sec", "ticket-1", "123456"));

        assertTrue(r.autenticado());
        verify(sesionService).cerrar(eq("S-1"), eq(EstadoSesion.REVOCADA), anyString());
        assertEquals(EstadoSolicitudSesion.CONSUMIDA, s.getEstado());
    }

    @Test
    void cierrePorCorreo_CodigoDeOtraSolicitudOVencida() {
        SolicitudSesion s = solicitud(EstadoSolicitudSesion.PENDIENTE, LocalDateTime.now().plusMinutes(2));
        when(solicitudRepository.findByIdForUpdate("SOL-1")).thenReturn(Optional.of(s));
        when(codigoService.buscarVigente("ticket-1", PropositoCodigo.CIERRE_SESION))
                .thenReturn(codigo(PropositoCodigo.CIERRE_SESION, "OTRA"));
        ConfirmarCierreCorreoRequest req = new ConfirmarCierreCorreoRequest("SOL-1", "sec", "ticket-1", "123456");
        assertThrows(BusinessException.class, () -> authService.confirmarCierrePorCorreo(req));

        s.setEstado(EstadoSolicitudSesion.RECHAZADA);
        ConsultaSolicitudRequest consulta = new ConsultaSolicitudRequest("SOL-1", "sec");
        assertThrows(BusinessException.class, () -> authService.solicitarCierrePorCorreo(consulta));
    }

    // ---------- Recuperación de contraseña ----------

    @Test
    void recuperacion_CorreoRegistradoEnviaCodigo() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(codigoService.emitir(usuario, PropositoCodigo.RECUPERACION, null, null, null)).thenReturn(EMISION);

        assertEquals("ticket-1", authService.solicitarRecuperacion(" " + CORREO + " ").getTicket());
    }

    @Test
    void recuperacion_CorreoNoRegistradoOBloqueadoRespondeIgual() {
        when(usuarioRepository.findByCorreo("nadie@example.com")).thenReturn(Optional.empty());
        usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(5));
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));

        SegundoFactorDto falso = authService.solicitarRecuperacion("nadie@example.com");
        SegundoFactorDto bloqueado = authService.solicitarRecuperacion(CORREO);

        assertNotNull(falso.getTicket());
        assertNotEquals(falso.getTicket(), bloqueado.getTicket());
        assertEquals("n***e@example.com", falso.getCorreoEnmascarado());
        assertEquals(180L, falso.getExpiraEnSegundos());
        verify(codigoService, never()).emitir(any(), any(), any(), any(), any());
    }

    @Test
    void recuperacion_ConfirmarCambiaContrasenaYCierraSesiones() {
        CodigoVerificacion c = codigo(PropositoCodigo.RECUPERACION, null);
        when(codigoService.buscarVigente("ticket-1", PropositoCodigo.RECUPERACION)).thenReturn(c);
        when(codigoService.verificar(c, "123456")).thenReturn(true);
        when(passwordEncoder.encode("NuevaClave1!")).thenReturn("$nuevo");

        authService.confirmarRecuperacion(new ConfirmarRecuperacionRequest("ticket-1", "123456", "NuevaClave1!"));

        assertEquals("$nuevo", usuario.getPassword());
        verify(sesionService).cerrarTodas(eq(5L), anyString());
        verify(correoService).notificarCambioContrasena(CORREO);
    }

    @Test
    void recuperacion_AvisoFallidoNoRevierteElCambio() {
        CodigoVerificacion c = codigo(PropositoCodigo.RECUPERACION, null);
        when(codigoService.buscarVigente("ticket-1", PropositoCodigo.RECUPERACION)).thenReturn(c);
        when(codigoService.verificar(c, "123456")).thenReturn(true);
        when(passwordEncoder.encode("NuevaClave1!")).thenReturn("$nuevo");
        doThrow(new IllegalStateException("brevo caído")).when(correoService).notificarCambioContrasena(CORREO);

        authService.confirmarRecuperacion(new ConfirmarRecuperacionRequest("ticket-1", "123456", "NuevaClave1!"));

        assertEquals("$nuevo", usuario.getPassword());
    }

    @Test
    void recuperacion_ContrasenaDebilSeRechazaAntesDeUsarElCodigo() {
        ConfirmarRecuperacionRequest req = new ConfirmarRecuperacionRequest("ticket-1", "123456", "debil");

        assertThrows(ContrasenaInvalidaException.class, () -> authService.confirmarRecuperacion(req));
        verify(codigoService, never()).buscarVigente(any(), any());
    }

    // ---------- Tokens ----------

    @Test
    void refrescarYLogout_DelegaEnSesiones() {
        when(sesionService.refrescar("r1")).thenReturn(tokens(Plataforma.MOVIL));

        assertEquals("access", authService.refrescar("r1").login().getAccessToken());
        authService.logout(dueno());

        verify(sesionService).cerrar(eq("S-1"), eq(EstadoSesion.CERRADA), anyString());
        assertNull(authService.refrescar("r1").solicitud());
        verify(codigoService, never()).emitir(any(), any(), any(), any(), isNull());
    }
}
