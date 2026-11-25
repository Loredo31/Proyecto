package com.proyecto.servicios.model.gestopago;

/**
 * Datos generados al reservar el pago que se envían a GestoPago.
 *
 * @param nombreProducto nombre del producto en el catálogo, guardado para el historial (puede ser nulo)
 */
public record DatosEnvioTx(String upc, String horaLocal, String telefono, String unidad, String nombreProducto) {

    public DatosEnvioTx(String upc, String horaLocal, String telefono, String unidad) {
        this(upc, horaLocal, telefono, unidad, null);
    }

    /** Copia con el nombre del producto (para el historial y el recibo). */
    public DatosEnvioTx conNombreProducto(String nombre) {
        return new DatosEnvioTx(upc, horaLocal, telefono, unidad, nombre);
    }
}
