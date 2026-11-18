package com.proyecto.servicios.service;

import com.proyecto.servicios.config.CorreoProperties;
import com.proyecto.servicios.enums.PropositoCodigo;
import com.proyecto.servicios.exception.CorreoNoEnviadoException;
import com.proyecto.servicios.service.Impl.BrevoCorreoService;
import com.proyecto.servicios.util.DatosSensiblesUtil;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrevoCorreoServiceTest {

    private static CorreoProperties propiedades(String apiKey) {
        CorreoProperties p = new CorreoProperties();
        p.setBrevoApiKey(apiKey);
        p.setRemitente("31loredo@gmail.com");
        p.setNombreRemitente("UTNG");
        return p;
    }

    @Test
    void enviaElCodigoConLaApiDeBrevo() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.brevo.com/v3");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        BrevoCorreoService service = new BrevoCorreoService(propiedades("xkeysib-prueba"), builder.build());
        servidor.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "xkeysib-prueba"))
                .andExpect(jsonPath("$.sender.email").value("31loredo@gmail.com"))
                .andExpect(jsonPath("$.to[0].email").value("juan@example.com"))
                .andExpect(jsonPath("$.subject").value(not(containsString("123456"))))
                .andExpect(content().string(containsString("123456")))
                .andRespond(withSuccess("{\"messageId\":\"m1\"}", MediaType.APPLICATION_JSON));

        service.enviarCodigo("juan@example.com", PropositoCodigo.LOGIN, "123456", 3);

        servidor.verify();
    }

    @Test
    void avisoDeCambioYMensajesPorProposito() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.brevo.com/v3");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        BrevoCorreoService service = new BrevoCorreoService(propiedades("k"), builder.build());
        servidor.expect(content().string(containsString("restablecer"))).andRespond(withSuccess());
        servidor.expect(content().string(containsString("cerrar tu sesi"))).andRespond(withSuccess());
        servidor.expect(content().string(containsString("contrase"))).andRespond(withSuccess());

        service.enviarCodigo("a@b.com", PropositoCodigo.RECUPERACION, "111111", 3);
        service.enviarCodigo("a@b.com", PropositoCodigo.CIERRE_SESION, "222222", 3);
        service.notificarCambioContrasena("a@b.com");

        servidor.verify();
    }

    @Test
    void errorDeBrevoSeTraduceAExcepcionDeCorreo() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.brevo.com/v3");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        BrevoCorreoService service = new BrevoCorreoService(propiedades("k"), builder.build());
        servidor.expect(requestTo("https://api.brevo.com/v3/smtp/email")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(CorreoNoEnviadoException.class,
                () -> service.enviarCodigo("a@b.com", PropositoCodigo.LOGIN, "123456", 3));
    }

    @Test
    void sinConfiguracionNoIntentaEnviar() {
        BrevoCorreoService service = new BrevoCorreoService(propiedades(" "));

        assertThrows(CorreoNoEnviadoException.class, () -> service.notificarCambioContrasena("a@b.com"));
        assertFalse(propiedades(null).configurado());
        assertTrue(propiedades("x").configurado());
        assertFalse(propiedades("secreta").toString().contains("secreta"));
    }

    @Test
    void enmascararCorreo() {
        assertEquals("j***n@example.com", DatosSensiblesUtil.enmascararCorreo("juan@example.com"));
        assertEquals("a***@b.com", DatosSensiblesUtil.enmascararCorreo("ab@b.com"));
        assertEquals("***", DatosSensiblesUtil.enmascararCorreo("sinarroba"));
        assertNull(DatosSensiblesUtil.enmascararCorreo(null));
    }
}
