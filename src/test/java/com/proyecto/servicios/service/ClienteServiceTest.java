package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.cliente.ClienteActualizacionRequest;
import com.proyecto.servicios.dto.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.dto.cliente.ClienteResponseDto;
import com.proyecto.servicios.dto.cliente.DatosContactoDto;
import com.proyecto.servicios.dto.cliente.DatosPersonalesActualizacionDto;
import com.proyecto.servicios.dto.cliente.DatosPersonalesDto;
import com.proyecto.servicios.dto.cliente.DomicilioDto;
import com.proyecto.servicios.dto.cliente.InformacionLaboralDto;
import com.proyecto.servicios.entity.cliente.BiometriaInfo;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.CuentaBancaria;
import com.proyecto.servicios.entity.cliente.Domicilio;
import com.proyecto.servicios.entity.cliente.InformacionLaboral;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.EstadoRepublica;
import com.proyecto.servicios.enums.EstadoCuenta;
import com.proyecto.servicios.enums.Sexo;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private CuentaBancariaRepository cuentaBancariaRepository;

    @Mock
    private GeneradorCuentaBancaria generadorCuentaBancaria;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private ClienteRegistroRequest request;
    private ClienteActualizacionRequest updateRequest;
    private Cliente clienteMock;
    private CuentaBancaria cuentaMock;
    private Usuario usuarioMock;

    @BeforeEach
    void setUp() {
        DatosPersonalesDto datosPersonales = DatosPersonalesDto.builder()
                .nombre("Juan")
                .segundoNombre("Carlos")
                .apellidoPaterno("Pérez")
                .apellidoMaterno("López")
                .fechaNacimiento(LocalDate.of(1995, 8, 15))
                .curp("PELJ950815HDFRPR01")
                .rfc("PELJ9508151A0")
                .sexo(Sexo.MASCULINO)
                .nacionalidad("MEXICANA")
                .estadoCivil(EstadoCivil.SOLTERO)
                .build();

        DatosContactoDto datosContacto = DatosContactoDto.builder()
                .correoElectronico("juan.perez@example.com")
                .telefonoMovil("4181234567")
                .telefonoAlternativo("4187654321")
                .build();

        DomicilioDto domicilio = DomicilioDto.builder()
                .calle("Av. Universidad")
                .numeroExterior(123)
                .numeroInterior(2)
                .colonia("Centro")
                .municipio("Dolores Hidalgo")
                .estado(EstadoRepublica.GUANAJUATO)
                .codigoPostal("37800")
                .pais("MÉXICO")
                .build();

        InformacionLaboralDto laboral = InformacionLaboralDto.builder()
                .ocupacion("Ingeniero de Software")
                .empresa("Tech Solutions")
                .ingresoMensual(new BigDecimal("25000.00"))
                .build();

        request = ClienteRegistroRequest.builder()
                .datosPersonales(datosPersonales)
                .datosContacto(datosContacto)
                .domicilio(domicilio)
                .informacionLaboral(laboral)
                .password("Password123!")
                .build();

        clienteMock = Cliente.builder()
                .id(1L)
                .nombre("Juan")
                .segundoNombre("Carlos")
                .apellidoPaterno("Pérez")
                .apellidoMaterno("López")
                .fechaNacimiento(LocalDate.of(1995, 8, 15))
                .curp("PELJ950815HDFRPR01")
                .rfc("PELJ9508151A0")
                .sexo(Sexo.MASCULINO)
                .nacionalidad("MEXICANA")
                .estadoCivil(EstadoCivil.SOLTERO)
                .correoElectronico("juan.perez@example.com")
                .telefonoMovil("4181234567")
                .telefonoAlternativo("4187654321")
                .domicilio(Domicilio.builder()
                        .calle("Av. Universidad")
                        .numeroExterior(123)
                        .numeroInterior(2)
                        .colonia("Centro")
                        .municipio("Dolores Hidalgo")
                        .estado(EstadoRepublica.GUANAJUATO)
                        .codigoPostal("37800")
                        .pais("MÉXICO")
                        .build())
                .informacionLaboral(InformacionLaboral.builder()
                        .ocupacion("Ingeniero de Software")
                        .empresa("Tech Solutions")
                        .ingresoMensual(new BigDecimal("25000.00"))
                        .build())
                .biometriaInfo(new BiometriaInfo())
                .activoLogin(true)
                .eliminadoLogico(false)
                .cuentas(new ArrayList<>())
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        cuentaMock = CuentaBancaria.builder()
                .id(10L)
                .numeroCuenta("4152311234567890")
                .saldo(BigDecimal.ZERO)
                .estado(EstadoCuenta.ACTIVA)
                .cliente(clienteMock)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        usuarioMock = Usuario.builder()
                .id(20L)
                .correo("juan.perez@example.com")
                .password("$2a$10$hashedpassword")
                .activo(true)
                .cliente(clienteMock)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        clienteMock.getCuentas().add(cuentaMock);
        clienteMock.setUsuario(usuarioMock);

        DatosPersonalesActualizacionDto datosPersonalesActualizacion = DatosPersonalesActualizacionDto.builder()
                .nombre("Juan Actualizado")
                .segundoNombre("Carlos")
                .apellidoPaterno("Pérez")
                .apellidoMaterno("López")
                .fechaNacimiento(LocalDate.of(1995, 8, 15))
                .sexo(Sexo.MASCULINO)
                .nacionalidad("MEXICANA")
                .estadoCivil(EstadoCivil.CASADO)
                .build();

        DatosContactoDto datosContactoActualizados = DatosContactoDto.builder()
                .correoElectronico("juan.nuevo@example.com")
                .telefonoMovil("4189998877")
                .telefonoAlternativo("4181112233")
                .build();

        updateRequest = ClienteActualizacionRequest.builder()
                .datosPersonales(datosPersonalesActualizacion)
                .datosContacto(datosContactoActualizados)
                .domicilio(domicilio)
                .informacionLaboral(laboral)
                .build();
    }

    @Test
    void testRegistrarCliente_Exito() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByTelefonoMovil(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreoElectronico(anyString())).thenReturn(false);

        String mockCuenta = "4152311234567890";
        when(generadorCuentaBancaria.generarNumeroCuenta()).thenReturn(mockCuenta);
        when(cuentaBancariaRepository.existsByNumeroCuenta(mockCuenta)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$mockHashedPassword");

        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente c = invocation.getArgument(0);
            if (c.getId() == null) c.setId(1L);
            return c;
        });

        ClienteResponseDto response = clienteService.registrarCliente(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Juan", response.getDatosPersonales().getNombre());
        assertEquals("PELJ950815HDFRPR01", response.getDatosPersonales().getCurp());
        assertNotNull(response.getCuentaBancaria());
        assertEquals(mockCuenta, response.getCuentaBancaria().getNumeroCuenta());
        assertEquals(BigDecimal.ZERO, response.getCuentaBancaria().getSaldo());
        assertEquals(EstadoCuenta.ACTIVA, response.getCuentaBancaria().getEstado());
        assertNotNull(response.getUsuario());
        assertEquals("juan.perez@example.com", response.getUsuario().getUsername());
        assertTrue(response.getUsuario().getActivo());
        assertTrue(response.getActivoLogin());
    }

    @Test
    void testRegistrarCliente_MenorDeEdad_LanzaExcepcion() {
        request.getDatosPersonales().setFechaNacimiento(LocalDate.now().minusYears(17));

        assertThrows(ValidacionNegocioException.class, () -> clienteService.registrarCliente(request));
    }

    @Test
    void testRegistrarCliente_CurpDuplicado_LanzaExcepcion() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(true);

        assertThrows(CurpDuplicadaException.class, () -> clienteService.registrarCliente(request));
    }

    @Test
    void testRegistrarCliente_RfcDuplicado_LanzaExcepcion() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(true);

        assertThrows(RfcDuplicadoException.class, () -> clienteService.registrarCliente(request));
    }

    @Test
    void testActualizarCliente_Exito() {
        when(clienteRepository.findByIdAndEliminadoLogicoFalse(1L)).thenReturn(Optional.of(clienteMock));
        when(clienteRepository.existsByTelefonoMovilAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(clienteRepository.existsByCorreoElectronicoAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClienteResponseDto response = clienteService.actualizarCliente(1L, updateRequest);

        assertNotNull(response);
        assertEquals("Juan Actualizado", response.getDatosPersonales().getNombre());
        assertEquals("PELJ950815HDFRPR01", response.getDatosPersonales().getCurp());
        assertEquals("PELJ9508151A0", response.getDatosPersonales().getRfc());
        assertEquals("juan.nuevo@example.com", response.getUsuario().getUsername());
    }

    @Test
    void testBajaLogica_Exito() {
        when(clienteRepository.findByIdAndEliminadoLogicoFalse(1L)).thenReturn(Optional.of(clienteMock));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        clienteService.bajaLogica(1L);

        assertTrue(clienteMock.getEliminadoLogico());
        assertFalse(clienteMock.getActivoLogin());
        assertFalse(clienteMock.getUsuario().getActivo());
        assertEquals(EstadoCuenta.CANCELADA, cuentaMock.getEstado());
        verify(clienteRepository).save(clienteMock);
    }
}

