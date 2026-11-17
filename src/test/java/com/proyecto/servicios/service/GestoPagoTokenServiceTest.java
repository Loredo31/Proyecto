package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.Impl.GestoPagoTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestoPagoTokenServiceTest {

    private static final Integer DISTRIBUIDOR = 83;
    private static final String DISPOSITIVO = "DEV-1";

    @Mock
    private GestoPagoAuthClient authClient;
    @Mock
    private GestoPagoTokenRepository tokenRepository;

    private GestoPagoTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        GestoPagoTokenMapper mapper = Mappers.getMapper(GestoPagoTokenMapper.class);
        service = new GestoPagoTokenServiceImpl(authClient, tokenRepository, mapper);
        ReflectionTestUtils.setField(service, "idDistribuidor", DISTRIBUIDOR);
        ReflectionTestUtils.setField(service, "codigoDispositivo", DISPOSITIVO);
        ReflectionTestUtils.setField(service, "password", "clave-prueba");
        ReflectionTestUtils.setField(service, "tokenTtl", Duration.ofHours(24));
    }

    private static GestoPagoAuthResponse respuesta(String token, Long expiresIn) {
        GestoPagoAuthResponse r = new GestoPagoAuthResponse();
        r.setToken(token);
        r.setTokenType("Bearer");
        r.setExpiresIn(expiresIn);
        return r;
    }

    private static GestoPagoToken tokenBd(String valor, boolean activo, LocalDateTime actualizado) {
        GestoPagoToken t = new GestoPagoToken();
        t.setToken(valor);
        t.setActivo(activo);
        t.setFechaActualizacion(actualizado);
        t.setIdDistribuidor(DISTRIBUIDOR);
        t.setCodigoDispositivo(DISPOSITIVO);
        return t;
    }

    private void guardaEco() {
        when(tokenRepository.save(any(GestoPagoToken.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void obtenerTokenVigente_SinTokenAutenticaYLuegoUsaMemoria() {
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(DISTRIBUIDOR, DISPOSITIVO)).thenReturn(Optional.empty());
        when(authClient.authenticate(DISTRIBUIDOR, DISPOSITIVO, "clave-prueba")).thenReturn(respuesta("T1", 3600L));
        guardaEco();

        assertEquals("T1", service.obtenerTokenVigente());
        assertEquals("T1", service.obtenerTokenVigente());

        verify(authClient, times(1)).authenticate(any(), any(), any());
    }

    @Test
    void obtenerTokenVigente_UsaTokenDeBaseDeDatos() {
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(DISTRIBUIDOR, DISPOSITIVO))
                .thenReturn(Optional.of(tokenBd("BD", true, LocalDateTime.now())));

        assertEquals("BD", service.obtenerTokenVigente());
        verify(authClient, never()).authenticate(any(), any(), any());
    }

    @Test
    void obtenerTokenVigente_TokenBdVencidoRenueva() {
        GestoPagoToken vencido = tokenBd("VIEJO", true, LocalDateTime.now().minusDays(2));
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(DISTRIBUIDOR, DISPOSITIVO)).thenReturn(Optional.of(vencido));
        when(authClient.authenticate(DISTRIBUIDOR, DISPOSITIVO, "clave-prueba")).thenReturn(respuesta("NUEVO", null));
        guardaEco();

        assertEquals("NUEVO", service.obtenerTokenVigente());
        assertTrue(vencido.getActivo());
    }

    @Test
    void renovarToken_RespuestaSinTokenNoPropagaError() {
        when(authClient.authenticate(DISTRIBUIDOR, DISPOSITIVO, "clave-prueba")).thenReturn(respuesta(" ", null));

        service.renovarToken();

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void obtenerTokenVigente_RespuestaNulaFalla() {
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(DISTRIBUIDOR, DISPOSITIVO)).thenReturn(Optional.empty());
        when(authClient.authenticate(DISTRIBUIDOR, DISPOSITIVO, "clave-prueba")).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> service.obtenerTokenVigente());
    }

    @Test
    void invalidarToken_DesactivaTokenPersistido() {
        GestoPagoToken t = tokenBd("X", true, LocalDateTime.now());
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(DISTRIBUIDOR, DISPOSITIVO)).thenReturn(Optional.of(t));

        service.invalidarToken();

        assertFalse(t.getActivo());
        verify(tokenRepository).save(t);
    }

    @Test
    void invalidarToken_SinTokenPersistido() {
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(DISTRIBUIDOR, DISPOSITIVO)).thenReturn(Optional.empty());

        service.invalidarToken();

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void obtenerTokenActivo_DelegaEnRepositorio() {
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(1, "D")).thenReturn(Optional.empty());
        assertTrue(service.obtenerTokenActivo(1, "D").isEmpty());
    }
}
