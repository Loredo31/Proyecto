package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.pago.PagoServicioRequest;
import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.model.gestopago.DatosEnvioTx;
import com.proyecto.servicios.model.gestopago.ResultadoSendTx;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.List;

public interface TransaccionPagoService {

    enum OrigenRespuesta { SEND_TX, CONFIRM_TX }

    ReservaSaldo reservarSaldo(Long clienteId, String idempotencyKey, PagoServicioRequest request, DatosEnvioTx envio);

    TransaccionPago aplicarResultado(Long transaccionId, ResultadoSendTx resultado, OrigenRespuesta origen);

    TransaccionPago liberarNoEnviada(Long transaccionId, String motivo);

    TransaccionPago consultar(Long clienteId, String folio);

    /** Operaciones del cliente en los estados indicados, de la más reciente a la más antigua. */
    Page<TransaccionPago> listar(Long clienteId, Collection<EstadoTransaccion> estados, int pagina, int tamano);

    List<TransaccionPago> listarPendientesConfirmables();

    long contarPendientesFueraDeVentana();

    int moverProcesandoVencidasAConciliacion();

    record ReservaSaldo(TransaccionPago transaccion, boolean repetida) {
    }
}
