package com.proyecto.servicios.service;

import com.proyecto.servicios.config.SeguridadProperties;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.entity.seguridad.Sesion;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.enums.EstadoSesion;
import com.proyecto.servicios.enums.Plataforma;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.repository.SesionRepository;
import com.proyecto.servicios.seguridad.NotificadorSesion;
import com.proyecto.servicios.seguridad.TokenSeguro;
import com.proyecto.servicios.service.Impl.SesionServiceImpl;
import com.proyecto.servicios.service.SesionService.TokensSesion;
import com.proyecto.servicios.util.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SesionServiceTest {

    @Mock
    private SesionRepository sesionRepository;
    @Mock
    private NotificadorSesion notificador;

    private SesionServiceImpl service;
    private JwtTokenService jwt;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        SeguridadProperties properties = new SeguridadProperties();
        jwt = new JwtTokenService("secreto-de-pruebas-con-longitud-suficiente-0123456789", properties);
        service = new SesionServiceImpl(sesionRepository, jwt, properties, notificador);
        usuario = Usuario.builder().id(5L).correo("a@example.com")
                .cliente(Cliente.builder().id(1L).build()).build();
    }

    private Sesion sesion(String id, EstadoSesion estado, LocalDateTime ultimaActividad, LocalDateTime expira) {
        return Sesion.builder().id(id).usuario(usuario).plataforma(Plataforma.MOVIL).estado(estado)
                .refreshHash(TokenSeguro.huella("r-" + id)).fechaCreacion(ultimaActividad)
                .ultimaActividad(ultimaActividad).expiraEn(expira).build();
    }

    @Test
    void crear_GuardaHuellaYEmiteTokensValidos() {
        when(sesionRepository.save(any(Sesion.class))).thenAnswer(i -> i.getArgument(0));

        TokensSesion tokens = service.crear(usuario, Plataforma.WEB, "  Chrome <script>\n en Windows ");

        ArgumentCaptor<Sesion> guardada = ArgumentCaptor.forClass(Sesion.class);
        verify(sesionRepository).save(guardada.capture());
        Sesion s = guardada.getValue();
        assertEquals(EstadoSesion.ACTIVA, s.getEstado());
        assertEquals("Chrome script en Windows", s.getDispositivo());
        assertEquals(TokenSeguro.huella(tokens.refreshToken()), s.getRefreshHash());
        assertNotEquals(tokens.refreshToken(), s.getRefreshHash());
        assertEquals(s.getId(), jwt.validar(tokens.accessToken()).orElseThrow().sesionId());
        assertEquals(900, tokens.accessSegundos());
        assertEquals(1800, tokens.refreshSegundos(), 2);
        assertFalse(tokens.toString().contains(tokens.refreshToken()));
    }

    @Test
    void crear_DispositivoVacioOLargo() {
        when(sesionRepository.save(any(Sesion.class))).thenAnswer(i -> i.getArgument(0));

        service.crear(usuario, Plataforma.MOVIL, " ");
        service.crear(usuario, Plataforma.MOVIL, "x".repeat(300));

        ArgumentCaptor<Sesion> guardada = ArgumentCaptor.forClass(Sesion.class);
        verify(sesionRepository, org.mockito.Mockito.times(2)).save(guardada.capture());
        assertNull(guardada.getAllValues().get(0).getDispositivo());
        assertEquals(150, guardada.getAllValues().get(1).getDispositivo().length());
    }

    @Test
    void buscarVigente_ExpiraLasCaducadas() {
        LocalDateTime ahora = LocalDateTime.now();
        Sesion inactiva = sesion("A", EstadoSesion.ACTIVA, ahora.minusHours(1), ahora.plusHours(5));
        Sesion vencida = sesion("B", EstadoSesion.ACTIVA, ahora, ahora.minusSeconds(1));
        Sesion vigente = sesion("C", EstadoSesion.ACTIVA, ahora, ahora.plusHours(5));
        when(sesionRepository.findByUsuarioIdAndEstado(5L, EstadoSesion.ACTIVA)).thenReturn(List.of(inactiva, vencida, vigente));

        assertSame(vigente, service.buscarVigente(5L).orElseThrow());
        assertEquals(EstadoSesion.EXPIRADA, inactiva.getEstado());
        assertEquals(EstadoSesion.EXPIRADA, vencida.getEstado());
    }

    @Test
    void estaActiva() {
        LocalDateTime ahora = LocalDateTime.now();
        when(sesionRepository.findById("C")).thenReturn(Optional.of(sesion("C", EstadoSesion.ACTIVA, ahora, ahora.plusHours(1))));
        when(sesionRepository.findById("R")).thenReturn(Optional.of(sesion("R", EstadoSesion.REVOCADA, ahora, ahora.plusHours(1))));
        when(sesionRepository.findById("X")).thenReturn(Optional.empty());

        assertTrue(service.estaActiva("C"));
        assertFalse(service.estaActiva("R"));
        assertFalse(service.estaActiva("X"));
        assertFalse(service.estaActiva(null));
    }

    @Test
    void refrescar_RotaElRefreshToken() {
        LocalDateTime ahora = LocalDateTime.now();
        Sesion s = sesion("C", EstadoSesion.ACTIVA, ahora.minusMinutes(10), ahora.plusHours(5));
        when(sesionRepository.findByRefreshHashForUpdate(TokenSeguro.huella("r-C"))).thenReturn(Optional.of(s));

        TokensSesion tokens = service.refrescar("r-C");

        assertEquals(TokenSeguro.huella("r-C"), s.getRefreshHashAnterior());
        assertEquals(TokenSeguro.huella(tokens.refreshToken()), s.getRefreshHash());
        assertTrue(s.getUltimaActividad().isAfter(ahora.minusSeconds(1)));
    }

    @Test
    void refrescar_TokenReutilizadoRevocaLaSesion() {
        LocalDateTime ahora = LocalDateTime.now();
        Sesion s = sesion("C", EstadoSesion.ACTIVA, ahora, ahora.plusHours(5));
        String huellaVieja = TokenSeguro.huella("viejo");
        when(sesionRepository.findByRefreshHashForUpdate(huellaVieja)).thenReturn(Optional.empty());
        when(sesionRepository.findByRefreshHashAnterior(huellaVieja)).thenReturn(Optional.of(s));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.refrescar("viejo"));

        assertEquals(ApiResponseEnum.REFRESH_INVALIDO, ex.getResponseStatus());
        assertEquals(EstadoSesion.REVOCADA, s.getEstado());
        verify(notificador).notificarSesionCerrada(eq("C"), anyString());
    }

    @Test
    void refrescar_SesionCaducadaOInvalida() {
        LocalDateTime ahora = LocalDateTime.now();
        Sesion caducada = sesion("C", EstadoSesion.ACTIVA, ahora.minusHours(2), ahora.plusHours(5));
        when(sesionRepository.findByRefreshHashForUpdate(TokenSeguro.huella("r-C"))).thenReturn(Optional.of(caducada));
        Sesion cerrada = sesion("D", EstadoSesion.CERRADA, ahora, ahora.plusHours(5));
        when(sesionRepository.findByRefreshHashForUpdate(TokenSeguro.huella("r-D"))).thenReturn(Optional.of(cerrada));
        when(sesionRepository.findByRefreshHashForUpdate(TokenSeguro.huella("nada"))).thenReturn(Optional.empty());
        when(sesionRepository.findByRefreshHashAnterior(TokenSeguro.huella("nada"))).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.refrescar("r-C"));
        assertEquals(EstadoSesion.EXPIRADA, caducada.getEstado());
        assertThrows(BusinessException.class, () -> service.refrescar("r-D"));
        assertEquals(EstadoSesion.CERRADA, cerrada.getEstado());
        assertThrows(BusinessException.class, () -> service.refrescar("nada"));
        assertThrows(BusinessException.class, () -> service.refrescar(null));
        assertThrows(BusinessException.class, () -> service.refrescar(" "));
    }

    @Test
    void cerrar_SoloSesionesActivasYNotifica() {
        LocalDateTime ahora = LocalDateTime.now();
        Sesion activa = sesion("C", EstadoSesion.ACTIVA, ahora, ahora.plusHours(1));
        Sesion cerrada = sesion("D", EstadoSesion.CERRADA, ahora, ahora.plusHours(1));
        when(sesionRepository.findById("C")).thenReturn(Optional.of(activa));
        when(sesionRepository.findById("D")).thenReturn(Optional.of(cerrada));

        service.cerrar("C", EstadoSesion.CERRADA, "logout");
        service.cerrar("D", EstadoSesion.REVOCADA, "x");

        assertEquals(EstadoSesion.CERRADA, activa.getEstado());
        assertEquals("logout", activa.getMotivoCierre());
        assertEquals(EstadoSesion.CERRADA, cerrada.getEstado());
        verify(notificador).notificarSesionCerrada("C", "logout");
        verify(notificador, never()).notificarSesionCerrada(eq("D"), any());
    }
}
