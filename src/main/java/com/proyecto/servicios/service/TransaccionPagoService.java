package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.pago.PagoServicioRequest;
import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.model.gestopago.DatosEnvioTx;
import com.proyecto.servicios.model.gestopago.ResultadoSendTx;

import java.util.List;

public interface TransaccionPagoService {

    enum OrigenRespuesta { SEND_TX, CONFIRM_TX }

    ReservaSaldo reservarSaldo(Long clienteId, String idempotencyKey, PagoServicioRequest request, DatosEnvioTx envio);

    TransaccionPago aplicarResultado(Long transaccionId, ResultadoSendTx resultado, OrigenRespuesta origen);

    TransaccionPago liberarNoEnviada(Long transaccionId, String motivo);

    TransaccionPago consultar(Long clienteId, String folio);

    List<TransaccionPago> listarPendientesConfirmables();

    long contarPendientesFueraDeVentana();

    int moverProcesandoVencidasAConciliacion();

    record ReservaSaldo(TransaccionPago transaccion, boolean repetida) {
    }
}
