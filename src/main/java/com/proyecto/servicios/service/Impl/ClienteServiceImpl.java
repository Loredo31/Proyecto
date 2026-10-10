package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.dto.cliente.ClienteActualizacionRequest;
import com.proyecto.servicios.dto.cliente.ClientePatchRequest;
import com.proyecto.servicios.dto.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.dto.cliente.ClienteResponseDto;
import com.proyecto.servicios.dto.cliente.CuentaBancariaResponseDto;
import com.proyecto.servicios.dto.cliente.DatosContactoDto;
import com.proyecto.servicios.dto.cliente.DatosPersonalesActualizacionDto;
import com.proyecto.servicios.dto.cliente.DatosPersonalesDto;
import com.proyecto.servicios.dto.cliente.DomicilioDto;
import com.proyecto.servicios.dto.cliente.InformacionLaboralDto;
import com.proyecto.servicios.dto.cliente.UsuarioResponseDto;
import com.proyecto.servicios.entity.cliente.BiometriaInfo;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.CuentaBancaria;
import com.proyecto.servicios.entity.cliente.Domicilio;
import com.proyecto.servicios.entity.cliente.InformacionLaboral;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.enums.EstadoCuenta;
import com.proyecto.servicios.enums.EstadoRepublica;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.repository.ClienteRepository;
import com.proyecto.servicios.repository.CuentaBancariaRepository;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.util.GeneradorCuentaBancaria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$";

    private final ClienteRepository clienteRepository;
    private final CuentaBancariaRepository cuentaBancariaRepository;
    private final GeneradorCuentaBancaria generadorCuentaBancaria;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public ClienteResponseDto registrarCliente(ClienteRegistroRequest request) {
                validarMayorDeEdad(request.getDatosPersonales().getFechaNacimiento());
        validarReglasContrasena(request.getPassword());
        validarCodigoPostalYEstado(request.getDomicilio().getCodigoPostal(), request.getDomicilio().getEstado());
        validarUnicidad(request);

        Cliente cliente = mapearAEntidad(request);
        cliente = clienteRepository.save(cliente);

        Domicilio domicilio = Domicilio.builder()
                .cliente(cliente)
                .calle(request.getDomicilio().getCalle())
                .numeroExterior(request.getDomicilio().getNumeroExterior())
                .numeroInterior(request.getDomicilio().getNumeroInterior())
                .colonia(request.getDomicilio().getColonia())
                .municipio(request.getDomicilio().getMunicipio())
                .estado(request.getDomicilio().getEstado())
                .codigoPostal(request.getDomicilio().getCodigoPostal())
                .pais(request.getDomicilio().getPais())
                .build();
        cliente.setDomicilio(domicilio);

        String numeroCuenta = generarNumeroCuentaUnico();
        CuentaBancaria cuenta = CuentaBancaria.builder()
                .numeroCuenta(numeroCuenta)
                .saldo(BigDecimal.ZERO)
                .estado(EstadoCuenta.ACTIVA)
                .cliente(cliente)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
        cliente.getCuentas().add(cuenta);

        Usuario usuario = Usuario.builder()
                .correo(cliente.getCorreoElectronico())
                .password(passwordEncoder.encode(request.getPassword()))
                .activo(true)
                .cliente(cliente)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
        cliente.setUsuario(usuario);

        cliente = clienteRepository.save(cliente);

        log.info("Cliente registrado con ID: {}, cuenta bancaria: {} y usuario: {}", cliente.getId(), numeroCuenta, usuario.getUsername());
        return mapearAResponseDto(cliente);
    }

    @Override
    @Transactional
    public ClienteResponseDto actualizarCliente(Long id, ClienteActualizacionRequest request) {
        Cliente cliente = clienteRepository.findByIdAndEliminadoLogicoFalse(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID: " + id));

        validarMayorDeEdad(request.getDatosPersonales().getFechaNacimiento());
        validarCodigoPostalYEstado(request.getDomicilio().getCodigoPostal(), request.getDomicilio().getEstado());

        String nuevoTelefono = request.getDatosContacto().getTelefonoMovil();
        if (clienteRepository.existsByTelefonoMovilAndIdNot(nuevoTelefono, id)) {
            throw new ValidacionNegocioException("El teléfono ya se encuentra registrado por otro cliente");
        }

        String nuevoCorreo = request.getDatosContacto().getCorreoElectronico();
        if (clienteRepository.existsByCorreoElectronicoAndIdNot(nuevoCorreo, id)) {
            throw new ValidacionNegocioException("El correo ya se encuentra registrado por otro cliente");
        }

        DatosPersonalesActualizacionDto p = request.getDatosPersonales();
        cliente.setNombre(p.getNombre());
        cliente.setSegundoNombre(p.getSegundoNombre());
        cliente.setApellidoPaterno(p.getApellidoPaterno());
        cliente.setApellidoMaterno(p.getApellidoMaterno());
        cliente.setFechaNacimiento(p.getFechaNacimiento());
        cliente.setSexo(p.getSexo());
        cliente.setNacionalidad(p.getNacionalidad());
        cliente.setEstadoCivil(p.getEstadoCivil());

        DatosContactoDto c = request.getDatosContacto();
        cliente.setCorreoElectronico(c.getCorreoElectronico());
        cliente.setTelefonoMovil(c.getTelefonoMovil());
        cliente.setTelefonoAlternativo(c.getTelefonoAlternativo());

        if (cliente.getUsuario() != null) {
            cliente.getUsuario().setCorreo(c.getCorreoElectronico());
            cliente.getUsuario().setFechaActualizacion(LocalDateTime.now());
        }

        DomicilioDto d = request.getDomicilio();
        if (cliente.getDomicilio() != null) {
            cliente.getDomicilio().setCalle(d.getCalle());
            cliente.getDomicilio().setNumeroExterior(d.getNumeroExterior());
            cliente.getDomicilio().setNumeroInterior(d.getNumeroInterior());
            cliente.getDomicilio().setColonia(d.getColonia());
            cliente.getDomicilio().setMunicipio(d.getMunicipio());
            cliente.getDomicilio().setEstado(d.getEstado());
            cliente.getDomicilio().setCodigoPostal(d.getCodigoPostal());
            cliente.getDomicilio().setPais(d.getPais());
        }

        InformacionLaboralDto l = request.getInformacionLaboral();
        cliente.setInformacionLaboral(InformacionLaboral.builder()
                .ocupacion(l.getOcupacion())
                .empresa(l.getEmpresa())
                .ingresoMensual(l.getIngresoMensual())
                .build());

        cliente = clienteRepository.save(cliente);
        log.info("Cliente con ID: {} actualizado exitosamente", id);

        return mapearAResponseDto(cliente);
    }

    @Override
    @Transactional
    public ClienteResponseDto patchCliente(Long id, ClientePatchRequest request) {
        Cliente cliente = clienteRepository.findByIdAndEliminadoLogicoFalse(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID: " + id));

        if (request.getNombre() != null) cliente.setNombre(request.getNombre());
        if (request.getSegundoNombre() != null) cliente.setSegundoNombre(request.getSegundoNombre());
        if (request.getApellidoPaterno() != null) cliente.setApellidoPaterno(request.getApellidoPaterno());
        if (request.getApellidoMaterno() != null) cliente.setApellidoMaterno(request.getApellidoMaterno());
        if (request.getFechaNacimiento() != null) {
            validarMayorDeEdad(request.getFechaNacimiento());
            cliente.setFechaNacimiento(request.getFechaNacimiento());
        }
        if (request.getSexo() != null) cliente.setSexo(request.getSexo());
        if (request.getNacionalidad() != null) cliente.setNacionalidad(request.getNacionalidad());
        if (request.getEstadoCivil() != null) cliente.setEstadoCivil(request.getEstadoCivil());

        if (request.getTelefonoMovil() != null) {
            if (clienteRepository.existsByTelefonoMovilAndIdNot(request.getTelefonoMovil(), id)) {
                throw new ValidacionNegocioException("El teléfono ya se encuentra registrado por otro cliente");
            }
            cliente.setTelefonoMovil(request.getTelefonoMovil());
        }
        if (request.getTelefonoAlternativo() != null) {
            cliente.setTelefonoAlternativo(request.getTelefonoAlternativo());
        }
        if (request.getCorreoElectronico() != null) {
            if (clienteRepository.existsByCorreoElectronicoAndIdNot(request.getCorreoElectronico(), id)) {
                throw new ValidacionNegocioException("El correo ya se encuentra registrado por otro cliente");
            }
            cliente.setCorreoElectronico(request.getCorreoElectronico());
            if (cliente.getUsuario() != null) {
                cliente.getUsuario().setCorreo(request.getCorreoElectronico());
                cliente.getUsuario().setFechaActualizacion(LocalDateTime.now());
            }
        }

        if (cliente.getDomicilio() != null) {
            if (request.getCalle() != null) cliente.getDomicilio().setCalle(request.getCalle());
            if (request.getNumeroExterior() != null) cliente.getDomicilio().setNumeroExterior(request.getNumeroExterior());
            if (request.getNumeroInterior() != null) cliente.getDomicilio().setNumeroInterior(request.getNumeroInterior());
            if (request.getColonia() != null) cliente.getDomicilio().setColonia(request.getColonia());
            if (request.getMunicipio() != null) cliente.getDomicilio().setMunicipio(request.getMunicipio());
            if (request.getEstado() != null) cliente.getDomicilio().setEstado(request.getEstado());
            if (request.getCodigoPostal() != null) cliente.getDomicilio().setCodigoPostal(request.getCodigoPostal());
            if (request.getPais() != null) cliente.getDomicilio().setPais(request.getPais());
        }

        if (cliente.getInformacionLaboral() != null) {
            if (request.getOcupacion() != null) cliente.getInformacionLaboral().setOcupacion(request.getOcupacion());
            if (request.getEmpresa() != null) cliente.getInformacionLaboral().setEmpresa(request.getEmpresa());
            if (request.getIngresoMensual() != null) {
                if (request.getIngresoMensual().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ValidacionNegocioException("El ingreso mensual debe ser mayor a cero");
                }
                cliente.getInformacionLaboral().setIngresoMensual(request.getIngresoMensual());
            }
        }

        if (request.getActivoLogin() != null) {
            cliente.setActivoLogin(request.getActivoLogin());
            if (cliente.getUsuario() != null) {
                cliente.getUsuario().setActivo(request.getActivoLogin());
                cliente.getUsuario().setFechaActualizacion(LocalDateTime.now());
            }
            if (!request.getActivoLogin()) {
                cliente.getCuentas().forEach(c -> c.setEstado(EstadoCuenta.BLOQUEADA));
            }
        }

        cliente = clienteRepository.save(cliente);
        log.info("Cliente con ID: {} actualizado con PATCH", id);
        return mapearAResponseDto(cliente);
    }

    @Override
    @Transactional
    public void bajaLogica(Long id) {
        Cliente cliente = clienteRepository.findByIdAndEliminadoLogicoFalse(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID: " + id));

        cliente.setEliminadoLogico(true);
        cliente.setActivoLogin(false);

        if (cliente.getUsuario() != null) {
            cliente.getUsuario().setActivo(false);
            cliente.getUsuario().setFechaActualizacion(LocalDateTime.now());
        }

        for (CuentaBancaria cuenta : cliente.getCuentas()) {
            cuenta.setEstado(EstadoCuenta.CANCELADA);
            cuenta.setFechaActualizacion(LocalDateTime.now());
        }

        clienteRepository.save(cliente);
        log.info("Baja lógica aplicada al cliente ID: {}, usuario desactivado y cuentas canceladas", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDto> consultarTodos() {
        return clienteRepository.findAllByEliminadoLogicoFalse()
                .stream()
                .map(this::mapearAResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDto consultarPorId(Long id) {
        Cliente cliente = clienteRepository.findByIdAndEliminadoLogicoFalse(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID: " + id));
        return mapearAResponseDto(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDto consultarPorCurp(String curp) {
        Cliente cliente = clienteRepository.findByCurpAndEliminadoLogicoFalse(curp)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con CURP: " + curp));
        return mapearAResponseDto(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDto consultarPorRfc(String rfc) {
        Cliente cliente = clienteRepository.findByRfcAndEliminadoLogicoFalse(rfc)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con RFC: " + rfc));
        return mapearAResponseDto(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDto consultarPorCorreo(String correo) {
        Cliente cliente = clienteRepository.findByCorreoElectronicoAndEliminadoLogicoFalse(correo)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con correo: " + correo));
        return mapearAResponseDto(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDto consultarPorNumeroCuenta(String numeroCuenta) {
        Cliente cliente = clienteRepository.findByNumeroCuentaAndEliminadoLogicoFalse(numeroCuenta)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con número de cuenta: " + numeroCuenta));
        return mapearAResponseDto(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDto> buscarPorNombre(String nombre) {
        return clienteRepository.findByNombreContainingIgnoreCaseAndEliminadoLogicoFalse(nombre)
                .stream()
                .map(this::mapearAResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDto> buscarPorApellidoPaterno(String apellidoPaterno) {
        return clienteRepository.findByApellidoPaternoContainingIgnoreCaseAndEliminadoLogicoFalse(apellidoPaterno)
                .stream()
                .map(this::mapearAResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDto> buscarPorApellidoMaterno(String apellidoMaterno) {
        return clienteRepository.findByApellidoMaternoContainingIgnoreCaseAndEliminadoLogicoFalse(apellidoMaterno)
                .stream()
                .map(this::mapearAResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDto> consultarActivos() {
        return clienteRepository.findByActivoLoginTrueAndEliminadoLogicoFalse()
                .stream()
                .map(this::mapearAResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDto> consultarPorRangoFechas(LocalDate inicio, LocalDate fin) {
        LocalDateTime start = inicio.atStartOfDay();
        LocalDateTime end = fin.atTime(23, 59, 59);
        return clienteRepository.findByFechaCreacionBetweenAndEliminadoLogicoFalse(start, end)
                .stream()
                .map(this::mapearAResponseDto)
                .collect(Collectors.toList());
    }

    private void validarCodigoPostalYEstado(String codigoPostal, EstadoRepublica estado) {
        if (estado != null && codigoPostal != null && !estado.esCodigoPostalValido(codigoPostal)) {
            throw new ValidacionNegocioException(
                    "El código postal " + codigoPostal + " no corresponde al estado de " + estado.getNombreOficial() +
                    ". Los prefijos válidos para este estado son: " + String.join(", ", estado.getPrefijosCp())
            );
        }
    }

    private void validarReglasContrasena(String password) {
        if (password == null || !password.matches(PASSWORD_PATTERN)) {
            throw new com.proyecto.servicios.exception.ContrasenaInvalidaException(
                    "La contraseña debe contener al menos 8 caracteres, una letra mayúscula, una minúscula, un número y un carácter especial"
            );
        }
    }

    private void validarMayorDeEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            throw new ValidacionNegocioException("La fecha de nacimiento es obligatoria");
        }
        if (Period.between(fechaNacimiento, LocalDate.now()).getYears() < 18) {
            throw new ValidacionNegocioException("El cliente debe ser mayor de edad (18 años o más)");
        }
    }

    private void validarUnicidad(ClienteRegistroRequest request) {
        String curp = request.getDatosPersonales().getCurp();
        if (clienteRepository.existsByCurp(curp)) {
            throw new CurpDuplicadaException("La CURP ya se encuentra registrada: " + curp);
        }

        String rfc = request.getDatosPersonales().getRfc();
        if (clienteRepository.existsByRfc(rfc)) {
            throw new RfcDuplicadoException("El RFC ya se encuentra registrado: " + rfc);
        }

        String telefono = request.getDatosContacto().getTelefonoMovil();
        if (clienteRepository.existsByTelefonoMovil(telefono)) {
            throw new ValidacionNegocioException("El teléfono ya se encuentra registrado: " + telefono);
        }

        String correo = request.getDatosContacto().getCorreoElectronico();
        if (clienteRepository.existsByCorreoElectronico(correo)) {
            throw new ValidacionNegocioException("El correo electrónico ya se encuentra registrado: " + correo);
        }
    }

    private String generarNumeroCuentaUnico() {
        String numeroCuenta;
        do {
            numeroCuenta = generadorCuentaBancaria.generarNumeroCuenta();
        } while (cuentaBancariaRepository.existsByNumeroCuenta(numeroCuenta));
        return numeroCuenta;
    }

    private Cliente mapearAEntidad(ClienteRegistroRequest request) {
        DatosPersonalesDto p = request.getDatosPersonales();
        DatosContactoDto c = request.getDatosContacto();
        InformacionLaboralDto l = request.getInformacionLaboral();

        InformacionLaboral laboral = InformacionLaboral.builder()
                .ocupacion(l.getOcupacion())
                .empresa(l.getEmpresa())
                .ingresoMensual(l.getIngresoMensual())
                .build();

        return Cliente.builder()
                .nombre(p.getNombre())
                .segundoNombre(p.getSegundoNombre())
                .apellidoPaterno(p.getApellidoPaterno())
                .apellidoMaterno(p.getApellidoMaterno())
                .fechaNacimiento(p.getFechaNacimiento())
                .curp(p.getCurp())
                .rfc(p.getRfc())
                .sexo(p.getSexo())
                .nacionalidad(p.getNacionalidad())
                .estadoCivil(p.getEstadoCivil())
                .correoElectronico(c.getCorreoElectronico())
                .telefonoMovil(c.getTelefonoMovil())
                .telefonoAlternativo(c.getTelefonoAlternativo())
                .informacionLaboral(laboral)
                .biometriaInfo(new BiometriaInfo())
                .activoLogin(true)
                .eliminadoLogico(false)
                .cuentas(new ArrayList<>())
                .build();
    }

    private ClienteResponseDto mapearAResponseDto(Cliente cliente) {
        DatosPersonalesDto p = DatosPersonalesDto.builder()
                .nombre(cliente.getNombre())
                .segundoNombre(cliente.getSegundoNombre())
                .apellidoPaterno(cliente.getApellidoPaterno())
                .apellidoMaterno(cliente.getApellidoMaterno())
                .fechaNacimiento(cliente.getFechaNacimiento())
                .curp(cliente.getCurp())
                .rfc(cliente.getRfc())
                .sexo(cliente.getSexo())
                .nacionalidad(cliente.getNacionalidad())
                .estadoCivil(cliente.getEstadoCivil())
                .build();

        DatosContactoDto c = DatosContactoDto.builder()
                .correoElectronico(cliente.getCorreoElectronico())
                .telefonoMovil(cliente.getTelefonoMovil())
                .telefonoAlternativo(cliente.getTelefonoAlternativo())
                .build();

        DomicilioDto d = null;
        if (cliente.getDomicilio() != null) {
            Domicilio dom = cliente.getDomicilio();
            d = DomicilioDto.builder()
                    .calle(dom.getCalle())
                    .numeroExterior(dom.getNumeroExterior())
                    .numeroInterior(dom.getNumeroInterior())
                    .colonia(dom.getColonia())
                    .municipio(dom.getMunicipio())
                    .estado(dom.getEstado())
                    .codigoPostal(dom.getCodigoPostal())
                    .pais(dom.getPais())
                    .build();
        }

        InformacionLaboralDto l = null;
        if (cliente.getInformacionLaboral() != null) {
            InformacionLaboral lab = cliente.getInformacionLaboral();
            l = InformacionLaboralDto.builder()
                    .ocupacion(lab.getOcupacion())
                    .empresa(lab.getEmpresa())
                    .ingresoMensual(lab.getIngresoMensual())
                    .build();
        }

        List<CuentaBancariaResponseDto> cuentasDto = cliente.getCuentas().stream()
                .map(cta -> CuentaBancariaResponseDto.builder()
                        .numeroCuenta(cta.getNumeroCuenta())
                        .saldo(cta.getSaldo())
                        .estado(cta.getEstado())
                        .fechaCreacion(cta.getFechaCreacion())
                        .build())
                .collect(Collectors.toList());

        CuentaBancariaResponseDto principal = cuentasDto.isEmpty() ? null : cuentasDto.get(0);

        UsuarioResponseDto usuarioDto = null;
        if (cliente.getUsuario() != null) {
            usuarioDto = UsuarioResponseDto.builder()
                    .correo(cliente.getUsuario().getCorreo())
                    .activo(cliente.getUsuario().getActivo())
                    .fechaCreacion(cliente.getUsuario().getFechaCreacion())
                    .build();
        }

        return ClienteResponseDto.builder()
                .id(cliente.getId())
                .datosPersonales(p)
                .datosContacto(c)
                .domicilio(d)
                .informacionLaboral(l)
                .cuentaBancaria(principal)
                .cuentas(cuentasDto)
                .usuario(usuarioDto)
                .activoLogin(cliente.getActivoLogin())
                .biometriaEnrolada(cliente.getBiometriaInfo() != null && Boolean.TRUE.equals(cliente.getBiometriaInfo().getBiometriaEnrolada()))
                .fechaCreacion(cliente.getFechaCreacion())
                .build();
    }
}

