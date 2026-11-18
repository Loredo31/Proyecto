package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.auth.ConfirmarCierreCorreoRequest;
import com.proyecto.servicios.dto.auth.ConfirmarRecuperacionRequest;
import com.proyecto.servicios.dto.auth.ConsultaSolicitudRequest;
import com.proyecto.servicios.dto.auth.LoginRequest;
import com.proyecto.servicios.dto.auth.LoginResponseDto;
import com.proyecto.servicios.dto.auth.RespuestaSolicitudRequest;
import com.proyecto.servicios.dto.auth.SegundoFactorDto;
import com.proyecto.servicios.dto.auth.SolicitudSesionDto;
import com.proyecto.servicios.dto.auth.VerificarCodigoRequest;
import com.proyecto.servicios.enums.Plataforma;
import com.proyecto.servicios.seguridad.UsuarioAutenticado;

/**
 * Flujo de acceso: contraseña → código por correo (2FA) → sesión única.
 * Toda validación ocurre en el servidor; el cliente solo transporta tickets opacos.
 */
public interface AuthService {

    /** Paso 1: valida la contraseña y envía el código 2FA al correo. */
    SegundoFactorDto login(LoginRequest request);

    /** Paso 2: valida el código. Emite los tokens o, si hay otra sesión activa, crea la solicitud de cierre. */
    ResultadoLogin verificarSegundoFactor(VerificarCodigoRequest request);

    /** Envía un código nuevo para un proceso en curso (2FA, recuperación o cierre por correo). */
    SegundoFactorDto reenviarCodigo(String ticket);

    /** Consulta (desde el dispositivo nuevo) si la solicitud fue aprobada; si lo fue, emite los tokens. */
    ResultadoLogin consultarSolicitud(ConsultaSolicitudRequest request);

    /** El dispositivo con la sesión activa aprueba o rechaza cerrarla. */
    SolicitudSesionDto responderSolicitud(UsuarioAutenticado usuario, RespuestaSolicitudRequest request);

    /** Respaldo cuando el otro dispositivo no responde: envía un código al correo para cerrar esa sesión. */
    SegundoFactorDto solicitarCierrePorCorreo(ConsultaSolicitudRequest request);

    /** Confirma con el código del correo: cierra la sesión anterior y emite los tokens del dispositivo nuevo. */
    ResultadoLogin confirmarCierrePorCorreo(ConfirmarCierreCorreoRequest request);

    /** Siempre responde igual, exista o no el correo, para no revelar qué cuentas existen. */
    SegundoFactorDto solicitarRecuperacion(String correo);

    void confirmarRecuperacion(ConfirmarRecuperacionRequest request);

    ResultadoLogin refrescar(String refreshToken);

    void logout(UsuarioAutenticado usuario);

    /**
     * @param login     tokens emitidos (incluye el refresh token en claro; el controlador decide si va en cookie)
     * @param solicitud solicitud de cierre de la otra sesión o su estado
     */
    record ResultadoLogin(LoginResponseDto login, SolicitudSesionDto solicitud, Plataforma plataforma) {

        public boolean autenticado() {
            return login != null;
        }
    }
}
