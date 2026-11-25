package com.proyecto.servicios.service;

import com.proyecto.servicios.config.GestoPagoTxProperties;
import com.proyecto.servicios.dto.ProductDto;
import com.proyecto.servicios.dto.pago.PagoServicioRequest;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.exception.CatalogException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidadorProductoPago {

    private static final int TIPO_FRONT_RECARGA = 1;
    private static final int TIPO_FRONT_PAGO_REFERENCIA = 2;

    private final ProductCatalogService productCatalogService;
    private final GestoPagoTxProperties properties;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidorConfigurado;

    public ProductDto validar(PagoServicioRequest request) {
        if (!idDistribuidorConfigurado.equals(request.getIdDistribuidor())) {
            throw new BusinessException(ApiResponseEnum.DATOS_INVALIDOS, "El idDistribuidor no corresponde a este comercio");
        }
        if (!properties.isValidarCatalogo()) {
            return null;
        }

        ProductDto producto = buscarProducto(request.getIdProducto());

        if (Boolean.FALSE.equals(producto.getActive())) {
            throw new BusinessException(ApiResponseEnum.PRODUCTO_INVALIDO, "El producto no está disponible");
        }
        if (producto.getIdServicio() != null && !producto.getIdServicio().equals(request.getIdServicio())) {
            throw new BusinessException(ApiResponseEnum.PRODUCTO_INVALIDO, "El idServicio no corresponde al producto");
        }
        validarTipoFront(producto, request);
        return producto;
    }

    private static void validarTipoFront(ProductDto producto, PagoServicioRequest request) {
        Integer tipoFront = producto.getTipoFront();
        if (tipoFront == null) {
            return;
        }
        switch (tipoFront) {
            case TIPO_FRONT_RECARGA -> validarRecarga(producto, request);
            case TIPO_FRONT_PAGO_REFERENCIA -> validarPagoReferencia(request);
            default -> throw new BusinessException(ApiResponseEnum.PRODUCTO_INVALIDO,
                    "El tipo de producto no está soportado por este endpoint");
        }
    }

    private static void validarRecarga(ProductDto producto, PagoServicioRequest request) {
        if (request.esConReferencia()) {
            throw new BusinessException(ApiResponseEnum.PRODUCTO_INVALIDO,
                    "El producto es una recarga / gift card: debe enviarse con bandera=false");
        }
        BigDecimal precio = producto.getPrice();
        if (precio != null && precio.signum() > 0 && precio.compareTo(request.getMontoPago()) != 0) {
            throw new BusinessException(ApiResponseEnum.PRODUCTO_INVALIDO,
                    "El monto no coincide con el precio del producto");
        }
    }

    private static void validarPagoReferencia(PagoServicioRequest request) {
        if (!request.esConReferencia()) {
            throw new BusinessException(ApiResponseEnum.PRODUCTO_INVALIDO,
                    "El producto es un pago de servicio: debe enviarse con bandera=true y referencia");
        }
    }

    /**
     * Nombre del producto en el catálogo, solo para el historial y el recibo (aunque la validación del catálogo
     * esté desactivada). Nunca bloquea el pago: si el catálogo no responde o no lo encuentra, devuelve null.
     */
    public String nombreProducto(Integer idProducto) {
        if (idProducto == null) {
            return null;
        }
        try {
            String id = String.valueOf(idProducto);
            return productCatalogService.getCatalogFromCache().stream()
                    .filter(p -> id.equals(p.getId()))
                    .map(ProductDto::getName)
                    .findFirst()
                    .orElse(null);
        } catch (RuntimeException ex) {
            log.warn("No se pudo obtener el nombre del producto {} para el historial", idProducto);
            return null;
        }
    }

    private ProductDto buscarProducto(Integer idProducto) {
        try {
            String id = String.valueOf(idProducto);
            return productCatalogService.getCatalogFromCache().stream()
                    .filter(p -> id.equals(p.getId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ApiResponseEnum.PRODUCTO_INVALIDO,
                            "El producto no existe en el catálogo"));
        } catch (CatalogException ex) {
            log.error("Catálogo no disponible; no es posible validar el producto {}", idProducto);
            throw new BusinessException(ApiResponseEnum.PROVEEDOR_PAGOS_NO_DISPONIBLE,
                    "El catálogo de productos no está disponible; intente más tarde");
        }
    }
}
