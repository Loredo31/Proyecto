package com.proyecto.servicios.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilidadesTest {

    private static final String SECRETO = "secreto-de-pruebas-con-longitud-suficiente-0123456789";

    @Test
    void jwt_GeneraYValidaToken() {
        JwtTokenService service = new JwtTokenService(SECRETO);

        String token = service.generarToken("ana@example.com", 7L);

        assertTrue(service.validarToken(token));
        assertEquals("ana@example.com", service.obtenerCorreo(token));
        assertEquals(7L, service.obtenerClienteId(token));
        assertEquals(7L, service.obtenerClienteIdDesdeHeader("Bearer " + token));
        assertEquals(86400L, service.getExpirationSeconds());
    }

    @Test
    void jwt_TokenFirmadoConOtroSecretoEsInvalido() {
        JwtTokenService emisor = new JwtTokenService(SECRETO);
        JwtTokenService otro = new JwtTokenService("otro-secreto-distinto-con-longitud-suficiente-987654");

        String token = emisor.generarToken("ana@example.com", 1L);

        assertFalse(otro.validarToken(token));
        assertFalse(emisor.validarToken("no-es-un-jwt"));
        assertFalse(emisor.validarToken(null));
        assertFalse(emisor.validarToken(" "));
    }

    @Test
    void jwt_SinSecretoConfiguradoGeneraUnoAleatorio() {
        JwtTokenService a = new JwtTokenService("");
        JwtTokenService b = new JwtTokenService(null);

        String token = a.generarToken("x@example.com", 2L);

        assertTrue(a.validarToken(token));
        assertFalse(b.validarToken(token));
    }

    @Test
    void jwt_HeaderInvalidoRegresaNull() {
        JwtTokenService service = new JwtTokenService(SECRETO);

        assertNull(service.obtenerClienteIdDesdeHeader(null));
        assertNull(service.obtenerClienteIdDesdeHeader("Basic abc"));
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
