package com.proyecto.servicios.service.Impl;

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
import com.proyecto.servicios.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$";

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponseDto> consultarConFiltro(String correo, Boolean activo) {
        List<Usuario> list;
        if (correo != null && !correo.isBlank() && activo != null) {
            list = usuarioRepository.findByCorreoContainingIgnoreCaseAndActivo(correo, activo);
        } else if (correo != null && !correo.isBlank()) {
            list = usuarioRepository.findByCorreoContainingIgnoreCase(correo);
        } else if (activo != null) {
            list = usuarioRepository.findByActivo(activo);
        } else {
            list = usuarioRepository.findAll();
        }

        return list.stream()
                .map(this::mapearDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UsuarioResponseDto agregarUsuario(AgregarUsuarioRequest request) {
        validarReglasContrasena(request.getPassword());

        Cliente cliente = clienteRepository.findByIdAndEliminadoLogicoFalse(request.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID: " + request.getClienteId()));

        if (usuarioRepository.findByClienteId(cliente.getId()).isPresent()) {
            throw new ValidacionNegocioException("El cliente ya tiene un usuario de acceso asociado");
        }

        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new CorreoElectronicoDuplicadoException("El correo ya se encuentra registrado para otro usuario: " + request.getCorreo());
        }

        Usuario usuario = Usuario.builder()
                .correo(request.getCorreo())
                .password(passwordEncoder.encode(request.getPassword()))
                .activo(true)
                .cliente(cliente)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        usuario = usuarioRepository.save(usuario);
        cliente.setUsuario(usuario);

        log.info("Usuario agregado exitosamente con correo: {} para cliente ID: {}", usuario.getCorreo(), cliente.getId());
        return mapearDto(usuario);
    }

    private void validarReglasContrasena(String password) {
        if (password == null || !password.matches(PASSWORD_PATTERN)) {
            throw new ContrasenaInvalidaException(
                    "La contraseña debe contener al menos 8 caracteres, una letra mayúscula, una minúscula, un número y un carácter especial"
            );
        }
    }

    private UsuarioResponseDto mapearDto(Usuario u) {
        return UsuarioResponseDto.builder()
                .id(u.getId())
                .clienteId(u.getCliente() != null ? u.getCliente().getId() : null)
                .correo(u.getCorreo())
                .activo(u.getActivo())
                .fechaCreacion(u.getFechaCreacion())
                .fechaActualizacion(u.getFechaActualizacion())
                .build();
    }
}

