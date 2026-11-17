package com.proyecto.servicios.service;

import com.proyecto.servicios.config.GestoPagoTxProperties;
import com.proyecto.servicios.dto.ProductDto;
import com.proyecto.servicios.dto.pago.PagoServicioRequest;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.exception.CatalogException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidadorProductoPagoTest {

    @Mock
    private ProductCatalogService productCatalogService;

    private GestoPagoTxProperties properties;
    private ValidadorProductoPago validador;
    private PagoServicioRequest request;

    @BeforeEach
    void setUp() {
        properties = new GestoPagoTxProperties();
        validador = new ValidadorProductoPago(productCatalogService, properties);
        ReflectionTestUtils.setField(validador, "idDistribuidorConfigurado", 83);

        request = PagoServicioRequest.builder()
                .numCuenta(4152319999888877L)
                .idServicio(133)
                .idProducto(582)
                .montoPago(new BigDecimal("100.00"))
                .idDistribuidor(83)
                .referencia("5555555551")
                .bandera(false)
                .pin("1234")
                .build();
    }

    private ProductDto producto(Integer tipoFront, BigDecimal precio) {
        return ProductDto.builder().id("582").active(true).idServicio(133).tipoFront(tipoFront).price(precio).build();
    }

    private ApiResponseEnum codigoError(Runnable r) {
        return assertThrows(BusinessException.class, r::run).getResponseStatus();
    }

    @Test
    void distribuidorDistinto_Rechaza() {
        request.setIdDistribuidor(1);
        assertEquals(ApiResponseEnum.DATOS_INVALIDOS, codigoError(() -> validador.validar(request)));
        verifyNoInteractions(productCatalogService);
    }

    @Test
    void validacionCatalogoDeshabilitada_RegresaNull() {
        properties.setValidarCatalogo(false);
        assertNull(validador.validar(request));
        verifyNoInteractions(productCatalogService);
    }

    @Test
    void recargaValida_RegresaProducto() {
        ProductDto p = producto(1, new BigDecimal("100.00"));
        when(productCatalogService.getCatalogFromCache()).thenReturn(List.of(p));
        assertSame(p, validador.validar(request));
    }

    @Test
    void productoSinTipoFront_RegresaProducto() {
        ProductDto p = producto(null, null);
        when(productCatalogService.getCatalogFromCache()).thenReturn(List.of(p));
        assertSame(p, validador.validar(request));
    }

    @Test
    void recargaConPrecioCeroNoValidaMonto() {
        ProductDto p = producto(1, BigDecimal.ZERO);
        when(productCatalogService.getCatalogFromCache()).thenReturn(List.of(p));
        assertSame(p, validador.validar(request));
    }

    @Test
    void recargaConMontoDistinto_Rechaza() {
        when(productCatalogService.getCatalogFromCache()).thenReturn(List.of(producto(1, new BigDecimal("50.00"))));
        assertEquals(ApiResponseEnum.PRODUCTO_INVALIDO, codigoError(() -> validador.validar(request)));
    }

    @Test
    void recargaConReferencia_Rechaza() {
        request.setBandera(true);
        when(productCatalogService.getCatalogFromCache()).thenReturn(List.of(producto(1, null)));
        assertEquals(ApiResponseEnum.PRODUCTO_INVALIDO, codigoError(() -> validador.validar(request)));
    }

    @Test
    void pagoReferenciaValido_RegresaProducto() {
        request.setBandera(true);
        ProductDto p = producto(2, null);
        when(productCatalogService.getCatalogFromCache()).thenReturn(List.of(p));
        assertSame(p, validador.validar(request));
    }

    @Test
    void pagoReferenciaSinBandera_Rechaza() {
        when(productCatalogService.getCatalogFromCache()).thenReturn(List.of(producto(2, null)));
        assertEquals(ApiResponseEnum.PRODUCTO_INVALIDO, codigoError(() -> validador.validar(request)));
    }

    @Test
    void tipoFrontNoSoportado_Rechaza() {
        when(productCatalogService.getCatalogFromCache()).thenReturn(List.of(producto(9, null)));
        assertEquals(ApiResponseEnum.PRODUCTO_INVALIDO, codigoError(() -> validador.validar(request)));
    }

    @Test
    void productoInactivo_Rechaza() {
        ProductDto p = producto(1, null);
        p.setActive(false);
        when(productCatalogService.getCatalogFromCache()).thenReturn(List.of(p));
        assertEquals(ApiResponseEnum.PRODUCTO_INVALIDO, codigoError(() -> validador.validar(request)));
    }

    @Test
    void servicioNoCorresponde_Rechaza() {
        ProductDto p = producto(1, null);
        p.setIdServicio(999);
        when(productCatalogService.getCatalogFromCache()).thenReturn(List.of(p));
        assertEquals(ApiResponseEnum.PRODUCTO_INVALIDO, codigoError(() -> validador.validar(request)));
    }

    @Test
    void productoInexistente_Rechaza() {
        when(productCatalogService.getCatalogFromCache()).thenReturn(List.of(ProductDto.builder().id("1").build()));
        assertEquals(ApiResponseEnum.PRODUCTO_INVALIDO, codigoError(() -> validador.validar(request)));
    }

    @Test
    void catalogoNoDisponible_Rechaza() {
        when(productCatalogService.getCatalogFromCache())
                .thenThrow(new CatalogException(ApiResponseEnum.CATALOGO_NO_DISPONIBLE));
        assertEquals(ApiResponseEnum.PROVEEDOR_PAGOS_NO_DISPONIBLE, codigoError(() -> validador.validar(request)));
    }
}
