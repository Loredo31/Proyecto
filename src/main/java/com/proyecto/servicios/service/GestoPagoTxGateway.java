package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.ResultadoSendTx;
import com.proyecto.servicios.model.gestopago.SendTxPayload;

public interface GestoPagoTxGateway {

    ResultadoSendTx enviar(SendTxPayload payload);

    ResultadoSendTx confirmar(SendTxPayload payload);
}
