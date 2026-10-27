package com.proyecto.servicios.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuración de sendTx.do / confirmTx.do de GestoPago (PuntoRed API Technical Specification).
 * Los valores sensibles (clave e IV AES, X-API-Key) se leen exclusivamente de variables de entorno.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "gestopago.tx")
public class GestoPagoTxProperties {

    private String url;

    private String path = "/sistema/service/sendTx.do";

    private String confirmPath = "/sistema/service/confirmTx.do";

    /** Header X-API-Key; opcional según el proveedor. */
    private String apiKey;

    /** Unidad comercial o sucursal (campo "unidad", 3 a 25 caracteres). */
    private String unidad;

    private String codigoExito = "01";

    /**
     * Códigos de sendTx que no confirman ni rechazan el pago: 06 (duplicado, la autorización puede ser
     * de otra transacción) y 82 (timeout del proveedor). Se resuelven con confirmTx.
     */
    private List<String> codigosIndeterminados = new ArrayList<>(List.of("06", "82"));

    /** Códigos de confirmTx que indican que la transacción SÍ fue aplicada. */
    private List<String> codigosConfirmacionAplicada = new ArrayList<>(List.of("01", "06"));

    /** Códigos de confirmTx que indican que la transacción NO fue aplicada. */
    private List<String> codigosConfirmacionNoAplicada = new ArrayList<>(List.of("70"));

    /** confirmTx no debe ejecutarse antes de 62 s tras el sendTx; se deja margen. */
    private Duration esperaMinimaConfirmacion = Duration.ofSeconds(90);

    /** confirmTx solo es válido durante 24 h tras el sendTx. */
    private Duration vigenciaConfirmacion = Duration.ofHours(24);

    private Duration ventanaDuplicados = Duration.ofMinutes(5);

    private int maxIntentosPin = 3;

    private String zonaHoraria = "America/Mexico_City";

    private String formatoHoraLocal = "yyyyMMddHHmmss";

    private boolean validarCatalogo = true;

    /** Tiempo tras el cual una transacción atascada en PROCESANDO pasa a PENDIENTE_CONCILIACION. */
    private Duration umbralConciliacion = Duration.ofMinutes(10);

    private Aes aes = new Aes();

    @Getter
    @Setter
    public static class Aes {

        private String transformation = "AES/CBC/PKCS5Padding";

        private String key;

        private String iv;

        /** Codificación de key/iv en la variable de entorno: BASE64 (formato del proveedor), HEX o RAW. */
        private String encoding = "BASE64";

        @Override
        public String toString() {
            return "Aes[transformation=" + transformation + ", key=****, iv=****]";
        }
    }
}
