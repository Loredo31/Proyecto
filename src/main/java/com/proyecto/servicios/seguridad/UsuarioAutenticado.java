package com.proyecto.servicios.seguridad;

/** Identidad extraída del access token y validada contra la sesión activa en el servidor. */
public record UsuarioAutenticado(Long usuarioId, Long clienteId, String correo, String sesionId) {

    @Override
    public String toString() {
        return "UsuarioAutenticado[usuarioId=" + usuarioId + "]";
    }
}
