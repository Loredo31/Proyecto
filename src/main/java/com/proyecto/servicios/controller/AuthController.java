package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.SeguridadProperties;
import com.proyecto.servicios.dto.ApiResponse;
import com.proyecto.servicios.dto.auth.ConfirmarCierreCorreoRequest;
import com.proyecto.servicios.dto.auth.ConfirmarRecuperacionRequest;
import com.proyecto.servicios.dto.auth.ConsultaSolicitudRequest;
import com.proyecto.servicios.dto.auth.LoginRequest;
import com.proyecto.servicios.dto.auth.LoginResponseDto;
import com.proyecto.servicios.dto.auth.RecuperacionRequest;
import com.proyecto.servicios.dto.auth.ReenviarCodigoRequest;
import com.proyecto.servicios.dto.auth.RefreshRequest;
import com.proyecto.servicios.dto.auth.RespuestaSolicitudRequest;
import com.proyecto.servicios.dto.auth.SegundoFactorDto;
import com.proyecto.servicios.dto.auth.SolicitudSesionDto;
import com.proyecto.servicios.dto.auth.VerificarCodigoRequest;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.enums.Plataforma;
import com.proyecto.servicios.seguridad.UsuarioAutenticado;
import com.proyecto.servicios.service.AuthService;
import com.proyecto.servicios.service.AuthService.ResultadoLogin;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Autenticación: contraseña → código por correo (2FA) → sesión única.
 * <ol>
 *   <li>{@code /login}: valida la contraseña y envía el código; responde un ticket.</li>
 *   <li>{@code /2fa/verificar}: con el ticket y el código emite los tokens, o responde 409 si hay otra sesión.</li>
 *   <li>Con 409, el otro dispositivo aprueba por WebSocket+REST o el usuario confirma por correo.</li>
 * </ol>
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SeguridadProperties properties;

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<SegundoFactorDto>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Intento de login desde plataforma {}", request.getPlataforma());
        return codigoEnviado(authService.login(request));
    }

    /** 200 con tokens; 409 con la solicitud de cierre si el usuario ya tiene una sesión activa. */
    @PostMapping(value = "/2fa/verificar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> verificarSegundoFactor(@Valid @RequestBody VerificarCodigoRequest request) {
        ResultadoLogin resultado = authService.verificarSegundoFactor(request);
        if (!resultado.autenticado()) {
            ApiResponseEnum conflicto = ApiResponseEnum.SESION_ACTIVA;
            return ResponseEntity.status(conflicto.getHttpStatus())
                    .body(ApiResponse.error(conflicto.getCode(), conflicto.getMessage(), resultado.solicitud()));
        }
        return conTokens(resultado, resultado.login());
    }

    @PostMapping(value = "/codigo/reenviar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<SegundoFactorDto>> reenviarCodigo(@Valid @RequestBody ReenviarCodigoRequest request) {
        return codigoEnviado(authService.reenviarCodigo(request.getTicket()));
    }

    /** El dispositivo nuevo consulta periódicamente si el otro dispositivo aprobó el cierre. */
    @PostMapping(value = "/sesion/solicitud/estado", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> estadoSolicitud(@Valid @RequestBody ConsultaSolicitudRequest request) {
        return conEstado(authService.consultarSolicitud(request));
    }

    /** El dispositivo con la sesión activa aprueba o rechaza cerrarla. */
    @PostMapping(value = "/sesion/solicitud/responder", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<SolicitudSesionDto>> responderSolicitud(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody RespuestaSolicitudRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.responderSolicitud(usuario, request)));
    }

    /** Respaldo si el otro dispositivo no responde: envía un código al correo. */
    @PostMapping(value = "/sesion/solicitud/correo", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<SegundoFactorDto>> cierrePorCorreo(@Valid @RequestBody ConsultaSolicitudRequest request) {
        return codigoEnviado(authService.solicitarCierrePorCorreo(request));
    }

    @PostMapping(value = "/sesion/solicitud/correo/confirmar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> confirmarCierrePorCorreo(@Valid @RequestBody ConfirmarCierreCorreoRequest request) {
        return conEstado(authService.confirmarCierrePorCorreo(request));
    }

    @PostMapping(value = "/recuperacion/solicitar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<SegundoFactorDto>> solicitarRecuperacion(@Valid @RequestBody RecuperacionRequest request) {
        SegundoFactorDto respuesta = authService.solicitarRecuperacion(request.getCorreo());
        return ResponseEntity.ok(ApiResponse.success(ApiResponseEnum.CODIGO_ENVIADO.getCode(),
                "Si el correo está registrado, recibirás un código para restablecer tu contraseña", respuesta));
    }

    @PostMapping(value = "/recuperacion/confirmar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> confirmarRecuperacion(@Valid @RequestBody ConfirmarRecuperacionRequest request) {
        authService.confirmarRecuperacion(request);
        return ResponseEntity.ok(ApiResponse.success("Contraseña actualizada; inicia sesión con tu nueva contraseña"));
    }

    /** Renueva el access token. La app móvil manda el refresh token en el cuerpo; la web, en la cookie. */
    @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> refresh(
            @Parameter(hidden = true) @CookieValue(name = "${seguridad.cookie.nombre:refresh_token}", required = false) String cookie,
            @Valid @RequestBody(required = false) RefreshRequest request) {
        String token = request != null && request.getRefreshToken() != null ? request.getRefreshToken() : cookie;
        ResultadoLogin resultado = authService.refrescar(token);
        return conTokens(resultado, resultado.login());
    }

    /**
     * Mantiene viva la sesión mientras el usuario interactúa con la app sin hacer otras peticiones.
     * El filtro JWT registra la actividad; si la sesión ya caducó por inactividad, responde 401 (40104).
     */
    @PostMapping(value = "/actividad", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> registrarActividad() {
        return ResponseEntity.ok(ApiResponse.success("Actividad registrada"));
    }

    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> logout(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        authService.logout(usuario);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieRefresh("", Duration.ZERO).toString())
                .body(ApiResponse.success("Sesión cerrada correctamente"));
    }

    private static ResponseEntity<ApiResponse<SegundoFactorDto>> codigoEnviado(SegundoFactorDto dto) {
        ApiResponseEnum estado = ApiResponseEnum.CODIGO_ENVIADO;
        return ResponseEntity.ok(ApiResponse.success(estado.getCode(), estado.getMessage(), dto));
    }

    private ResponseEntity<ApiResponse<Object>> conEstado(ResultadoLogin resultado) {
        SolicitudSesionDto estado = resultado.solicitud();
        if (resultado.autenticado()) {
            estado.setLogin(resultado.login());
            return conTokens(resultado, estado);
        }
        return ResponseEntity.ok(ApiResponse.success(estado));
    }

    /** En la web el refresh token se entrega solo en cookie HttpOnly (inaccesible para JavaScript). */
    private ResponseEntity<ApiResponse<Object>> conTokens(ResultadoLogin resultado, Object cuerpo) {
        LoginResponseDto login = resultado.login();
        if (resultado.plataforma() == Plataforma.WEB) {
            ResponseCookie cookie = cookieRefresh(login.getRefreshToken(), Duration.ofSeconds(login.getRefreshExpiraEnSegundos()));
            login.setRefreshToken(null);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(ApiResponse.success(cuerpo));
        }
        return ResponseEntity.ok(ApiResponse.success(cuerpo));
    }

    private ResponseCookie cookieRefresh(String valor, Duration maxAge) {
        SeguridadProperties.Cookie config = properties.getCookie();
        return ResponseCookie.from(config.getNombre(), valor)
                .httpOnly(true)
                .secure(config.isSecure())
                .sameSite(config.getSameSite())
                .path(config.getPath())
                .maxAge(maxAge)
                .build();
    }
}
