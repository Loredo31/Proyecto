package com.proyecto.servicios.util;

/**
 * URLs públicas de imágenes publicadas por GestoPago (documentadas en su colección de Postman):
 * logo del servicio e imagen de ayuda que indica dónde encontrar la referencia en el recibo.
 */
public final class ImagenesGestoPago {

    private static final String BASE = "https://gestopago.portalventas.net/sistema/images/gestopago";

    private ImagenesGestoPago() {
    }

    public static String logoServicio(Integer idServicio) {
        return idServicio == null ? null : BASE + "/servicios/" + idServicio + ".png";
    }

    public static String imagenReferencia(Integer idServicio) {
        return idServicio == null ? null : BASE + "/referencias/" + idServicio + ".jpg";
    }
}
