package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.entity.seguridad.Sesion;
import com.proyecto.servicios.enums.EstadoSesion;
import com.proyecto.servicios.enums.Plataforma;

import java.util.Optional;

public interface SesionService {

    /** Crea la sesión del usuario y emite su access token y refresh token. */
    TokensSesion crear(Usuario usuario, Plataforma plataforma, String dispositivo);

    /** Sesión ACTIVA y vigente del usuario; las que caducaron se marcan como EXPIRADA. */
    Optional<Sesion> buscarVigente(Long usuarioId);

    boolean estaActiva(String sesionId);

    /** Rota el refresh token. Si se presenta un refresh token ya usado, revoca la sesión (posible robo). */
    TokensSesion refrescar(String refreshToken);

    void cerrar(String sesionId, EstadoSesion estadoFinal, String motivo);

    /** Cierra todas las sesiones activas del usuario (p. ej. tras cambiar la contraseña). */
    void cerrarTodas(Long usuarioId, String motivo);

    record TokensSesion(Usuario usuario, Plataforma plataforma, String accessToken, long accessSegundos,
                        String refreshToken, long refreshSegundos) {

        @Override
        public String toString() {
            return "TokensSesion[plataforma=" + plataforma + ", accessToken=****, refreshToken=****]";
        }
    }
}
