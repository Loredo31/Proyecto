package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.dto.cliente.LoginRequest;
import com.proyecto.servicios.dto.cliente.LoginResponseDto;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.repository.UsuarioRepository;
import com.proyecto.servicios.service.AuthService;
import com.proyecto.servicios.util.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con correo: " + request.getCorreo()));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            log.warn("Intento de login con usuario inactivo: {}", request.getCorreo());
            throw new UsuarioInactivoException("El usuario se encuentra inactivo");
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            log.warn("Credenciales incorrectas para usuario: {}", request.getCorreo());
            throw new CredencialesInvalidasException("Credenciales de acceso inválidas");
        }

        Long clienteId = (usuario.getCliente() != null) ? usuario.getCliente().getId() : null;
        String token = jwtTokenService.generarToken(usuario.getCorreo(), clienteId);

        log.info("Login exitoso para usuario: {}", usuario.getCorreo());

        return LoginResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .correo(usuario.getCorreo())
                .clienteId(clienteId)
                .expiraEnSegundos(jwtTokenService.getExpirationSeconds())
                .build();
    }
}

