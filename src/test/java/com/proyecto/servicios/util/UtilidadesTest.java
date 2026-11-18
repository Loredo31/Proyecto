package com.proyecto.servicios.util;

import com.proyecto.servicios.config.SeguridadProperties;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.seguridad.TokenSeguro;
import com.proyecto.servicios.seguridad.UsuarioAutenticado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilidadesTest {

    private static final String SECRETO = "secreto-de-pruebas-con-longitud-suficiente-0123456789";

    private static JwtTokenService jwt(String secreto) {
        return new JwtTokenService(secreto, new SeguridadProperties());
    }

    private static Usuario usuario(Long clienteId) {
        return Usuario.builder().id(3L).correo("ana@example.com").cliente(Cliente.builder().id(clienteId).build()).build();
    }

    @Test
    void jwt_GeneraYValidaAccessToken() {
        JwtTokenService service = jwt(SECRETO);

        String token = service.generarAccessToken(usuario(7L), "S-1");
        UsuarioAutenticado u = service.validar(token).orElseThrow();

        assertEquals(3L, u.usuarioId());
        assertEquals(7L, u.clienteId());
        assertEquals("ana@example.com", u.correo());
        assertEquals("S-1", u.sesionId());
        assertFalse(u.toString().contains("ana@example.com"));
        assertEquals(900L, service.getAccessTokenSegundos());
    }


    @Test
    void jwt_TokenInvalidoOFirmadoConOtroSecreto() {
        JwtTokenService emisor = jwt(SECRETO);
        JwtTokenService otro = jwt("otro-secreto-distinto-con-longitud-suficiente-987654");

        String token = emisor.generarAccessToken(usuario(1L), "S-1");

        assertTrue(otro.validar(token).isEmpty());
        assertTrue(emisor.validar("no-es-un-jwt").isEmpty());
        assertTrue(emisor.validar(null).isEmpty());
        assertTrue(emisor.validar(" ").isEmpty());
    }

    @Test
    void jwt_TokenSinClaimsDeSesionEsRechazado() {
        String sinSesion = com.auth0.jwt.JWT.create().withIssuer("servicios-pagos").withClaim("typ", "access")
                .sign(com.auth0.jwt.algorithms.Algorithm.HMAC256(SECRETO));

        assertTrue(jwt(SECRETO).validar(sinSesion).isEmpty());
    }

    @Test
    void jwt_TokenExpiradoEsRechazado() {
        SeguridadProperties props = new SeguridadProperties();
        props.setAccessTokenTtl(java.time.Duration.ofSeconds(-5));
        JwtTokenService service = new JwtTokenService(SECRETO, props);

        assertTrue(service.validar(service.generarAccessToken(usuario(1L), "S")).isEmpty());
    }

    @Test
    void jwt_SinSecretoConfiguradoGeneraUnoAleatorio() {
        JwtTokenService a = jwt("");
        JwtTokenService b = jwt(null);

        String token = a.generarAccessToken(usuario(1L), "S");

        assertTrue(a.validar(token).isPresent());
        assertTrue(b.validar(token).isEmpty());
    }

    @Test
    void tokenSeguro_GeneraHuellaYCompara() {
        String token = TokenSeguro.generar();

        assertEquals(43, token.length());
        assertNotEquals(token, TokenSeguro.generar());
        assertEquals(64, TokenSeguro.huella(token).length());
        assertTrue(TokenSeguro.coincide(token, TokenSeguro.huella(token)));
        assertFalse(TokenSeguro.coincide("otro", TokenSeguro.huella(token)));
        assertFalse(TokenSeguro.coincide(null, "x"));
        assertFalse(TokenSeguro.coincide("x", null));
        assertThrows(IllegalArgumentException.class, () -> TokenSeguro.huella(null));
    }

    @Test
    void generadorCuenta_GeneraNumeroValidoLuhn() {
        GeneradorCuentaBancaria generador = new GeneradorCuentaBancaria();

        String numero = generador.generarNumeroCuenta();

        assertEquals(16, numero.length());
        assertTrue(numero.startsWith("415231"));
        assertTrue(GeneradorCuentaBancaria.esValidoLuhn(numero));
    }

    @Test
    void generadorCuenta_ValidacionLuhn() {
        assertTrue(GeneradorCuentaBancaria.esValidoLuhn("4111111111111111"));
        assertFalse(GeneradorCuentaBancaria.esValidoLuhn("4111111111111112"));
        assertFalse(GeneradorCuentaBancaria.esValidoLuhn("41111111111111a1"));
        assertFalse(GeneradorCuentaBancaria.esValidoLuhn("123"));
        assertFalse(GeneradorCuentaBancaria.esValidoLuhn(null));
    }

    @Test
    void generadorCuenta_NumerosDistintos() {
        GeneradorCuentaBancaria generador = new GeneradorCuentaBancaria();
        assertNotEquals(generador.generarNumeroCuenta() + "x", generador.generarNumeroCuenta());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Password123!", "Abcdefg1$", "Zz9-zzzz", "aA1\\aaaa"})
    void validadorContrasena_Validas(String valor) {
        assertTrue(ValidadorContrasena.cumpleReglas(valor));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Ab1!", "password123!", "PASSWORD123!", "Password!!!", "Password123",
            "Password 123", "Pass\n123!word", "Pass\r123!word"})
    void validadorContrasena_Invalidas(String valor) {
        assertFalse(ValidadorContrasena.cumpleReglas(valor));
    }

    @Test
    void datosSensibles_Enmascarar() {
        assertNull(DatosSensiblesUtil.enmascarar(null));
        assertEquals("***", DatosSensiblesUtil.enmascarar("123"));
        assertEquals("************7890", DatosSensiblesUtil.enmascarar("1234567890127890"));
    }
}
