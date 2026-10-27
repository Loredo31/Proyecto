package com.proyecto.servicios.client;

import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * Operaciones transaccionales de GestoPago. Ambas reciben un formulario con {@code random} y
 * {@code signed} (payload JSON cifrado con AES-256).
 * <p>
 * Devuelven {@link Response} en crudo para clasificar nosotros cada código HTTP (un 5xx o timeout
 * NO significa que el pago haya fallado). Feign no reintenta por defecto (Retryer.NEVER_RETRY),
 * lo cual es obligatorio aquí: reenviar un pago podría duplicar el cargo.
 */
@FeignClient(name = "gestoPagoTx", url = "${gestopago.tx.url}")
public interface GestoPagoTxClient {

    String API_KEY_HEADER = "X-API-Key";

    @PostMapping(value = "${gestopago.tx.path:/sistema/service/sendTx.do}",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    Response sendTx(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                    @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
                    @RequestBody Map<String, ?> formulario);

    /** Consulta si un sendTx sin respuesta fue aplicado. Debe recibir exactamente los mismos parámetros. */
    @PostMapping(value = "${gestopago.tx.confirm-path:/sistema/service/confirmTx.do}",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    Response confirmTx(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                       @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
                       @RequestBody Map<String, ?> formulario);
}
