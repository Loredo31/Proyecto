package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.pago.PagoServicioRequest;
import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.model.gestopago.DatosEnvioTx;
import com.proyecto.servicios.model.gestopago.ResultadoSendTx;

import java.util.List;

/**
 * Operaciones atómicas (una transacción de BD cada una) sobre saldo y transacciones de pago.
 * Ninguna realiza llamadas HTTP: el bloqueo de la cuenta se mantiene solo milisegundos.
 */
public interface TransaccionPagoService {

    enum OrigenRespuesta { SEND_TX, CONFIRM_TX }

    /**
     * Bloquea la cuenta, valida titularidad, estado, PIN, duplicados y saldo, retiene el monto y
     * registra la transacción con los parámetros de envío.
     */
    ReservaSaldo reservarSaldo(Long clienteId, String idempotencyKey, PagoServicioRequest request, DatosEnvioTx envio);

    /**
     * APROBADA: descuenta el saldo y libera la retención. RECHAZADA: libera la retención sin descontar.
     * INDETERMINADA: conserva la retención en PENDIENTE_CONCILIACION.
     */
    TransaccionPago aplicarResultado(Long transaccionId, ResultadoSendTx resultado, OrigenRespuesta origen);

    /** El pago nunca llegó al proveedor: libera la retención y marca ERROR. */
    TransaccionPago liberarNoEnviada(Long transaccionId, String motivo);

    TransaccionPago consultar(Long clienteId, String folio);

    /** Pendientes dentro de la ventana válida de confirmTx (mínimo 62 s y máximo 24 h tras el envío). */
    List<TransaccionPago> listarPendientesConfirmables();

    /** Pendientes cuya ventana de confirmTx expiró: requieren conciliación manual. */
    long contarPendientesFueraDeVentana();

    /** @return número de transacciones atascadas en PROCESANDO que pasaron a conciliación */
    int moverProcesandoVencidasAConciliacion();

    record ReservaSaldo(TransaccionPago transaccion, boolean repetida) {
    }
}
