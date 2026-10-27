package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.gestopago.GestoPagoToken;

import java.util.Optional;

public interface GestoPagoTokenService {

    void renovarToken();

    Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidor, String codigoDispositivo);

    /**
     * Devuelve un Bearer token vigente: memoria → BD → autenticación contra GestoPago, en ese orden.
     * El token se reutiliza durante su ciclo de vida (24 h por defecto).
     *
     * @throws IllegalStateException si no es posible obtener un token
     */
    String obtenerTokenVigente();

    /** Descarta el token actual (p. ej. tras un 401 del proveedor) para forzar uno nuevo. */
    void invalidarToken();
}
