package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.dto.ProductDto;
import com.proyecto.servicios.dto.pago.PagoServicioRequest;
import com.proyecto.servicios.dto.pago.PagoServicioResponseDto;
import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.exception.EnvioNoRealizadoException;
import com.proyecto.servicios.exception.ProveedorNoDisponibleException;
import com.proyecto.servicios.mapper.SendTxPayloadMapper;
import com.proyecto.servicios.mapper.TransaccionPagoMapper;
import com.proyecto.servicios.model.gestopago.ResultadoSendTx;
import com.proyecto.servicios.service.GestoPagoTxGateway;
import com.proyecto.servicios.service.PagoServicioService;
import com.proyecto.servicios.service.TransaccionPagoService;
import com.proyecto.servicios.service.TransaccionPagoService.OrigenRespuesta;
import com.proyecto.servicios.service.ValidadorProductoPago;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orquesta el pago: validar → retener saldo → firmar y enviar a GestoPago → confirmar/liberar/conciliar.
 * Deliberadamente NO es @Transactional: la llamada HTTP ocurre fuera de cualquier transacción de BD,
 * así el bloqueo de la cuenta no se mantiene mientras se espera al proveedor.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagoServicioServiceImpl implements PagoServicioService {

    private static final int MAX_IDEMPOTENCY_KEY = 64;

    private final ValidadorProductoPago validadorProducto;
    private final TransaccionPagoService transaccionPagoService;
    private final GestoPagoTxGateway gestoPagoTxGateway;
    private final SendTxPayloadMapper payloadMapper;
    private final TransaccionPagoMapper transaccionMapper;

    @Override
    public PagoServicioResponseDto procesarPago(Long clienteId, String idempotencyKey, PagoServicioRequest request) {
        validarIdempotencyKey(idempotencyKey);
        ProductDto producto = validadorProducto.validar(request);

        TransaccionPagoService.ReservaSaldo reserva = transaccionPagoService.reservarSaldo(
                clienteId, idempotencyKey, request, payloadMapper.prepararEnvio(request, producto));
        if (reserva.repetida()) {
            return transaccionMapper.toResponse(reserva.transaccion());
        }

        TransaccionPago transaccion = reserva.transaccion();
        ResultadoSendTx resultado;
        try {
            resultado = gestoPagoTxGateway.enviar(payloadMapper.toPayload(transaccion));
        } catch (EnvioNoRealizadoException ex) {
            log.error("Pago folio={} no enviado a GestoPago: {}", transaccion.getFolio(), ex.getMessage());
            transaccionPagoService.liberarNoEnviada(transaccion.getId(), ex.getMessage());
            throw new ProveedorNoDisponibleException();
        }

        return transaccionMapper.toResponse(
                transaccionPagoService.aplicarResultado(transaccion.getId(), resultado, OrigenRespuesta.SEND_TX));
    }

    @Override
    public PagoServicioResponseDto consultarPago(Long clienteId, String folio) {
        return transaccionMapper.toResponse(transaccionPagoService.consultar(clienteId, folio));
    }

    @Override
    public int conciliarPendientes() {
        int resueltas = 0;
        for (TransaccionPago pendiente : transaccionPagoService.listarPendientesConfirmables()) {
            try {
                ResultadoSendTx resultado = gestoPagoTxGateway.confirmar(payloadMapper.toPayload(pendiente));
                TransaccionPago actualizada = transaccionPagoService.aplicarResultado(
                        pendiente.getId(), resultado, OrigenRespuesta.CONFIRM_TX);
                if (actualizada.getEstado().esFinal()) {
                    resueltas++;
                    log.info("Conciliación folio={} resuelta como {}", actualizada.getFolio(), actualizada.getEstado());
                }
            } catch (RuntimeException ex) {
                // Incluye EnvioNoRealizadoException: si confirmTx no se pudo consultar, el pago sigue
                // incierto y la retención NO se libera; se reintenta en la siguiente ejecución.
                log.error("No fue posible conciliar el folio={}: {}", pendiente.getFolio(), ex.getMessage());
            }
        }
        return resueltas;
    }

    private static void validarIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey != null && (idempotencyKey.isBlank() || idempotencyKey.length() > MAX_IDEMPOTENCY_KEY)) {
            throw new BusinessException(ApiResponseEnum.DATOS_INVALIDOS,
                    "El header Idempotency-Key debe tener entre 1 y " + MAX_IDEMPOTENCY_KEY + " caracteres");
        }
    }
}
