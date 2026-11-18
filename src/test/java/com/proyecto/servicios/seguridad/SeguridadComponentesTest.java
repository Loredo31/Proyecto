package com.proyecto.servicios.seguridad;

import com.proyecto.servicios.config.SeguridadProperties;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.CuentaBancaria;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.repository.CuentaBancariaRepository;
import com.proyecto.servicios.service.SesionService;
import com.proyecto.servicios.util.JwtTokenService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeguridadComponentesTest {

    private static final String SECRETO = "secreto-de-pruebas-con-longitud-suficiente-0123456789";

    private final JwtTokenService jwt = new JwtTokenService(SECRETO, new SeguridadProperties());

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private String token(Long clienteId, String sesionId) {
        Usuario u = Usuario.builder().id(9L).correo("u@example.com").cliente(Cliente.builder().id(clienteId).build()).build();
        return jwt.generarAccessToken(u, sesionId);
    }

    private static void autenticarComo(UsuarioAutenticado usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
    }

    // ---------- JwtAuthenticationFilter ----------

    @Test
    void filtro_TokenValidoConSesionActivaAutentica() throws Exception {
        SesionService sesiones = mock(SesionService.class);
        when(sesiones.estaActiva("S-1")).thenReturn(true);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token(1L, "S-1"));
        FilterChain chain = mock(FilterChain.class);

        new JwtAuthenticationFilter(jwt, sesiones).doFilter(req, new MockHttpServletResponse(), chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("S-1", ((UsuarioAutenticado) auth.getPrincipal()).sesionId());
        assertEquals("ROLE_CLIENTE", auth.getAuthorities().iterator().next().getAuthority());
        verify(chain).doFilter(any(), any());
    }

    @Test
    void filtro_SesionFinalizadaNoAutenticaYMarcaMotivo() throws Exception {
        SesionService sesiones = mock(SesionService.class);
        when(sesiones.estaActiva("S-1")).thenReturn(false);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token(1L, "S-1"));

        new JwtAuthenticationFilter(jwt, sesiones).doFilter(req, new MockHttpServletResponse(), mock(FilterChain.class));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(ApiResponseEnum.SESION_FINALIZADA, req.getAttribute(JwtAuthenticationFilter.ATRIBUTO_ERROR));
    }

    @Test
    void filtro_TokenInvalidoYSinHeader() throws Exception {
        SesionService sesiones = mock(SesionService.class);
        JwtAuthenticationFilter filtro = new JwtAuthenticationFilter(jwt, sesiones);

        MockHttpServletRequest invalido = new MockHttpServletRequest();
        invalido.addHeader("Authorization", "Bearer basura");
        filtro.doFilter(invalido, new MockHttpServletResponse(), mock(FilterChain.class));
        assertEquals(ApiResponseEnum.TOKEN_INVALIDO, invalido.getAttribute(JwtAuthenticationFilter.ATRIBUTO_ERROR));

        MockHttpServletRequest sinHeader = new MockHttpServletRequest();
        filtro.doFilter(sinHeader, new MockHttpServletResponse(), mock(FilterChain.class));
        assertNull(sinHeader.getAttribute(JwtAuthenticationFilter.ATRIBUTO_ERROR));

        MockHttpServletRequest basic = new MockHttpServletRequest();
        basic.addHeader("Authorization", "Basic abc");
        filtro.doFilter(basic, new MockHttpServletResponse(), mock(FilterChain.class));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(sesiones, never()).estaActiva(any());
    }

    // ---------- RespuestaSeguridadHandler ----------

    @Test
    void respuestas_401ConMotivoY403() throws Exception {
        RespuestaSeguridadHandler handler = new RespuestaSeguridadHandler();

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute(JwtAuthenticationFilter.ATRIBUTO_ERROR, ApiResponseEnum.SESION_FINALIZADA);
        MockHttpServletResponse r1 = new MockHttpServletResponse();
        handler.commence(req, r1, new InsufficientAuthenticationException("x"));
        assertEquals(401, r1.getStatus());
        assertTrue(r1.getContentAsString().contains(ApiResponseEnum.SESION_FINALIZADA.getCode()));

        MockHttpServletResponse r2 = new MockHttpServletResponse();
        handler.commence(new MockHttpServletRequest(), r2, new InsufficientAuthenticationException("x"));
        assertTrue(r2.getContentAsString().contains(ApiResponseEnum.TOKEN_INVALIDO.getCode()));

        MockHttpServletResponse r3 = new MockHttpServletResponse();
        handler.handle(new MockHttpServletRequest(), r3, new AccessDeniedException("x"));
        assertEquals(403, r3.getStatus());
        assertTrue(r3.getContentAsString().contains(ApiResponseEnum.SIN_PERMISOS.getCode()));
    }

    // ---------- AccesoRecursos ----------

    @Test
    void acceso_ClienteSoloLoPropio() {
        CuentaBancariaRepository repo = mock(CuentaBancariaRepository.class);
        AccesoRecursos acceso = new AccesoRecursos(repo);
        autenticarComo(new UsuarioAutenticado(9L, 1L, "u", "S"));
        CuentaBancaria propia = CuentaBancaria.builder().cliente(Cliente.builder().id(1L).build()).build();
        CuentaBancaria ajena = CuentaBancaria.builder().cliente(Cliente.builder().id(2L).build()).build();
        when(repo.findByNumeroCuenta("P")).thenReturn(Optional.of(propia));
        when(repo.findByNumeroCuenta("A")).thenReturn(Optional.of(ajena));
        when(repo.findByNumeroCuenta("X")).thenReturn(Optional.empty());

        assertEquals(1L, acceso.clienteActual());
        assertDoesNotThrow(() -> acceso.validarCliente(1L));
        assertThrows(BusinessException.class, () -> acceso.validarCliente(2L));
        assertThrows(BusinessException.class, () -> acceso.validarCliente(null));
        assertDoesNotThrow(() -> acceso.validarCuenta("P"));
        assertThrows(CuentaNoEncontradaException.class, () -> acceso.validarCuenta("A"));
        assertThrows(CuentaNoEncontradaException.class, () -> acceso.validarCuenta("X"));
    }


    @Test
    void acceso_SinAutenticacion() {
        AccesoRecursos acceso = new AccesoRecursos(mock(CuentaBancariaRepository.class));
        assertThrows(BusinessException.class, acceso::actual);
    }

    @Test
    void usuarioAutenticado_NoExponeElCorreo() {
        UsuarioAutenticado u = new UsuarioAutenticado(1L, 2L, "privado@example.com", "S");
        assertTrue(!u.toString().contains("privado"));
        assertSame("S", u.sesionId());
    }
}
