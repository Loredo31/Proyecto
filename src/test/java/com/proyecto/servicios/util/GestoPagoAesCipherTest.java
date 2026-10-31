package com.proyecto.servicios.util;

import com.proyecto.servicios.config.GestoPagoTxProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GestoPagoAesCipherTest {

    private static final String SANDBOX_KEY = "HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA=";
    private static final String SANDBOX_IV = "p5ldmBPdd/9pjC0bDC/nSg==";

    private static GestoPagoAesCipher cipherCon(String key, String iv) {
        GestoPagoTxProperties properties = new GestoPagoTxProperties();
        properties.getAes().setKey(key);
        properties.getAes().setIv(iv);
        return new GestoPagoAesCipher(properties);
    }

    @Test
    void testCifrar_CoincideConResultadoEsperadoDeLaDocumentacion() {
        String payload = "{'idProducto':582,'idServicio':133,'telefono':'5555555551',"
                + "'horaLocal':'20220101235900','upc':'test_UPC_83_182'}";
        String esperado = "+LWZ7icI60/DAI8ggv/SpDvJpFYeaYOZ5J7hPGK7g/87r9PRJfgFRBoBLYpBxW33ouvuwQPnhhKHXqzbiqLhMSH4gSH8"
                + "SMvW6PvHfeklNfTrZnbgWFWDPSy/nqFCP0B/gKavafHG0tW5UP4KPwyhECZK2pLT+I3qYAA85uQaAPQ=";

        GestoPagoAesCipher cipher = cipherCon(SANDBOX_KEY, SANDBOX_IV);

        assertEquals(esperado, cipher.cifrar(payload));
        assertEquals(payload, cipher.descifrar(esperado));
    }

    @Test
    void testCifrar_SinClave_FallaSinExponerDatos() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> cipherCon(null, SANDBOX_IV).cifrar("secreto"));
        assertFalse(ex.getMessage().contains("secreto"));
    }

    @Test
    void testCifrar_ClaveDeLongitudInvalida_Falla() {
        assertThrows(IllegalStateException.class, () -> cipherCon("Y29ydGE=", SANDBOX_IV).cifrar("x"));
    }
}
