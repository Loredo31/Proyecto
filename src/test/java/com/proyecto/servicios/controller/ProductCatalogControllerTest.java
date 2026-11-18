package com.proyecto.servicios.controller;

import com.proyecto.servicios.dto.ApiResponse;
import com.proyecto.servicios.dto.PaginaDto;
import com.proyecto.servicios.dto.ProductDto;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.CatalogException;
import com.proyecto.servicios.service.ConsultaCatalogoService;
import com.proyecto.servicios.service.ProductCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas unitarias para ProductCatalogController")
class ProductCatalogControllerTest {

    @Mock
    private ProductCatalogService productCatalogService;

    private ProductCatalogController productCatalogController;

    @BeforeEach
    void setUp() {
        productCatalogController = new ProductCatalogController(productCatalogService,
                new ConsultaCatalogoService(productCatalogService));
    }

    private static ProductDto producto(String id, String nombre, String categoria, Integer idServicio, Boolean activo) {
        return ProductDto.builder().id(id).name(nombre).category(categoria).idServicio(idServicio)
                .price(new BigDecimal("100.00")).active(activo).build();
    }

    @Test
    @DisplayName("GET /api/catalog/products responde 200 con la página de productos e imágenes")
    void testGetProducts_Success() {
        when(productCatalogService.getCatalogFromCache()).thenReturn(List.of(
                producto("1", "Recarga Telcel", "Telefonía", 79, true)));

        ResponseEntity<ApiResponse<PaginaDto<ProductDto>>> response =
                productCatalogController.getProducts(0, 20, null, null, null, true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ApiResponseEnum.EXITO.getCode(), response.getBody().getCode());
        PaginaDto<ProductDto> pagina = response.getBody().getData();
        assertEquals(1, pagina.totalElementos());
        ProductDto p = pagina.contenido().get(0);
        assertEquals("Recarga Telcel", p.getName());
        assertEquals("https://gestopago.portalventas.net/sistema/images/gestopago/servicios/79.png", p.getLogoUrl());
        assertEquals("https://gestopago.portalventas.net/sistema/images/gestopago/referencias/79.jpg", p.getImagenReferenciaUrl());
    }

    @Test
    @DisplayName("Filtra por categoría, búsqueda sin acentos, servicio y activos, y pagina")
    void testGetProducts_FiltrosYPaginacion() {
        when(productCatalogService.getCatalogFromCache()).thenReturn(List.of(
                producto("1", "Recarga Telcel 100", "Telefonía", 79, true),
                producto("2", "Recarga Telcel 200", "Telefonía", 79, true),
                producto("3", "Recarga Telcel 500", "Telefonía", 79, false),
                producto("4", "Pago CFE", "Servicios", 10, true)));

        PaginaDto<ProductDto> telefonia = productCatalogController
                .getProducts(0, 1, "telefonía", null, null, true).getBody().getData();
        assertEquals(2, telefonia.totalElementos());
        assertEquals(2, telefonia.totalPaginas());
        assertEquals(1, telefonia.contenido().size());

        PaginaDto<ProductDto> busqueda = productCatalogController
                .getProducts(0, 20, null, "telefonia", null, false).getBody().getData();
        assertEquals(3, busqueda.totalElementos());

        PaginaDto<ProductDto> porServicio = productCatalogController
                .getProducts(0, 20, null, null, 10, true).getBody().getData();
        assertEquals("Pago CFE", porServicio.contenido().get(0).getName());

        PaginaDto<ProductDto> fueraDeRango = productCatalogController
                .getProducts(5, 20, null, null, null, true).getBody().getData();
        assertEquals(0, fueraDeRango.contenido().size());
    }

    @Test
    @DisplayName("Detalle y categorías del catálogo")
    void testDetalleYCategorias() {
        when(productCatalogService.getCatalogFromCache()).thenReturn(List.of(
                producto("1", "Recarga", "Telefonía", 79, true),
                producto("2", "Luz", "Servicios", 10, true),
                producto("3", "Otra", null, null, true)));

        assertEquals("Luz", productCatalogController.getProducto("2").getBody().getData().getName());
        assertEquals(List.of("Servicios", "Telefonía"), productCatalogController.getCategorias().getBody().getData());
        assertThrows(com.proyecto.servicios.exception.BusinessException.class,
                () -> productCatalogController.getProducto("99"));
    }

    @Test
    @DisplayName("GET /api/catalog/products propaga CatalogException si la caché está vacía")
    void testGetProducts_EmptyCache_ThrowsException() {
        when(productCatalogService.getCatalogFromCache())
                .thenThrow(new CatalogException(ApiResponseEnum.CATALOGO_NO_DISPONIBLE));

        CatalogException exception = assertThrows(CatalogException.class, () ->
                productCatalogController.getProducts(0, 20, null, null, null, true));

        assertEquals(ApiResponseEnum.CATALOGO_NO_DISPONIBLE, exception.getResponseStatus());
    }

    @Test
    @DisplayName("POST /api/catalog/sync invoca syncCatalog y responde 200 OK")
    void testSyncCatalog_Success() {
        doNothing().when(productCatalogService).syncCatalog();

        ResponseEntity<ApiResponse<String>> response = productCatalogController.syncCatalog();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productCatalogService, times(1)).syncCatalog();
    }
}
