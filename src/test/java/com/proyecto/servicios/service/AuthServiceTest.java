package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.cliente.LoginRequest;
import com.proyecto.servicios.dto.cliente.LoginResponseDto;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.repository.UsuarioRepository;
import com.proyecto.servicios.service.Impl.AuthServiceImpl;
import com.proyecto.servicios.util.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Usuario usuarioMock;

    @BeforeEach
    void setUp() {
        Cliente cliente = Cliente.builder().id(5L).nombre("Juan").build();
        usuarioMock = Usuario.builder()
                .id(1L)
                .correo("juan.perez@example.com")
                .password("$2a$10$hashedPassword")
                .activo(true)
                .cliente(cliente)
                .build();
    }

    @Test
    void testLogin_Exito() {
        when(usuarioRepository.findByCorreo("juan.perez@example.com")).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("Password123!", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtTokenService.generarToken(anyString(), anyLong())).thenReturn("mock.jwt.token");
        when(jwtTokenService.getExpirationSeconds()).thenReturn(86400L);

        LoginRequest request = LoginRequest.builder()
                .correo("juan.perez@example.com")
                .password("Password123!")
                .build();

        LoginResponseDto response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("juan.perez@example.com", response.getCorreo());
        assertEquals(5L, response.getClienteId());
    }

    @Test
    void testLogin_UsuarioNoEncontrado() {
        when(usuarioRepository.findByCorreo("noexiste@example.com")).thenReturn(Optional.empty());

        LoginRequest request = LoginRequest.builder()
                .correo("noexiste@example.com")
                .password("Password123!")
                .build();

        assertThrows(UsuarioNoEncontradoException.class, () -> authService.login(request));
    }

    @Test
    void testLogin_UsuarioInactivo() {
        usuarioMock.setActivo(false);
        when(usuarioRepository.findByCorreo("juan.perez@example.com")).thenReturn(Optional.of(usuarioMock));

        LoginRequest request = LoginRequest.builder()
                .correo("juan.perez@example.com")
                .password("Password123!")
                .build();

        assertThrows(UsuarioInactivoException.class, () -> authService.login(request));
    }

    @Test
    void testLogin_CredencialesInvalidas() {
        when(usuarioRepository.findByCorreo("juan.perez@example.com")).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("WrongPassword", "$2a$10$hashedPassword")).thenReturn(false);

        LoginRequest request = LoginRequest.builder()
                .correo("juan.perez@example.com")
                .password("WrongPassword")
                .build();

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(request));
    }
}

