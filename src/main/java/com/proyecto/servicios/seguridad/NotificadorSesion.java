package com.proyecto.servicios.seguridad;

import com.proyecto.servicios.enums.Plataforma;

/** Envía avisos en tiempo real al dispositivo dueño de una sesión (implementado con WebSocket). */
public interface NotificadorSesion {

    void notificarSolicitudCierre(String sesionId, String solicitudId, Plataforma plataforma,
                                  String dispositivo, long expiraEnSegundos);

    void notificarSesionCerrada(String sesionId, String motivo);
}
