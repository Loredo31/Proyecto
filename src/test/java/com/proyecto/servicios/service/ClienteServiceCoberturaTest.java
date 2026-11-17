package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.cliente.ClientePatchRequest;
import com.proyecto.servicios.dto.cliente.ClienteResponseDto;
import com.proyecto.servicios.entity.cliente.BiometriaInfo;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.CuentaBancaria;
import com.proyecto.servicios.entity.cliente.Domicilio;
import com.proyecto.servicios.entity.cliente.InformacionLaboral;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.EstadoCuenta;
import com.proyecto.servicios.enums.EstadoRepublica;
import com.proyecto.servicios.enums.Sexo;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.repository.ClienteRepository;
import com.proyecto.servicios.repository.CuentaBancariaRepository;
import com.proyecto.servicios.service.Impl.ClienteServiceImpl;
import com.proyecto.servicios.util.GeneradorCuentaBancaria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceCoberturaTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private CuentaBancariaRepository cuentaBancariaRepository;
    @Mock
    private GeneradorCuentaBancaria generadorCuentaBancaria;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClienteServiceImpl service;

    private Cliente cliente;
    private CuentaBancaria cuenta;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder()
                .id(1L)
                .nombre("Juan")
                .apellidoPaterno("Perez")
                .apellidoMaterno("Lopez")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .curp("PELJ900101HDFRPR01")
                .rfc("PELJ900101AA0")
                .correoElectronico("juan@example.com")
                .telefonoMovil("4181234567")
                .domicilio(Domicilio.builder().calle("C").estado(EstadoRepublica.GUANAJUATO).codigoPostal("37800").build())
                .informacionLaboral(InformacionLaboral.builder().ocupacion("Dev").empresa("ACME")
                        .ingresoMensual(new BigDecimal("1000")).build())
                .biometriaInfo(new BiometriaInfo())
                .activoLogin(true)
                .eliminadoLogico(false)
                .cuentas(new ArrayList<>())
                .fechaCreacion(LocalDateTime.now())
                .build();
        cuenta = CuentaBancaria.builder().id(5L).numeroCuenta("4152311234567890").saldo(BigDecimal.TEN)
                .estado(EstadoCuenta.ACTIVA).cliente(cliente).build();
        cliente.getCuentas().add(cuenta);
        cliente.setUsuario(Usuario.builder().id(3L).correo("juan@example.com").activo(true).cliente(cliente).build());
    }

    private void clienteExiste() {
        when(clienteRepository.findByIdAndEliminadoLogicoFalse(1L)).thenReturn(Optional.of(cliente));
    }

    private void guardaEco() {
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void patch_ActualizaTodosLosCampos() {
        clienteExiste();
        guardaEco();
        when(clienteRepository.existsByTelefonoMovilAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(clienteRepository.existsByCorreoElectronicoAndIdNot(anyString(), anyLong())).thenReturn(false);

        ClientePatchRequest req = ClientePatchRequest.builder()
                .id(1L)
                .nombre("Pedro").segundoNombre("Jose").apellidoPaterno("Ruiz").apellidoMaterno("Mora")
                .fechaNacimiento(LocalDate.of(1985, 5, 5)).sexo(Sexo.MASCULINO).nacionalidad("MEXICANA")
                .estadoCivil(EstadoCivil.CASADO)
                .telefonoMovil(4189998877L).telefonoAlternativo(4181112233L).correoElectronico("pedro@example.com")
                .calle("Nueva").numeroExterior(1).numeroInterior(2).colonia("Centro").municipio("Dolores")
                .estado(EstadoRepublica.GUANAJUATO).codigoPostal("37801").pais("MEXICO")
                .ocupacion("QA").empresa("Otra").ingresoMensual(new BigDecimal("2000"))
                .activoLogin(false)
                .build();

        ClienteResponseDto dto = service.patchCliente(1L, req);

        assertEquals("Pedro", dto.getDatosPersonales().getNombre());
        assertEquals("Mora", cliente.getApellidoMaterno());
        assertEquals("4189998877", cliente.getTelefonoMovil());
        assertEquals("4181112233", cliente.getTelefonoAlternativo());
        assertEquals("pedro@example.com", cliente.getUsuario().getCorreo());
        assertEquals("Nueva", cliente.getDomicilio().getCalle());
        assertEquals("37801", cliente.getDomicilio().getCodigoPostal());
        assertEquals("QA", cliente.getInformacionLaboral().getOcupacion());
        assertFalse(cliente.getActivoLogin());
        assertFalse(cliente.getUsuario().getActivo());
        assertEquals(EstadoCuenta.BLOQUEADA, cuenta.getEstado());
    }

    @Test
    void patch_SinCamposNoModificaNada() {
        clienteExiste();
        guardaEco();
        cliente.setDomicilio(null);
        cliente.setInformacionLaboral(null);
        cliente.setUsuario(null);

        ClienteResponseDto dto = service.patchCliente(1L, ClientePatchRequest.builder().id(1L).build());

        assertEquals("Juan", dto.getDatosPersonales().getNombre());
        assertNull(dto.getDomicilio());
        assertNull(dto.getInformacionLaboral());
        assertNull(dto.getUsuario());
    }

    @Test
    void patch_ActivarLoginNoBloqueaCuentas() {
        clienteExiste();
        guardaEco();
        cliente.setUsuario(null);

        service.patchCliente(1L, ClientePatchRequest.builder().id(1L).activoLogin(true).build());

        assertTrue(cliente.getActivoLogin());
        assertEquals(EstadoCuenta.ACTIVA, cuenta.getEstado());
    }

    @Test
    void patch_CorreoSinUsuarioAsociado() {
        clienteExiste();
        guardaEco();
        cliente.setUsuario(null);
        when(clienteRepository.existsByCorreoElectronicoAndIdNot("n@example.com", 1L)).thenReturn(false);

        service.patchCliente(1L, ClientePatchRequest.builder().id(1L).correoElectronico("n@example.com").build());

        assertEquals("n@example.com", cliente.getCorreoElectronico());
    }

    @Test
    void patch_TelefonoDuplicado() {
        clienteExiste();
        when(clienteRepository.existsByTelefonoMovilAndIdNot("4189998877", 1L)).thenReturn(true);
        ClientePatchRequest req = ClientePatchRequest.builder().id(1L).telefonoMovil(4189998877L).build();

        assertThrows(ValidacionNegocioException.class, () -> service.patchCliente(1L, req));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void patch_CorreoDuplicado() {
        clienteExiste();
        when(clienteRepository.existsByCorreoElectronicoAndIdNot("x@example.com", 1L)).thenReturn(true);
        ClientePatchRequest req = ClientePatchRequest.builder().id(1L).correoElectronico("x@example.com").build();

        assertThrows(ValidacionNegocioException.class, () -> service.patchCliente(1L, req));
    }

    @Test
    void patch_IngresoNoPositivo() {
        clienteExiste();
        ClientePatchRequest req = ClientePatchRequest.builder().id(1L).ingresoMensual(BigDecimal.ZERO).build();

        assertThrows(ValidacionNegocioException.class, () -> service.patchCliente(1L, req));
    }

    @Test
    void patch_MenorDeEdad() {
        clienteExiste();
        ClientePatchRequest req = ClientePatchRequest.builder().id(1L)
                .fechaNacimiento(LocalDate.now().minusYears(10)).build();

        assertThrows(ValidacionNegocioException.class, () -> service.patchCliente(1L, req));
    }

    @Test
    void patch_ClienteInexistente() {
        when(clienteRepository.findByIdAndEliminadoLogicoFalse(9L)).thenReturn(Optional.empty());
        ClientePatchRequest req = ClientePatchRequest.builder().id(9L).build();

        assertThrows(ClienteNoEncontradoException.class, () -> service.patchCliente(9L, req));
    }

    @Test
    void consultas_Individuales() {
        clienteExiste();
        when(clienteRepository.findByCurpAndEliminadoLogicoFalse("C")).thenReturn(Optional.of(cliente));
        when(clienteRepository.findByRfcAndEliminadoLogicoFalse("R")).thenReturn(Optional.of(cliente));
        when(clienteRepository.findByCorreoElectronicoAndEliminadoLogicoFalse("M")).thenReturn(Optional.of(cliente));
        when(clienteRepository.findByNumeroCuentaAndEliminadoLogicoFalse("N")).thenReturn(Optional.of(cliente));

        assertEquals(1L, service.consultarPorId(1L).getId());
        assertEquals(1L, service.consultarPorCurp("C").getId());
        assertEquals(1L, service.consultarPorRfc("R").getId());
        assertEquals(1L, service.consultarPorCorreo("M").getId());
        ClienteResponseDto dto = service.consultarPorNumeroCuenta("N");
        assertEquals("4152311234567890", dto.getCuentaBancaria().getNumeroCuenta());
        assertEquals(4181234567L, dto.getDatosContacto().getTelefonoMovil());
    }

    @Test
    void consultas_IndividualesNoEncontradas() {
        when(clienteRepository.findByIdAndEliminadoLogicoFalse(2L)).thenReturn(Optional.empty());
        when(clienteRepository.findByCurpAndEliminadoLogicoFalse("C")).thenReturn(Optional.empty());
        when(clienteRepository.findByRfcAndEliminadoLogicoFalse("R")).thenReturn(Optional.empty());
        when(clienteRepository.findByCorreoElectronicoAndEliminadoLogicoFalse("M")).thenReturn(Optional.empty());
        when(clienteRepository.findByNumeroCuentaAndEliminadoLogicoFalse("N")).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () -> service.consultarPorId(2L));
        assertThrows(ClienteNoEncontradoException.class, () -> service.consultarPorCurp("C"));
        assertThrows(ClienteNoEncontradoException.class, () -> service.consultarPorRfc("R"));
        assertThrows(ClienteNoEncontradoException.class, () -> service.consultarPorCorreo("M"));
        assertThrows(ClienteNoEncontradoException.class, () -> service.consultarPorNumeroCuenta("N"));
    }

    @Test
    void consultas_Listados() {
        Cliente sinCuentas = Cliente.builder().id(2L).nombre("Ana").telefonoMovil(" ").cuentas(new ArrayList<>()).build();
        List<Cliente> lista = List.of(cliente, sinCuentas);
        when(clienteRepository.findAllByEliminadoLogicoFalse()).thenReturn(lista);
        when(clienteRepository.findByNombreContainingIgnoreCaseAndEliminadoLogicoFalse("J")).thenReturn(lista);
        when(clienteRepository.findByApellidoPaternoContainingIgnoreCaseAndEliminadoLogicoFalse("P")).thenReturn(lista);
        when(clienteRepository.findByApellidoMaternoContainingIgnoreCaseAndEliminadoLogicoFalse("L")).thenReturn(lista);
        when(clienteRepository.findByActivoLoginTrueAndEliminadoLogicoFalse()).thenReturn(lista);
        when(clienteRepository.findByFechaCreacionBetweenAndEliminadoLogicoFalse(any(), any())).thenReturn(lista);

        assertEquals(2, service.consultarTodos().size());
        assertEquals(2, service.buscarPorNombre("J").size());
        assertEquals(2, service.buscarPorApellidoPaterno("P").size());
        assertEquals(2, service.buscarPorApellidoMaterno("L").size());
        assertEquals(2, service.consultarActivos().size());
        List<ClienteResponseDto> porFecha = service.consultarPorRangoFechas(LocalDate.now().minusDays(1), LocalDate.now());
        assertEquals(2, porFecha.size());
        assertNull(porFecha.get(1).getCuentaBancaria());
        assertNull(porFecha.get(1).getDatosContacto().getTelefonoMovil());
    }
}
