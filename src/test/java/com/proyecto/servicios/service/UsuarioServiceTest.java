package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.cliente.AgregarUsuarioRequest;
import com.proyecto.servicios.dto.cliente.UsuarioResponseDto;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ContrasenaInvalidaException;
import com.proyecto.servicios.exception.CorreoElectronicoDuplicadoException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.repository.ClienteRepository;
import com.proyecto.servicios.repository.UsuarioRepository;
import com.proyecto.servicios.service.Impl.UsuarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private Cliente clienteMock;
    private Usuario usuarioMock;

    @BeforeEach
    void setUp() {
        clienteMock = Cliente.builder().id(10L).nombre("Maria").activoLogin(true).eliminadoLogico(false).build();
        usuarioMock = Usuario.builder().id(1L).correo("maria@example.com").activo(true).cliente(clienteMock).build();
    }

    @Test
    void testConsultarConFiltro_Exito() {
        when(usuarioRepository.findAll()).thenReturn(Collections.singletonList(usuarioMock));

        List<UsuarioResponseDto> lista = usuarioService.consultarConFiltro(null, null);

        assertNotNull(lista);
        assertEquals(1, lista.size());
        assertEquals("maria@example.com", lista.get(0).getCorreo());
    }

    @Test
    void testAgregarUsuario_Exito() {
        when(clienteRepository.findByIdAndEliminadoLogicoFalse(10L)).thenReturn(Optional.of(clienteMock));
        when(usuarioRepository.findByClienteId(10L)).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo("maria@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId(1L);
            return u;
        });

        AgregarUsuarioRequest request = AgregarUsuarioRequest.builder()
                .clienteId(10L)
                .correo("maria@example.com")
                .password("Password123!")
                .build();

        UsuarioResponseDto response = usuarioService.agregarUsuario(request);

        assertNotNull(response);
        assertEquals("maria@example.com", response.getCorreo());
        assertTrue(response.getActivo());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void testAgregarUsuario_ContrasenaInvalida() {
        AgregarUsuarioRequest request = AgregarUsuarioRequest.builder()
                .clienteId(10L)
                .correo("maria@example.com")
                .password("simple")
                .build();

        assertThrows(ContrasenaInvalidaException.class, () -> usuarioService.agregarUsuario(request));
    }

    @Test
    void testAgregarUsuario_ClienteNoEncontrado() {
        when(clienteRepository.findByIdAndEliminadoLogicoFalse(99L)).thenReturn(Optional.empty());

        AgregarUsuarioRequest request = AgregarUsuarioRequest.builder()
                .clienteId(99L)
                .correo("test@example.com")
                .password("Password123!")
                .build();

        assertThrows(ClienteNoEncontradoException.class, () -> usuarioService.agregarUsuario(request));
    }

    @Test
    void testAgregarUsuario_ClienteYaTieneUsuario() {
        when(clienteRepository.findByIdAndEliminadoLogicoFalse(10L)).thenReturn(Optional.of(clienteMock));
        when(usuarioRepository.findByClienteId(10L)).thenReturn(Optional.of(usuarioMock));

        AgregarUsuarioRequest request = AgregarUsuarioRequest.builder()
                .clienteId(10L)
                .correo("maria2@example.com")
                .password("Password123!")
                .build();

        assertThrows(ValidacionNegocioException.class, () -> usuarioService.agregarUsuario(request));
    }

    @Test
    void testAgregarUsuario_CorreoDuplicado() {
        when(clienteRepository.findByIdAndEliminadoLogicoFalse(10L)).thenReturn(Optional.of(clienteMock));
        when(usuarioRepository.findByClienteId(10L)).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo("maria@example.com")).thenReturn(true);

        AgregarUsuarioRequest request = AgregarUsuarioRequest.builder()
                .clienteId(10L)
                .correo("maria@example.com")
                .password("Password123!")
                .build();

        assertThrows(CorreoElectronicoDuplicadoException.class, () -> usuarioService.agregarUsuario(request));
    }
}

