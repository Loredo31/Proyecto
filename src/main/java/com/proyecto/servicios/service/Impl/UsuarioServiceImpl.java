package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.dto.cliente.AgregarUsuarioRequest;
import com.proyecto.servicios.dto.cliente.UsuarioResponseDto;
import com.proyecto.servicios.dto.usuario.UsuarioActualizacionRequest;
import com.proyecto.servicios.dto.usuario.UsuarioBajaRequest;
import com.proyecto.servicios.dto.usuario.UsuarioConsultaIdRequest;
import com.proyecto.servicios.dto.usuario.UsuarioCreacionRequest;
import com.proyecto.servicios.dto.usuario.UsuarioFiltroRequest;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ContrasenaInvalidaException;
import com.proyecto.servicios.exception.CorreoElectronicoDuplicadoException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.repository.ClienteRepository;
import com.proyecto.servicios.repository.UsuarioRepository;
import com.proyecto.servicios.service.UsuarioService;
import com.proyecto.servicios.specification.UsuarioSpecification;
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
    @Transactional
    public UsuarioResponseDto crearUsuario(UsuarioCreacionRequest request) {
        log.info("Iniciando creación de usuario para cliente ID: {} con correo: {}", request.getClienteId(), request.getCorreo());
        validarReglasContrasena(request.getPassword());

        Cliente cliente;
        if (request.getClienteId() != null) {
            cliente = clienteRepository.findByIdAndEliminadoLogicoFalse(request.getClienteId())
                    .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID: " + request.getClienteId()));

            if (usuarioRepository.findByClienteId(cliente.getId()).isPresent()) {
                throw new ValidacionNegocioException("El cliente ya tiene un usuario de acceso asociado");
            }
        } else {
            cliente = clienteRepository.findAllByEliminadoLogicoFalse().stream()
                    .filter(c -> usuarioRepository.findByClienteId(c.getId()).isEmpty())
                    .findFirst()
                    .orElseGet(() -> {
                        String sufijo = String.valueOf(System.currentTimeMillis() % 1000000);
                        return clienteRepository.save(Cliente.builder()
                                .nombre("Usuario")
                                .apellidoPaterno("Sistema")
                                .apellidoMaterno("General")
                                .curp("SIST" + sufijo + "HDFRRN01")
                                .rfc("SIST" + sufijo.substring(0, Math.min(6, sufijo.length())) + "01")
                                .sexo(com.proyecto.servicios.enums.Sexo.MASCULINO)
                                .nacionalidad("Mexicana")
                                .estadoCivil(com.proyecto.servicios.enums.EstadoCivil.SOLTERO)
                                .correoElectronico(request.getCorreo())
                                .telefonoMovil(String.format("%010d", Math.abs((request.getCorreo() + sufijo).hashCode() % 10000000000L)))
                                .informacionLaboral(com.proyecto.servicios.entity.cliente.InformacionLaboral.builder()
                                        .ocupacion("Empleado")
                                        .empresa("Empresa")
                                        .ingresoMensual(java.math.BigDecimal.valueOf(15000))
                                        .build())
                                .activoLogin(true)
                                .eliminadoLogico(false)
                                .fechaNacimiento(java.time.LocalDate.of(1995, 1, 1))
                                .build());
                    });
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

        Usuario guardado = usuarioRepository.save(usuario);
        cliente.setUsuario(guardado);

        log.info("Usuario creado exitosamente con ID: {} para correo: {}", guardado.getId(), guardado.getCorreo());
        return mapearDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponseDto> consultarUsuarios(UsuarioFiltroRequest filtro) {
        log.info("Consultando usuarios con especificación dinámica de filtros");
        List<Usuario> usuarios = (filtro == null || esFiltroVacio(filtro))
                ? usuarioRepository.findAll()
                : usuarioRepository.findAll(UsuarioSpecification.conFiltros(filtro));
        return usuarios.stream()
                .map(this::mapearDto)
                .collect(Collectors.toList());
    }

    private boolean esFiltroVacio(UsuarioFiltroRequest f) {
        return f.getId() == null
                && f.getClienteId() == null
                && (f.getCorreo() == null || f.getCorreo().isBlank())
                && f.getActivo() == null
                && f.getFechaInicio() == null
                && f.getFechaFin() == null;
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDto consultarPorId(UsuarioConsultaIdRequest request) {
        log.info("Consultando usuario por ID: {}", request.getId());
        Usuario usuario = usuarioRepository.findById(request.getId())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con ID: " + request.getId()));
        return mapearDto(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponseDto actualizarUsuario(UsuarioActualizacionRequest request) {
        log.info("Iniciando actualización de usuario ID: {}", request.getId());
        Usuario usuario = usuarioRepository.findById(request.getId())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con ID: " + request.getId()));

        if (request.getCorreo() != null && !request.getCorreo().isBlank()) {
            String nuevoCorreo = request.getCorreo().trim();
            if (!nuevoCorreo.equalsIgnoreCase(usuario.getCorreo())) {
                if (usuarioRepository.existsByCorreo(nuevoCorreo)) {
                    throw new CorreoElectronicoDuplicadoException("El correo ya se encuentra registrado para otro usuario: " + nuevoCorreo);
                }
                usuario.setCorreo(nuevoCorreo);
            }
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            validarReglasContrasena(request.getPassword());
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getActivo() != null) {
            usuario.setActivo(request.getActivo());
        }

        usuario.setFechaActualizacion(LocalDateTime.now());
        Usuario actualizado = usuarioRepository.save(usuario);

        log.info("Usuario ID: {} actualizado exitosamente", actualizado.getId());
        return mapearDto(actualizado);
    }

    @Override
    @Transactional
    public UsuarioResponseDto bajaUsuario(UsuarioBajaRequest request) {
        log.info("Iniciando baja lógica de usuario ID: {}", request.getId());
        Usuario usuario = usuarioRepository.findById(request.getId())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con ID: " + request.getId()));

        usuario.setActivo(false);
        usuario.setFechaActualizacion(LocalDateTime.now());
        Usuario guardado = usuarioRepository.save(usuario);

        log.info("Usuario ID: {} dado de baja exitosamente", guardado.getId());
        return mapearDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponseDto> consultarConFiltro(String correo, Boolean activo) {
        UsuarioFiltroRequest filtro = UsuarioFiltroRequest.builder()
                .correo(correo)
                .activo(activo)
                .build();
        return consultarUsuarios(filtro);
    }

    @Override
    @Transactional
    public UsuarioResponseDto agregarUsuario(AgregarUsuarioRequest request) {
        UsuarioCreacionRequest creacionRequest = UsuarioCreacionRequest.builder()
                .clienteId(request.getClienteId())
                .correo(request.getCorreo())
                .password(request.getPassword())
                .build();
        return crearUsuario(creacionRequest);
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
