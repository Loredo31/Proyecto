package com.proyecto.servicios.service;

import com.proyecto.servicios.config.SeguridadProperties;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.entity.seguridad.CodigoVerificacion;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.enums.EstadoCodigo;
import com.proyecto.servicios.enums.Plataforma;
import com.proyecto.servicios.enums.PropositoCodigo;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.repository.CodigoVerificacionRepository;
import com.proyecto.servicios.seguridad.TokenSeguro;
import com.proyecto.servicios.service.CodigoVerificacionService.Emision;
import com.proyecto.servicios.service.Impl.CodigoVerificacionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodigoVerificacionServiceTest {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    @Mock
    private CodigoVerificacionRepository repository;
    @Mock
    private CorreoService correoService;

    private CodigoVerificacionServiceImpl service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new CodigoVerificacionServiceImpl(repository, ENCODER, correoService, new SeguridadProperties());
        usuario = Usuario.builder().id(5L).correo("juan@example.com").build();
    }

    private CodigoVerificacion pendiente(String ticket, String codigo, LocalDateTime enviado, LocalDateTime expira) {
        return CodigoVerificacion.builder().id("C1").usuario(usuario).proposito(PropositoCodigo.LOGIN)
                .codigoHash(ENCODER.encode(codigo)).ticketHash(TokenSeguro.huella(ticket))
                .estado(EstadoCodigo.PENDIENTE).fechaCreacion(enviado).fechaEnvio(enviado).expiraEn(expira).build();
    }

    private ApiResponseEnum codigoError(Runnable r) {
        return assertThrows(BusinessException.class, r::run).getResponseStatus();
    }

    @Test
    void emitir_GuardaCifradoYEnviaPorCorreo() {
        CodigoVerificacion anterior = pendiente("viejo", "111111", LocalDateTime.now().minusMinutes(2), LocalDateTime.now().plusMinutes(1));
        when(repository.findByUsuarioIdAndPropositoAndEstado(5L, PropositoCodigo.LOGIN, EstadoCodigo.PENDIENTE))
                .thenReturn(List.of(anterior));

        Emision emision = service.emitir(usuario, PropositoCodigo.LOGIN, Plataforma.MOVIL, "Pixel", null);

        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviarCodigo(eq("juan@example.com"), eq(PropositoCodigo.LOGIN), codigo.capture(), eq(3L));
        ArgumentCaptor<CodigoVerificacion> guardado = ArgumentCaptor.forClass(CodigoVerificacion.class);
        verify(repository).save(guardado.capture());

        CodigoVerificacion c = guardado.getValue();
        assertTrue(codigo.getValue().matches("[0-9]{6}"));
        assertNotEquals(codigo.getValue(), c.getCodigoHash());
        assertTrue(ENCODER.matches(codigo.getValue(), c.getCodigoHash()));
        assertEquals(TokenSeguro.huella(emision.ticket()), c.getTicketHash());
        assertEquals(EstadoCodigo.ANULADO, anterior.getEstado());
        assertEquals("j***n@example.com", emision.correoEnmascarado());
        assertEquals(180, emision.expiraEnSegundos());
        assertFalse(emision.toString().contains(emision.ticket()));
    }

    @Test
    void emitir_RespetaLaEsperaEntreEnvios() {
        CodigoVerificacion reciente = pendiente("t", "111111", LocalDateTime.now().minusSeconds(10), LocalDateTime.now().plusMinutes(2));
        when(repository.findByUsuarioIdAndPropositoAndEstado(5L, PropositoCodigo.LOGIN, EstadoCodigo.PENDIENTE))
                .thenReturn(List.of(reciente));

        assertEquals(ApiResponseEnum.DEMASIADAS_SOLICITUDES,
                codigoError(() -> service.emitir(usuario, PropositoCodigo.LOGIN, Plataforma.WEB, null, null)));
        verify(correoService, never()).enviarCodigo(any(), any(), any(), anyLong());
    }

    @Test
    void emitir_LimitePorHora() {
        when(repository.countByUsuarioIdAndPropositoAndFechaCreacionAfter(eq(5L), eq(PropositoCodigo.RECUPERACION), any()))
                .thenReturn(5L);

        assertEquals(ApiResponseEnum.DEMASIADAS_SOLICITUDES,
                codigoError(() -> service.emitir(usuario, PropositoCodigo.RECUPERACION, null, null, null)));
    }

    @Test
    void buscarVigente_ValidaTicketPropositoYExpiracion() {
        CodigoVerificacion ok = pendiente("t1", "123456", LocalDateTime.now(), LocalDateTime.now().plusMinutes(3));
        CodigoVerificacion expirado = pendiente("t2", "123456", LocalDateTime.now().minusMinutes(5), LocalDateTime.now().minusSeconds(1));
        CodigoVerificacion usado = pendiente("t3", "123456", LocalDateTime.now(), LocalDateTime.now().plusMinutes(3));
        usado.setEstado(EstadoCodigo.USADO);
        when(repository.findByTicketHashForUpdate(TokenSeguro.huella("t1"))).thenReturn(Optional.of(ok));
        when(repository.findByTicketHashForUpdate(TokenSeguro.huella("t2"))).thenReturn(Optional.of(expirado));
        when(repository.findByTicketHashForUpdate(TokenSeguro.huella("t3"))).thenReturn(Optional.of(usado));
        when(repository.findByTicketHashForUpdate(TokenSeguro.huella("nada"))).thenReturn(Optional.empty());

        assertSame(ok, service.buscarVigente("t1", PropositoCodigo.LOGIN));
        assertEquals(ApiResponseEnum.CODIGO_INVALIDO, codigoError(() -> service.buscarVigente("t1", PropositoCodigo.RECUPERACION)));
        assertEquals(ApiResponseEnum.CODIGO_INVALIDO, codigoError(() -> service.buscarVigente("t2", PropositoCodigo.LOGIN)));
        assertEquals(EstadoCodigo.EXPIRADO, expirado.getEstado());
        assertEquals(ApiResponseEnum.CODIGO_INVALIDO, codigoError(() -> service.buscarVigente("t3", PropositoCodigo.LOGIN)));
        assertEquals(ApiResponseEnum.CODIGO_INVALIDO, codigoError(() -> service.buscarVigente("nada", PropositoCodigo.LOGIN)));
        assertEquals(ApiResponseEnum.CODIGO_INVALIDO, codigoError(() -> service.buscarVigente(" ", PropositoCodigo.LOGIN)));
    }

    @Test
    void verificar_UnSoloUsoYAnulaTrasCincoErrores() {
        CodigoVerificacion c = pendiente("t", "123456", LocalDateTime.now(), LocalDateTime.now().plusMinutes(3));

        for (int i = 0; i < 4; i++) {
            assertFalse(service.verificar(c, "000000"));
        }
        assertEquals(EstadoCodigo.PENDIENTE, c.getEstado());
        assertFalse(service.verificar(c, null));
        assertEquals(EstadoCodigo.ANULADO, c.getEstado());

        CodigoVerificacion otro = pendiente("t", "123456", LocalDateTime.now(), LocalDateTime.now().plusMinutes(3));
        assertTrue(service.verificar(otro, "123456"));
        assertEquals(EstadoCodigo.USADO, otro.getEstado());
        assertTrue(otro.getFechaUso() != null);
    }

    @Test
    void reenviar_NuevoCodigoMismoTicket() {
        CodigoVerificacion c = pendiente("t", "111111", LocalDateTime.now().minusMinutes(2), LocalDateTime.now().plusMinutes(1));
        c.setIntentos(2);
        String hashAnterior = c.getCodigoHash();
        when(repository.findByTicketHashForUpdate(TokenSeguro.huella("t"))).thenReturn(Optional.of(c));

        Emision emision = service.reenviar("t");

        assertEquals("t", emision.ticket());
        assertNotEquals(hashAnterior, c.getCodigoHash());
        assertEquals(1, c.getReenvios());
        assertEquals(0, c.getIntentos());
        verify(correoService).enviarCodigo(eq("juan@example.com"), eq(PropositoCodigo.LOGIN), anyString(), eq(3L));
    }

    @Test
    void reenviar_LimitesYEstados() {
        CodigoVerificacion reciente = pendiente("a", "1", LocalDateTime.now().minusSeconds(5), LocalDateTime.now().plusMinutes(2));
        CodigoVerificacion agotado = pendiente("b", "1", LocalDateTime.now().minusMinutes(2), LocalDateTime.now().plusMinutes(2));
        agotado.setReenvios(3);
        CodigoVerificacion usado = pendiente("c", "1", LocalDateTime.now().minusMinutes(2), LocalDateTime.now().plusMinutes(2));
        usado.setEstado(EstadoCodigo.USADO);
        when(repository.findByTicketHashForUpdate(TokenSeguro.huella("a"))).thenReturn(Optional.of(reciente));
        when(repository.findByTicketHashForUpdate(TokenSeguro.huella("b"))).thenReturn(Optional.of(agotado));
        when(repository.findByTicketHashForUpdate(TokenSeguro.huella("c"))).thenReturn(Optional.of(usado));

        assertEquals(ApiResponseEnum.DEMASIADAS_SOLICITUDES, codigoError(() -> service.reenviar("a")));
        assertEquals(ApiResponseEnum.DEMASIADAS_SOLICITUDES, codigoError(() -> service.reenviar("b")));
        assertEquals(ApiResponseEnum.CODIGO_INVALIDO, codigoError(() -> service.reenviar("c")));
        assertEquals(ApiResponseEnum.CODIGO_INVALIDO, codigoError(() -> service.reenviar(null)));
    }
}
