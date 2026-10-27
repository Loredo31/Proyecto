package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.ResultadoSendTx;
import com.proyecto.servicios.model.gestopago.SendTxPayload;

public interface GestoPagoTxGateway {

    /**
     * Firma y envía el pago a sendTx.do.
     *
     * @return resultado clasificado; cualquier falla posterior al envío se reporta como INDETERMINADA
     * @throws com.proyecto.servicios.exception.EnvioNoRealizadoException si hay certeza de que el
     *         proveedor no recibió el pago (seguro liberar la retención)
     */
    ResultadoSendTx enviar(SendTxPayload payload);

    /**
     * Consulta en confirmTx.do si un sendTx sin respuesta definitiva fue aplicado.
     * Debe recibir exactamente el mismo payload que se envió a sendTx.
     *
     * @return APROBADA (aplicada), RECHAZADA (no aplicada) o INDETERMINADA (aún sin certeza)
     */
    ResultadoSendTx confirmar(SendTxPayload payload);
}
