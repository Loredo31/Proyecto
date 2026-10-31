package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.GestoPagoTxProperties;
import com.proyecto.servicios.dto.pago.PagoServicioRequest;
import com.proyecto.servicios.entity.cliente.CuentaBancaria;
import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.enums.EstadoCuenta;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.exception.CuentaInactivaException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.PinInvalidoException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.exception.TransaccionDuplicadaException;
import com.proyecto.servicios.model.gestopago.DatosEnvioTx;
import com.proyecto.servicios.model.gestopago.ResultadoSendTx;
import com.proyecto.servicios.model.gestopago.SendTxRespuesta;
import com.proyecto.servicios.repository.CuentaBancariaRepository;
import com.proyecto.servicios.repository.TransaccionPagoRepository;
import com.proyecto.servicios.service.TransaccionPagoService;
import com.proyecto.servicios.util.DatosSensiblesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransaccionPagoServiceImpl implements TransaccionPagoService {

    private static final Set<EstadoTransaccion> ESTADOS_DUPLICABLES = EnumSet.of(
            EstadoTransaccion.PROCESANDO, EstadoTransaccion.EXITOSA, EstadoTransaccion.PENDIENTE_CONCILIACION);

    private static final int MAX_MENSAJE = 500;

    private final CuentaBancariaRepository cuentaRepository;
    private final TransaccionPagoRepository transaccionRepository;
    private final PasswordEncoder passwordEncoder;
    private final GestoPagoTxProperties properties;

    @Override
    @Transactional(noRollbackFor = PinInvalidoException.class)
    public ReservaSaldo reservarSaldo(Long clienteId, String idempotencyKey, PagoServicioRequest request,
                                      DatosEnvioTx envio) {
        CuentaBancaria cuenta = cuentaRepository.findByNumeroCuentaForUpdate(String.valueOf(request.getNumCuenta()))
                .orElseThrow(() -> new CuentaNoEncontradaException("Cuenta no encontrada"));

        validarTitularidad(cuenta, clienteId);
        validarCuentaActiva(cuenta);
        validarPin(cuenta, request.getPin());

        if (idempotencyKey != null) {
            var previa = transaccionRepository.findByIdempotencyKey(idempotencyKey);
            if (previa.isPresent()) {
                return new ReservaSaldo(validarMismaOperacion(previa.get(), cuenta, request), true);
            }
        }
        validarNoDuplicado(cuenta, request);

        BigDecimal monto = request.getMontoPago();
        if (cuenta.getSaldoDisponible().compareTo(monto) < 0) {
            throw new SaldoInsuficienteException();
        }

        cuenta.setSaldoRetenido(cuenta.getSaldoRetenido().add(monto));
        cuenta.setFechaActualizacion(LocalDateTime.now());

        TransaccionPago transaccion = transaccionRepository.save(TransaccionPago.builder()
                .folio(UUID.randomUUID().toString())
                .idempotencyKey(idempotencyKey)
                .cuentaId(cuenta.getId())
                .numeroCuenta(cuenta.getNumeroCuenta())
                .clienteId(clienteId)
                .idServicio(request.getIdServicio())
                .idProducto(request.getIdProducto())
                .referencia(request.getReferencia())
                .conReferencia(request.esConReferencia())
                .monto(monto)
                .upc(envio.upc())
                .horaLocal(envio.horaLocal())
                .telefonoEnvio(envio.telefono())
                .unidad(envio.unidad())
                .estado(EstadoTransaccion.PROCESANDO)
                .build());

        log.info("Saldo retenido para pago folio={} upc={} cuenta={} monto={}", transaccion.getFolio(),
                transaccion.getUpc(), DatosSensiblesUtil.enmascarar(cuenta.getNumeroCuenta()), monto);
        return new ReservaSaldo(transaccion, false);
    }

    @Override
    @Transactional
    public TransaccionPago aplicarResultado(Long transaccionId, ResultadoSendTx resultado, OrigenRespuesta origen) {
        TransaccionPago transaccion = obtenerNoFinal(transaccionId);
        registrarEvidencia(transaccion, resultado.respuesta(), origen);

        return switch (resultado.tipo()) {
            case APROBADA -> confirmar(transaccion, resultado);
            case RECHAZADA -> liberar(transaccion, EstadoTransaccion.RECHAZADA, resultado.codigo(), resultado.mensaje());
            case INDETERMINADA -> marcarPendiente(transaccion, resultado, origen);
        };
    }

    @Override
    @Transactional
    public TransaccionPago liberarNoEnviada(Long transaccionId, String motivo) {
        return liberar(obtenerNoFinal(transaccionId), EstadoTransaccion.ERROR, "NO_ENVIADO", motivo);
    }

    @Override
    @Transactional(readOnly = true)
    public TransaccionPago consultar(Long clienteId, String folio) {
        return transaccionRepository.findByFolio(folio)
                .filter(t -> t.getClienteId().equals(clienteId))
                .orElseThrow(() -> new BusinessException(ApiResponseEnum.TRANSACCION_NO_ENCONTRADA));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransaccionPago> listarPendientesConfirmables() {
        LocalDateTime ahora = LocalDateTime.now();
        return transaccionRepository.findByEstadoAndFechaCreacionBetween(
                EstadoTransaccion.PENDIENTE_CONCILIACION,
                ahora.minus(properties.getVigenciaConfirmacion()),
                ahora.minus(properties.getEsperaMinimaConfirmacion()));
    }

    @Override
    @Transactional(readOnly = true)
    public long contarPendientesFueraDeVentana() {
        return transaccionRepository.countByEstadoAndFechaCreacionBefore(EstadoTransaccion.PENDIENTE_CONCILIACION,
                LocalDateTime.now().minus(properties.getVigenciaConfirmacion()));
    }

    @Override
    @Transactional
    public int moverProcesandoVencidasAConciliacion() {
        LocalDateTime limite = LocalDateTime.now().minus(properties.getUmbralConciliacion());
        List<TransaccionPago> atascadas = transaccionRepository
                .findByEstadoAndFechaActualizacionBefore(EstadoTransaccion.PROCESANDO, limite);
        atascadas.forEach(t -> {
            t.setEstado(EstadoTransaccion.PENDIENTE_CONCILIACION);
            t.setMensajeRespuesta("Transacción sin resultado final tras el umbral; requiere conciliación");
        });
        return atascadas.size();
    }

    private TransaccionPago confirmar(TransaccionPago transaccion, ResultadoSendTx resultado) {
        CuentaBancaria cuenta = bloquearCuenta(transaccion.getCuentaId());
        BigDecimal monto = transaccion.getMonto();
        cuenta.setSaldo(cuenta.getSaldo().subtract(monto));
        cuenta.setSaldoRetenido(cuenta.getSaldoRetenido().subtract(monto));
        cuenta.setFechaActualizacion(LocalDateTime.now());

        transaccion.setEstado(EstadoTransaccion.EXITOSA);
        transaccion.setCodigoRespuesta(resultado.codigo());
        transaccion.setMensajeRespuesta(truncar(resultado.mensaje()));

        SendTxRespuesta respuesta = resultado.respuesta();
        if (respuesta != null) {
            transaccion.setNumeroAutorizacion(respuesta.numeroAutorizacion());
            transaccion.setIdTxProveedor(respuesta.idTx());
            if (respuesta.codigoCanje() != null) {
                transaccion.setCodigoCanje(respuesta.codigoCanje());
                transaccion.setLeyenda(respuesta.leyenda());
            }
        }

        log.info("Pago folio={} EXITOSO (autorización={}); saldo descontado",
                transaccion.getFolio(), transaccion.getNumeroAutorizacion());
        return transaccion;
    }

    private TransaccionPago liberar(TransaccionPago transaccion, EstadoTransaccion estadoFinal, String codigo, String mensaje) {
        CuentaBancaria cuenta = bloquearCuenta(transaccion.getCuentaId());
        cuenta.setSaldoRetenido(cuenta.getSaldoRetenido().subtract(transaccion.getMonto()));
        cuenta.setFechaActualizacion(LocalDateTime.now());

        transaccion.setEstado(estadoFinal);
        transaccion.setCodigoRespuesta(codigo);
        transaccion.setMensajeRespuesta(truncar(mensaje));

        log.info("Pago folio={} {}; retención liberada (código={})", transaccion.getFolio(), estadoFinal, codigo);
        return transaccion;
    }

    private TransaccionPago marcarPendiente(TransaccionPago transaccion, ResultadoSendTx resultado, OrigenRespuesta origen) {
        transaccion.setEstado(EstadoTransaccion.PENDIENTE_CONCILIACION);
        if (origen == OrigenRespuesta.SEND_TX) {
            transaccion.setCodigoRespuesta(resultado.codigo());
            transaccion.setMensajeRespuesta(truncar(resultado.mensaje()));
        }
        log.warn("Pago folio={} en PENDIENTE_CONCILIACION (código={}); el saldo permanece retenido",
                transaccion.getFolio(), resultado.codigo());
        return transaccion;
    }

    private static void registrarEvidencia(TransaccionPago transaccion, SendTxRespuesta respuesta, OrigenRespuesta origen) {
        if (respuesta == null || respuesta.cuerpo() == null) {
            return;
        }
        if (origen == OrigenRespuesta.SEND_TX) {
            transaccion.setRespuestaProveedor(respuesta.cuerpo());
        } else {
            transaccion.setRespuestaConfirmacion(respuesta.cuerpo());
        }
    }

    private void validarTitularidad(CuentaBancaria cuenta, Long clienteId) {
        if (cuenta.getCliente() == null || !cuenta.getCliente().getId().equals(clienteId)) {
            log.warn("Cliente {} intentó pagar con una cuenta ajena ({})",
                    clienteId, DatosSensiblesUtil.enmascarar(cuenta.getNumeroCuenta()));
            throw new BusinessException(ApiResponseEnum.CUENTA_NO_PERTENECE);
        }
    }

    private static void validarCuentaActiva(CuentaBancaria cuenta) {
        if (cuenta.getEstado() != EstadoCuenta.ACTIVA) {
            throw new CuentaInactivaException("La cuenta se encuentra " + cuenta.getEstado().name().toLowerCase());
        }
        if (cuenta.getCliente() != null && Boolean.TRUE.equals(cuenta.getCliente().getEliminadoLogico())) {
            throw new CuentaInactivaException("El titular de la cuenta no está activo");
        }
    }

    private void validarPin(CuentaBancaria cuenta, String pin) {
        if (cuenta.getPinSeguridad() == null) {
            throw new BusinessException(ApiResponseEnum.PIN_NO_CONFIGURADO);
        }
        if (!passwordEncoder.matches(pin, cuenta.getPinSeguridad())) {
            int intentos = cuenta.getIntentosPinFallidos() + 1;
            cuenta.setIntentosPinFallidos(intentos);
            if (intentos >= properties.getMaxIntentosPin()) {
                cuenta.setEstado(EstadoCuenta.BLOQUEADA);
                log.warn("Cuenta {} bloqueada por {} intentos de PIN fallidos",
                        DatosSensiblesUtil.enmascarar(cuenta.getNumeroCuenta()), intentos);
            }
            throw new PinInvalidoException();
        }
        if (cuenta.getIntentosPinFallidos() > 0) {
            cuenta.setIntentosPinFallidos(0);
        }
    }

    private static TransaccionPago validarMismaOperacion(TransaccionPago previa, CuentaBancaria cuenta,
                                                         PagoServicioRequest request) {
        boolean misma = previa.getCuentaId().equals(cuenta.getId())
                && previa.getReferencia().equals(request.getReferencia())
                && previa.getIdProducto().equals(request.getIdProducto())
                && previa.getMonto().compareTo(request.getMontoPago()) == 0;
        if (!misma) {
            throw new TransaccionDuplicadaException("La Idempotency-Key ya fue usada para una operación distinta");
        }
        log.info("Petición repetida con Idempotency-Key; se devuelve el folio {}", previa.getFolio());
        return previa;
    }

    private void validarNoDuplicado(CuentaBancaria cuenta, PagoServicioRequest request) {
        LocalDateTime desde = LocalDateTime.now().minus(properties.getVentanaDuplicados());
        boolean duplicado = transaccionRepository
                .existsByCuentaIdAndReferenciaAndIdProductoAndMontoAndEstadoInAndFechaCreacionAfter(
                        cuenta.getId(), request.getReferencia(), request.getIdProducto(),
                        request.getMontoPago(), ESTADOS_DUPLICABLES, desde);
        if (duplicado) {
            throw new TransaccionDuplicadaException("Ya existe un pago idéntico en los últimos "
                    + properties.getVentanaDuplicados().toMinutes() + " minutos");
        }
    }

    private TransaccionPago obtenerNoFinal(Long transaccionId) {
        TransaccionPago transaccion = transaccionRepository.findById(transaccionId)
                .orElseThrow(() -> new BusinessException(ApiResponseEnum.TRANSACCION_NO_ENCONTRADA));
        if (transaccion.getEstado().esFinal()) {
            throw new IllegalStateException("La transacción " + transaccion.getFolio()
                    + " ya está en estado final " + transaccion.getEstado());
        }
        return transaccion;
    }

    private CuentaBancaria bloquearCuenta(Long cuentaId) {
        return cuentaRepository.findByIdForUpdate(cuentaId)
                .orElseThrow(() -> new CuentaNoEncontradaException("Cuenta no encontrada"));
    }

    private static String truncar(String mensaje) {
        return mensaje != null && mensaje.length() > MAX_MENSAJE ? mensaje.substring(0, MAX_MENSAJE) : mensaje;
    }
}
