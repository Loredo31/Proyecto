package com.proyecto.servicios.mapper;

import com.proyecto.servicios.dto.pago.PagoServicioResponseDto;
import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.util.DatosSensiblesUtil;
import com.proyecto.servicios.util.ImagenesGestoPago;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = {DatosSensiblesUtil.class, ImagenesGestoPago.class})
public interface TransaccionPagoMapper {

    @Mapping(target = "numCuenta", expression = "java(DatosSensiblesUtil.enmascarar(transaccion.getNumeroCuenta()))")
    @Mapping(target = "montoPago", source = "monto")
    @Mapping(target = "logoUrl", expression = "java(ImagenesGestoPago.logoServicio(transaccion.getIdServicio()))")
    PagoServicioResponseDto toResponse(TransaccionPago transaccion);
}
