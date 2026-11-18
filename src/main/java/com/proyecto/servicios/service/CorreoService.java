package com.proyecto.servicios.service;

import com.proyecto.servicios.enums.PropositoCodigo;

/** Correos transaccionales de seguridad. */
public interface CorreoService {

    void enviarCodigo(String destino, PropositoCodigo proposito, String codigo, long minutosVigencia);

    void notificarCambioContrasena(String destino);
}
