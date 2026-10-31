package com.proyecto.servicios.mapper;

import com.proyecto.servicios.config.GestoPagoTxProperties;
import com.proyecto.servicios.dto.ProductDto;
import com.proyecto.servicios.dto.pago.PagoServicioRequest;
import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.model.gestopago.DatosEnvioTx;
import com.proyecto.servicios.model.gestopago.SendTxPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@Component
public class SendTxPayloadMapper {

    static final String TELEFONO_SERVICIOS = "1111111111";
    static final String TELEFONO_GIFT_CARD = "#1111111111";
    private static final Set<Integer> TIPOS_GIFT_CARD = Set.of(10, 11);

    private static final String PREFIJO_UPC = "GP";
    private static final int LONGITUD_ALEATORIA_UPC = 13;
    private static final char[] ALFABETO_UPC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private final GestoPagoTxProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public SendTxPayloadMapper(GestoPagoTxProperties properties) {
        this(properties, Clock.systemUTC());
    }

    SendTxPayloadMapper(GestoPagoTxProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public DatosEnvioTx prepararEnvio(PagoServicioRequest request, ProductDto producto) {
        return new DatosEnvioTx(generarUpc(), horaLocal(), resolverTelefono(request, producto), properties.getUnidad());
    }

    public SendTxPayload toPayload(TransaccionPago transaccion) {
        boolean conReferencia = Boolean.TRUE.equals(transaccion.getConReferencia());
        return SendTxPayload.builder()
                .idProducto(transaccion.getIdProducto())
                .idServicio(transaccion.getIdServicio())
                .telefono(transaccion.getTelefonoEnvio())
                .horaLocal(transaccion.getHoraLocal())
                .referencia(conReferencia ? transaccion.getReferencia() : null)
                .montoPago(conReferencia ? transaccion.getMonto() : null)
                .upc(transaccion.getUpc())
                .unidad(transaccion.getUnidad())
                .build();
    }

    private static String resolverTelefono(PagoServicioRequest request, ProductDto producto) {
        if (producto != null && producto.getIdCatTipoServicio() != null
                && TIPOS_GIFT_CARD.contains(producto.getIdCatTipoServicio())) {
            return TELEFONO_GIFT_CARD;
        }
        return request.esConReferencia() ? TELEFONO_SERVICIOS : request.getReferencia();
    }

    private String generarUpc() {
        StringBuilder upc = new StringBuilder(PREFIJO_UPC);
        for (int i = 0; i < LONGITUD_ALEATORIA_UPC; i++) {
            upc.append(ALFABETO_UPC[random.nextInt(ALFABETO_UPC.length)]);
        }
        return upc.toString();
    }

    private String horaLocal() {
        return ZonedDateTime.now(clock.withZone(ZoneId.of(properties.getZonaHoraria())))
                .format(DateTimeFormatter.ofPattern(properties.getFormatoHoraLocal()));
    }
}
