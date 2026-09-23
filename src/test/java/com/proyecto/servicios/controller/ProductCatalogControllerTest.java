package com.proyecto.servicios.controller;

import com.proyecto.servicios.dto.ApiResponse;
import com.proyecto.servicios.dto.ProductDto;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.CatalogException;
import com.proyecto.servicios.service.ProductCatalogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas unitarias para ProductCatalogController")
class ProductCatalogControllerTest {

    @Mock
    private ProductCatalogService productCatalogService;

    @InjectMocks
    private ProductCatalogController productCatalogController;

    @Test
    @DisplayName("GET /api/catalog/products debe responder 200 OK con la lista de productos")
    void testGetProducts_Success() {
        ProductDto product = ProductDto.builder()
                .id("1")
                .sku("SKU-100")
                .name("Recarga Telcel")
                .price(new BigDecimal("100.00"))
                .stock(50)
                .active(true)
                .build();

        when(productCatalogService.getCatalogFromCache()).thenReturn(List.of(product));

        ResponseEntity<ApiResponse<List<ProductDto>>> response = productCatalogController.getProducts();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ApiResponseEnum.EXITO.getCode(), response.getBody().getCode());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("Recarga Telcel", response.getBody().getData().get(0).getName());

        verify(productCatalogService, times(1)).getCatalogFromCache();
    }

    @Test
    @DisplayName("GET /api/catalog/products debe propagar CatalogException si la caché está vacía")
    void testGetProducts_EmptyCache_ThrowsException() {
        when(productCatalogService.getCatalogFromCache())
                .thenThrow(new CatalogException(ApiResponseEnum.CATALOGO_NO_DISPONIBLE));

        CatalogException exception = assertThrows(CatalogException.class, () ->
                productCatalogController.getProducts()
        );

        assertEquals(ApiResponseEnum.CATALOGO_NO_DISPONIBLE, exception.getResponseStatus());
        verify(productCatalogService, times(1)).getCatalogFromCache();
    }

    @Test
    @DisplayName("POST /api/catalog/sync debe invocar syncCatalog y responder 200 OK")
    void testSyncCatalog_Success() {
        doNothing().when(productCatalogService).syncCatalog();

        ResponseEntity<ApiResponse<String>> response = productCatalogController.syncCatalog();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ApiResponseEnum.EXITO.getCode(), response.getBody().getCode());

        verify(productCatalogService, times(1)).syncCatalog();
    }
}
