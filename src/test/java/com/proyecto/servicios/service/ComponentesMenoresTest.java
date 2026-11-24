package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.ApiResponse;
import com.proyecto.servicios.dto.pago.PagoServicioRequest;
import com.proyecto.servicios.dto.pago.PagoServicioResponseDto;
import com.proyecto.servicios.dto.usuario.UsuarioFiltroRequest;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.entity.sf.Personas;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.exception.CatalogException;
import com.proyecto.servicios.exception.ClienteYaRegistradoException;
import com.proyecto.servicios.exception.CuentaInactivaException;
import com.proyecto.servicios.exception.ExternalServiceException;
import com.proyecto.servicios.mapper.TransaccionPagoMapper;
import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.repositorys.sf.PersonasRepository;
import com.proyecto.servicios.service.Impl.PersonasServiceImpl;
import com.proyecto.servicios.specification.UsuarioSpecification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComponentesMenoresTest {

    // ---------- ConciliacionPagoScheduler ----------

    @Test
    void scheduler_ConciliaYReportaContadores() {
        TransaccionPagoService tx = mock(TransaccionPagoService.class);
        PagoServicioService pagos = mock(PagoServicioService.class);
        when(tx.moverProcesandoVencidasAConciliacion()).thenReturn(2);
        when(pagos.conciliarPendientes()).thenReturn(1);
        when(tx.contarPendientesFueraDeVentana()).thenReturn(3L);

        new ConciliacionPagoScheduler(tx, pagos).conciliar();

        verify(pagos).conciliarPendientes();
        verify(tx).contarPendientesFueraDeVentana();
    }

    @Test
    void scheduler_SinPendientes() {
        TransaccionPagoService tx = mock(TransaccionPagoService.class);
        PagoServicioService pagos = mock(PagoServicioService.class);

        new ConciliacionPagoScheduler(tx, pagos).conciliar();

        verify(tx).contarPendientesFueraDeVentana();
    }

    @Test
    void scheduler_ErrorNoSePropaga() {
        TransaccionPagoService tx = mock(TransaccionPagoService.class);
        PagoServicioService pagos = mock(PagoServicioService.class);
        when(tx.moverProcesandoVencidasAConciliacion()).thenThrow(new IllegalStateException("bd caida"));

        new ConciliacionPagoScheduler(tx, pagos).conciliar();

        verify(pagos, never()).conciliarPendientes();
    }

    // ---------- PersonasServiceImpl ----------

    private static PersonasRequest personaRequest() {
        PersonasRequest r = new PersonasRequest();
        r.setNombre("Luis");
        r.setApellidoP("Lopez");
        r.setApellidoMaterno("Diaz");
        return r;
    }

    @Test
    void personas_Crear() {
        PersonasRepository repo = mock(PersonasRepository.class);
        PersonaResponse resp = new PersonasServiceImpl(repo).creaPersona(personaRequest());

        verify(repo).save(any(Personas.class));
        assertEquals(1, resp.getCodigo());
        assertEquals("Luis", resp.getNombre());
        assertEquals("Lopez", resp.getApellidoP());
    }

    @Test
    void personas_EliminarExistente() {
        PersonasRepository repo = mock(PersonasRepository.class);
        Personas p = new Personas();
        when(repo.findByNombre("Luis")).thenReturn(Optional.of(p));
        EliminaPersonaRequest r = new EliminaPersonaRequest();
        r.setNombre("Luis");

        GenericResponse<?> resp = new PersonasServiceImpl(repo).eliminaPersona(r);

        verify(repo).delete(p);
        assertEquals(0, resp.getCodigo());
    }

    @Test
    void personas_EliminarInexistente() {
        PersonasRepository repo = mock(PersonasRepository.class);
        when(repo.findByNombre("X")).thenReturn(Optional.empty());
        EliminaPersonaRequest r = new EliminaPersonaRequest();
        r.setNombre("X");

        assertEquals(1, new PersonasServiceImpl(repo).eliminaPersona(r).getCodigo());
    }

    @Test
    void personas_ActualizarExistente() {
        PersonasRepository repo = mock(PersonasRepository.class);
        Personas p = new Personas();
        when(repo.findByNombre("Luis")).thenReturn(Optional.of(p));

        GenericResponse<?> resp = new PersonasServiceImpl(repo).actualizaPersona(personaRequest());

        verify(repo).save(p);
        assertEquals(0, resp.getCodigo());
        assertEquals("Diaz", p.getApellidoMaterno());
    }

    @Test
    void personas_ActualizarInexistente() {
        PersonasRepository repo = mock(PersonasRepository.class);
        when(repo.findByNombre("Luis")).thenReturn(Optional.empty());

        assertEquals(1, new PersonasServiceImpl(repo).actualizaPersona(personaRequest()).getCodigo());
        verify(repo, never()).save(any());
    }

    // ---------- ApiResponse ----------

    @Test
    void apiResponse_Fabricas() {
        ApiResponse<String> ok = ApiResponse.success("dato");
        assertEquals(ApiResponseEnum.EXITO.getCode(), ok.getCode());
        assertEquals("dato", ok.getData());
        assertNotNull(ok.getTimestamp());

        ApiResponse<Integer> okCustom = ApiResponse.success("201", "creado", 5);
        assertEquals("201", okCustom.getCode());
        assertEquals(5, okCustom.getData());

        ApiResponse<Void> err = ApiResponse.error(ApiResponseEnum.ERROR_INTERNO);
        assertEquals(ApiResponseEnum.ERROR_INTERNO.getMessage(), err.getMessage());
        assertNull(err.getData());

        ApiResponse<Void> err2 = ApiResponse.error("400", "malo");
        assertEquals("malo", err2.getMessage());

        ApiResponse<String> err3 = ApiResponse.error("400", "malo", "detalle");
        assertEquals("detalle", err3.getData());

        ApiResponse<String> vacio = new ApiResponse<>();
        vacio.setCode("1");
        assertEquals("1", vacio.getCode());
    }

    // ---------- Excepciones ----------

    @Test
    void excepciones_MapeoDeEstados() {
        assertEquals(ApiResponseEnum.ERROR_AUTENTICACION, new ExternalServiceException("x", 401).toResponseStatus());
        assertEquals(ApiResponseEnum.ERROR_AUTENTICACION, new ExternalServiceException("x", 403).toResponseStatus());
        RuntimeException causa = new RuntimeException("c");
        ExternalServiceException ex = new ExternalServiceException("x", 500, causa);
        assertEquals(ApiResponseEnum.SERVICIO_EXTERNO_NO_DISPONIBLE, ex.toResponseStatus());
        assertEquals(500, ex.getHttpStatus());
        assertSame(causa, ex.getCause());

        CatalogException ce = new CatalogException(ApiResponseEnum.TIMEOUT_SERVICIO, causa);
        assertEquals(ApiResponseEnum.TIMEOUT_SERVICIO, ce.getResponseStatus());
        assertSame(causa, ce.getCause());

        assertEquals("inactiva", new CuentaInactivaException("inactiva").getMessage());
        assertEquals("dup", new ClienteYaRegistradoException("dup").getMessage());
    }

    @Test
    void pagoServicioRequest_ValidacionReferencia() {
        PagoServicioRequest r = PagoServicioRequest.builder().bandera(false).referencia("5555555551").build();
        assertTrue(r.isReferenciaValidaParaTipoDePago());
        r.setReferencia("ABC");
        assertFalse(r.isReferenciaValidaParaTipoDePago());
        r.setBandera(true);
        assertFalse(r.isReferenciaValidaParaTipoDePago(), "3 caracteres: referencia demasiado corta");
        r.setReferencia("ABC-1");
        assertTrue(r.isReferenciaValidaParaTipoDePago());
        assertTrue(r.esConReferencia());
        r.setReferencia(null);
        assertTrue(r.isReferenciaValidaParaTipoDePago());
    }

    // ---------- TransaccionPagoMapper ----------

    @Test
    void transaccionMapper_EnmascaraCuenta() {
        TransaccionPagoMapper mapper = Mappers.getMapper(TransaccionPagoMapper.class);
        TransaccionPago tx = TransaccionPago.builder()
                .folio("F-1")
                .estado(EstadoTransaccion.EXITOSA)
                .codigoRespuesta("01")
                .mensajeRespuesta("ok")
                .numeroAutorizacion("A1")
                .upc("UPC")
                .codigoCanje("CC")
                .leyenda("L")
                .numeroCuenta("4152319999888877")
                .idServicio(1)
                .idProducto(2)
                .referencia("ref")
                .monto(new BigDecimal("10.00"))
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        PagoServicioResponseDto dto = mapper.toResponse(tx);

        assertEquals("F-1", dto.getFolio());
        assertEquals("************8877", dto.getNumCuenta());
        assertEquals(new BigDecimal("10.00"), dto.getMontoPago());
        assertEquals(EstadoTransaccion.EXITOSA, dto.getEstado());
        assertNull(mapper.toResponse(null));
    }

    // ---------- UsuarioSpecification ----------

    @Test
    @SuppressWarnings("unchecked")
    void especificacion_ConTodosLosFiltros() {
        Root<Usuario> root = mock(Root.class, RETURNS_DEEP_STUBS);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        UsuarioFiltroRequest filtro = UsuarioFiltroRequest.builder()
                .id(1L).clienteId(2L).correo(" Ana ").activo(true)
                .fechaInicio(LocalDateTime.now().minusDays(1)).fechaFin(LocalDateTime.now())
                .build();

        Specification<Usuario> spec = UsuarioSpecification.conFiltros(filtro);
        spec.toPredicate(root, query, cb);

        verify(cb, times(3)).equal(any(), any(Object.class));
        verify(cb).like(any(), org.mockito.ArgumentMatchers.eq("%ana%"));
        verify(cb).and(any(jakarta.persistence.criteria.Predicate[].class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void especificacion_FiltroNuloYVacio() {
        Root<Usuario> root = mock(Root.class, RETURNS_DEEP_STUBS);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        UsuarioSpecification.conFiltros(null).toPredicate(root, query, cb);
        verify(cb).conjunction();

        UsuarioSpecification.conFiltros(UsuarioFiltroRequest.builder().correo("  ").build()).toPredicate(root, query, cb);
        verify(cb, never()).like(any(), any(String.class));
    }
}
