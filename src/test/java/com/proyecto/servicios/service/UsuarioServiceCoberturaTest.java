package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.cliente.UsuarioResponseDto;
import com.proyecto.servicios.dto.usuario.UsuarioActualizacionRequest;
import com.proyecto.servicios.dto.usuario.UsuarioBajaRequest;
import com.proyecto.servicios.dto.usuario.UsuarioConsultaIdRequest;
import com.proyecto.servicios.dto.usuario.UsuarioCreacionRequest;
import com.proyecto.servicios.dto.usuario.UsuarioFiltroRequest;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.exception.ContrasenaInvalidaException;
import com.proyecto.servicios.exception.CorreoElectronicoDuplicadoException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.repository.ClienteRepository;
import com.proyecto.servicios.repository.UsuarioRepository;
import com.proyecto.servicios.service.Impl.UsuarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceCoberturaTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl service;

    private Cliente cliente;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder().id(10L).nombre("Maria").activoLogin(true).eliminadoLogico(false).build();
        usuario = Usuario.builder().id(1L).correo("maria@example.com").password("hash").activo(true).cliente(cliente).build();
    }

    private void guardaEco() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void crear_SinClienteUsaClienteLibre() {
        when(clienteRepository.findAllByEliminadoLogicoFalse()).thenReturn(List.of(cliente));
        when(usuarioRepository.findByClienteId(10L)).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreo("nuevo@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hash");
        guardaEco();

        UsuarioResponseDto dto = service.crearUsuario(UsuarioCreacionRequest.builder()
                .correo("nuevo@example.com").password("Password123!").build());

        assertEquals(10L, dto.getClienteId());
        assertEquals("nuevo@example.com", dto.getCorreo());
    }

    @Test
    void crear_SinClienteLibreCreaClienteDeSistema() {
        when(clienteRepository.findAllByEliminadoLogicoFalse()).thenReturn(Collections.emptyList());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> {
            Cliente c = i.getArgument(0);
            c.setId(99L);
            return c;
        });
        when(usuarioRepository.existsByCorreo("nuevo@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hash");
        guardaEco();

        UsuarioResponseDto dto = service.crearUsuario(UsuarioCreacionRequest.builder()
                .correo("nuevo@example.com").password("Password123!").build());

        assertEquals(99L, dto.getClienteId());
    }

    @Test
    void crear_ContrasenaDebil() {
        UsuarioCreacionRequest req = UsuarioCreacionRequest.builder().correo("a@example.com").password("debil").build();
        assertThrows(ContrasenaInvalidaException.class, () -> service.crearUsuario(req));
    }

    @Test
    void consultarUsuarios_ConFiltroUsaEspecificacion() {
        when(usuarioRepository.findAll(any(Specification.class))).thenReturn(List.of(usuario));

        List<UsuarioResponseDto> lista = service.consultarUsuarios(UsuarioFiltroRequest.builder().activo(true).build());

        assertEquals(1, lista.size());
    }

    @Test
    void consultarUsuarios_FiltroNulo() {
        when(usuarioRepository.findAll()).thenReturn(List.of(Usuario.builder().id(2L).correo("x").build()));

        List<UsuarioResponseDto> lista = service.consultarUsuarios(null);

        assertNull(lista.get(0).getClienteId());
    }

    @Test
    void consultarPorId() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        assertEquals(1L, service.consultarPorId(new UsuarioConsultaIdRequest(1L)).getId());

        when(usuarioRepository.findById(2L)).thenReturn(Optional.empty());
        UsuarioConsultaIdRequest req = new UsuarioConsultaIdRequest(2L);
        assertThrows(UsuarioNoEncontradoException.class, () -> service.consultarPorId(req));
    }

    @Test
    void actualizar_TodosLosCampos() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByCorreo("otro@example.com")).thenReturn(false);
        when(passwordEncoder.encode("NuevaClave1!")).thenReturn("nuevoHash");
        guardaEco();

        UsuarioResponseDto dto = service.actualizarUsuario(UsuarioActualizacionRequest.builder()
                .id(1L).correo(" otro@example.com ").password("NuevaClave1!").activo(false).build());

        assertEquals("otro@example.com", dto.getCorreo());
        assertEquals("nuevoHash", usuario.getPassword());
        assertFalse(dto.getActivo());
    }

    @Test
    void actualizar_MismoCorreoYSinCambios() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        guardaEco();

        service.actualizarUsuario(UsuarioActualizacionRequest.builder().id(1L).correo("MARIA@example.com").password(" ").build());
        service.actualizarUsuario(UsuarioActualizacionRequest.builder().id(1L).build());

        assertEquals("maria@example.com", usuario.getCorreo());
        assertEquals("hash", usuario.getPassword());
        verify(usuarioRepository, never()).existsByCorreo(any());
    }

    @Test
    void actualizar_CorreoDuplicado() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByCorreo("otro@example.com")).thenReturn(true);
        UsuarioActualizacionRequest req = UsuarioActualizacionRequest.builder().id(1L).correo("otro@example.com").build();

        assertThrows(CorreoElectronicoDuplicadoException.class, () -> service.actualizarUsuario(req));
    }

    @Test
    void actualizar_NoEncontrado() {
        when(usuarioRepository.findById(5L)).thenReturn(Optional.empty());
        UsuarioActualizacionRequest req = UsuarioActualizacionRequest.builder().id(5L).build();

        assertThrows(UsuarioNoEncontradoException.class, () -> service.actualizarUsuario(req));
    }

    @Test
    void baja_DesactivaUsuario() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        guardaEco();

        UsuarioResponseDto dto = service.bajaUsuario(new UsuarioBajaRequest(1L));

        assertFalse(dto.getActivo());
        assertSame(Boolean.FALSE, usuario.getActivo());
    }

    @Test
    void baja_NoEncontrado() {
        when(usuarioRepository.findById(5L)).thenReturn(Optional.empty());
        UsuarioBajaRequest req = new UsuarioBajaRequest(5L);

        assertThrows(UsuarioNoEncontradoException.class, () -> service.bajaUsuario(req));
    }
}
