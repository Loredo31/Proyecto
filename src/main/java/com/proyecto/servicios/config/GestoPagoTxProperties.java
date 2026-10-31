package com.proyecto.servicios.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "gestopago.tx")
public class GestoPagoTxProperties {

    private String url;

    private String path = "/sistema/service/sendTx.do";

    private String confirmPath = "/sistema/service/confirmTx.do";

    private String apiKey;

    private String unidad;

    private String codigoExito = "01";

    private List<String> codigosIndeterminados = new ArrayList<>(List.of("06", "82"));

    private List<String> codigosConfirmacionAplicada = new ArrayList<>(List.of("01", "06"));

    private List<String> codigosConfirmacionNoAplicada = new ArrayList<>(List.of("70"));

    private Duration esperaMinimaConfirmacion = Duration.ofSeconds(90);

    private Duration vigenciaConfirmacion = Duration.ofHours(24);

    private Duration ventanaDuplicados = Duration.ofMinutes(5);

    private int maxIntentosPin = 3;

    private String zonaHoraria = "America/Mexico_City";

    private String formatoHoraLocal = "yyyyMMddHHmmss";

    private boolean validarCatalogo = true;

    private Duration umbralConciliacion = Duration.ofMinutes(10);

    private Aes aes = new Aes();

    @Getter
    @Setter
    public static class Aes {

        private String transformation = "AES/CBC/PKCS5Padding";

        private String key;

        private String iv;

        private String encoding = "BASE64";

        @Override
        public String toString() {
            return "Aes[transformation=" + transformation + ", key=****, iv=****]";
        }
    }
}
