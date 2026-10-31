package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.config.GestoPagoTxProperties;
import com.proyecto.servicios.exception.EnvioNoRealizadoException;
import com.proyecto.servicios.model.gestopago.ResultadoSendTx;
import com.proyecto.servicios.model.gestopago.SendTxPayload;
import com.proyecto.servicios.model.gestopago.SendTxRespuesta;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.GestoPagoTxGateway;
import com.proyecto.servicios.service.SendTxResponseParser;
import com.proyecto.servicios.util.GestoPagoAesCipher;
import feign.Response;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class GestoPagoTxGatewayImpl implements GestoPagoTxGateway {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String PARAM_RANDOM = "random";
    private static final String PARAM_SIGNED = "signed";

    private final GestoPagoTxClient gestoPagoTxClient;
    private final GestoPagoTokenService tokenService;
    private final GestoPagoAesCipher aesCipher;
    private final SendTxResponseParser responseParser;
    private final GestoPagoTxProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public ResultadoSendTx enviar(SendTxPayload payload) {
        return invocar("sendTx", payload, this::clasificarVenta, venta());
    }

    @Override
    public ResultadoSendTx confirmar(SendTxPayload payload) {
        return invocar("confirmTx", payload, this::clasificarConfirmacion, confirmacion());
    }

    private ResultadoSendTx invocar(String operacion, SendTxPayload payload,
                                    Function<SendTxRespuesta, ResultadoSendTx> clasificador, Llamada llamada) {
        String firmado = firmar(payload);

        Optional<ResultadoSendTx> resultado = ejecutar(operacion, firmado, obtenerToken(), clasificador, llamada);
        if (resultado.isEmpty()) {
            tokenService.invalidarToken();
            resultado = ejecutar(operacion, firmado, obtenerToken(), clasificador, llamada);
        }
        return resultado.orElseThrow(() ->
                new EnvioNoRealizadoException("GestoPago rechazó las credenciales tras renovar el token"));
    }

    private String firmar(SendTxPayload payload) {
        try {
            return aesCipher.cifrar(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException | RuntimeException ex) {
            throw new EnvioNoRealizadoException("No fue posible firmar el payload para GestoPago", ex);
        }
    }

    private String obtenerToken() {
        try {
            return tokenService.obtenerTokenVigente();
        } catch (RuntimeException ex) {
            throw new EnvioNoRealizadoException("No fue posible obtener el token de GestoPago", ex);
        }
    }

    private Optional<ResultadoSendTx> ejecutar(String operacion, String firmado, String token,
                                               Function<SendTxRespuesta, ResultadoSendTx> clasificador,
                                               Llamada llamada) {
        Map<String, String> formulario = Map.of(
                PARAM_RANDOM, String.valueOf(System.currentTimeMillis()),
                PARAM_SIGNED, firmado);
        long inicio = System.currentTimeMillis();

        try (Response response = llamada.ejecutar(BEARER_PREFIX + token, formulario)) {
            int status = response.status();
            log.info("{}.do respondió HTTP {} en {} ms", operacion, status, System.currentTimeMillis() - inicio);

            if (status == 401 || status == 403) {
                return Optional.empty();
            }
            if (status >= 500 || status == 408 || status == 409) {
                return Optional.of(ResultadoSendTx.indeterminada("HTTP_" + status,
                        "Respuesta HTTP " + status + " del proveedor; requiere conciliación"));
            }
            if (status >= 400) {
                return Optional.of(llamada.esVenta()
                        ? ResultadoSendTx.rechazada("HTTP_" + status, "El proveedor rechazó la solicitud (HTTP " + status + ")")
                        : ResultadoSendTx.indeterminada("HTTP_" + status, "confirmTx respondió HTTP " + status));
            }
            SendTxRespuesta respuesta = responseParser.parse(leerCuerpo(response));
            log.info("{}.do respondió CODIGO={}", operacion, respuesta.codigo());
            return Optional.of(clasificador.apply(respuesta));

        } catch (RetryableException ex) {
            if (conexionNoEstablecida(ex)) {
                throw new EnvioNoRealizadoException("No fue posible conectar con GestoPago", ex);
            }
            log.error("Timeout/IO en {}.do tras {} ms; la transacción quedará en conciliación",
                    operacion, System.currentTimeMillis() - inicio);
            return Optional.of(ResultadoSendTx.indeterminada("TIMEOUT", "Sin respuesta del proveedor; requiere conciliación"));
        } catch (RuntimeException ex) {
            log.error("Respuesta no procesable de {}.do: {}", operacion, ex.getClass().getSimpleName());
            return Optional.of(ResultadoSendTx.indeterminada("RESPUESTA_INVALIDA",
                    "Respuesta del proveedor no interpretable; requiere conciliación"));
        }
    }

    private ResultadoSendTx clasificarVenta(SendTxRespuesta respuesta) {
        String codigo = respuesta.codigo();
        if (codigo == null) {
            return ResultadoSendTx.indeterminada(respuesta);
        }
        if (properties.getCodigoExito().equals(codigo)) {
            return ResultadoSendTx.aprobada(respuesta);
        }
        if (properties.getCodigosIndeterminados().contains(codigo)) {
            return ResultadoSendTx.indeterminada(respuesta);
        }
        return ResultadoSendTx.rechazada(respuesta);
    }

    private ResultadoSendTx clasificarConfirmacion(SendTxRespuesta respuesta) {
        String codigo = respuesta.codigo();
        if (codigo != null && properties.getCodigosConfirmacionAplicada().contains(codigo)) {
            return ResultadoSendTx.aprobada(respuesta);
        }
        if (codigo != null && properties.getCodigosConfirmacionNoAplicada().contains(codigo)) {
            return ResultadoSendTx.rechazada(respuesta);
        }
        return ResultadoSendTx.indeterminada(respuesta);
    }

    private String apiKey() {
        String apiKey = properties.getApiKey();
        return apiKey == null || apiKey.isBlank() ? null : apiKey;
    }

    private static String leerCuerpo(Response response) {
        if (response.body() == null) {
            return "";
        }
        try (InputStream is = response.body().asInputStream()) {
            return new String(is.readAllBytes(), response.charset() != null ? response.charset() : StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Error leyendo la respuesta de GestoPago", ex);
        }
    }

    private static boolean conexionNoEstablecida(Throwable ex) {
        for (Throwable causa = ex; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConnectException || causa instanceof UnknownHostException) {
                return true;
            }
        }
        return false;
    }

    private interface Llamada {
        Response ejecutar(String authorization, Map<String, ?> formulario);

        boolean esVenta();
    }

    private Llamada venta() {
        return new Llamada() {
            @Override
            public Response ejecutar(String authorization, Map<String, ?> formulario) {
                return gestoPagoTxClient.sendTx(authorization, apiKey(), formulario);
            }

            @Override
            public boolean esVenta() {
                return true;
            }
        };
    }

    private Llamada confirmacion() {
        return new Llamada() {
            @Override
            public Response ejecutar(String authorization, Map<String, ?> formulario) {
                return gestoPagoTxClient.confirmTx(authorization, apiKey(), formulario);
            }

            @Override
            public boolean esVenta() {
                return false;
            }
        };
    }
}
