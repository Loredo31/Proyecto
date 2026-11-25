package com.proyecto.servicios.mapper;

import com.proyecto.servicios.dto.pago.PagoServicioResponseDto;
import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.util.DatosSensiblesUtil;
import com.proyecto.servicios.util.ImagenesGestoPago;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Mapper(componentModel = "spring", imports = {DatosSensiblesUtil.class, ImagenesGestoPago.class})
public interface TransaccionPagoMapper {

    /** Formato del campo horaLocal (Date) que se envía a GestoPago en sendTx. */
    DateTimeFormatter FORMATO_HORA_LOCAL = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Mapping(target = "numCuenta", expression = "java(DatosSensiblesUtil.enmascarar(transaccion.getNumeroCuenta()))")
    @Mapping(target = "montoPago", source = "monto")
    @Mapping(target = "logoUrl", expression = "java(ImagenesGestoPago.logoServicio(transaccion.getIdServicio()))")
    @Mapping(target = "fechaEnvio", source = "horaLocal", qualifiedByName = "horaLocalAFecha")
    PagoServicioResponseDto toResponse(TransaccionPago transaccion);

    /** "20261124222410" -> 2026-11-24T22:24:10. Si no tiene el formato esperado, no se devuelve. */
    @Named("horaLocalAFecha")
    default LocalDateTime horaLocalAFecha(String horaLocal) {
        if (horaLocal == null || horaLocal.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(horaLocal.trim(), FORMATO_HORA_LOCAL);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
