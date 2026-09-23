package com.proyecto.servicios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.ProductCatalogClient;
import com.proyecto.servicios.dto.ProductDto;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.CatalogException;
import com.proyecto.servicios.model.ProductCatalogCache;
import com.proyecto.servicios.repository.ProductCatalogCacheRepository;
import com.proyecto.servicios.service.Impl.ProductCatalogServiceImpl;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas unitarias de la capa de servicio: ProductCatalogService")
class ProductCatalogServiceTest {

    @Mock
    private ProductCatalogClient productCatalogClient;

    @Mock
    private ProductCatalogCacheRepository cacheRepository;

    private ObjectMapper objectMapper;
    private ProductCatalogXmlParser xmlParser;
    private ProductCatalogServiceImpl productCatalogService;
    private Request dummyRequest;

    @BeforeEach
    void setUp() {
        dummyRequest = mock(Request.class);
        objectMapper = new ObjectMapper();
        xmlParser = new ProductCatalogXmlParser();

        productCatalogService = new ProductCatalogServiceImpl(
                productCatalogClient,
                cacheRepository,
                objectMapper,
                xmlParser
        );

        ReflectionTestUtils.setField(productCatalogService, "maxAttempts", 3);
        ReflectionTestUtils.setField(productCatalogService, "retryDelayMs", 10L);
    }

    private Response buildResponse(int status, String body) {
        byte[] bytes = body != null ? body.getBytes(StandardCharsets.UTF_8) : new byte[0];
        Map<String, Collection<String>> headers = Collections.emptyMap();
        return Response.builder()
                .status(status)
                .reason(status == 200 ? "OK" : "Error")
                .request(dummyRequest)
                .headers(headers)
                .body(bytes)
                .build();
    }

    @Test
    @DisplayName("syncCatalog() exitoso con respuesta JSON debe persistir en MongoDB")
    void testSyncCatalog_Successful_JsonResponse() {
        String json = """
                {
                    "products": [
                        {
                            "id": "1",
                            "sku": "SKU-001",
                            "name": "Paquete Telcel",
                            "description": "Recarga 100",
                            "price": 100.0,
                            "stock": 50,
                            "category": "Telefonia",
                            "active": true
                        }
                    ]
                }
                """;

        when(productCatalogClient.getProductList()).thenReturn(buildResponse(200, json));

        productCatalogService.syncCatalog();

        ArgumentCaptor<ProductCatalogCache> captor = ArgumentCaptor.forClass(ProductCatalogCache.class);
        verify(cacheRepository, times(1)).save(captor.capture());

        ProductCatalogCache savedCache = captor.getValue();
        assertEquals(ProductCatalogCache.CACHE_KEY, savedCache.getId());
        assertEquals(200, savedCache.getHttpStatus());
        assertNotNull(savedCache.getProducts());
        assertEquals(1, savedCache.getProducts().size());

        ProductDto product = savedCache.getProducts().get(0);
        assertEquals("1", product.getId());
        assertEquals("SKU-001", product.getSku());
        assertEquals("Paquete Telcel", product.getName());
    }

    @Test
    @DisplayName("syncCatalog() exitoso con respuesta XML (Cat Product XML) debe parsear y persistir en MongoDB")
    void testSyncCatalog_Successful_XmlResponse() {
        String xml = """
                <CatProduct>
                    <product>
                        <id>99</id>
                        <sku>GP-XML-99</sku>
                        <name>Producto XML GestoPago</name>
                        <description>Descripción XML</description>
                        <price>150.50</price>
                        <stock>20</stock>
                        <category>Servicios</category>
                        <active>true</active>
                    </product>
                </CatProduct>
                """;

        when(productCatalogClient.getProductList()).thenReturn(buildResponse(200, xml));

        productCatalogService.syncCatalog();

        ArgumentCaptor<ProductCatalogCache> captor = ArgumentCaptor.forClass(ProductCatalogCache.class);
        verify(cacheRepository, times(1)).save(captor.capture());

        ProductCatalogCache savedCache = captor.getValue();
        assertEquals(ProductCatalogCache.CACHE_KEY, savedCache.getId());
        assertNotNull(savedCache.getProducts());
        assertEquals(1, savedCache.getProducts().size());

        ProductDto product = savedCache.getProducts().get(0);
        assertEquals("99", product.getId());
        assertEquals("GP-XML-99", product.getSku());
        assertEquals("Producto XML GestoPago", product.getName());
    }

    @Test
    @DisplayName("syncCatalog() con respuesta HTTP 500 debe reintentar y NO actualizar MongoDB si se agotan intentos")
    void testSyncCatalog_ExternalServiceReturns500_RetriesAndFails() {
        when(productCatalogClient.getProductList()).thenReturn(buildResponse(500, "Internal Server Error"));

        productCatalogService.syncCatalog();

        verify(productCatalogClient, times(3)).getProductList();
        verify(cacheRepository, never()).save(any());
    }

    @Test
    @DisplayName("syncCatalog() con error HTTP 401 Unauthorized debe reintentar y no actualizar caché")
    void testSyncCatalog_Unauthorized401() {
        when(productCatalogClient.getProductList()).thenReturn(buildResponse(401, "Unauthorized"));

        productCatalogService.syncCatalog();

        verify(productCatalogClient, times(3)).getProductList();
        verify(cacheRepository, never()).save(any());
    }

    @Test
    @DisplayName("syncCatalog() ante Timeout (RetryableException) debe reintentar y fallar controladamente")
    void testSyncCatalog_Timeout() {
        RetryableException timeoutException = mock(RetryableException.class);
        when(productCatalogClient.getProductList()).thenThrow(timeoutException);

        productCatalogService.syncCatalog();

        verify(productCatalogClient, times(3)).getProductList();
        verify(cacheRepository, never()).save(any());
    }

    @Test
    @DisplayName("syncCatalog() ante ResourceAccessException debe reintentar y fallar controladamente")
    void testSyncCatalog_ResourceAccessException() {
        when(productCatalogClient.getProductList()).thenThrow(new ResourceAccessException("Timeout de red"));

        productCatalogService.syncCatalog();

        verify(productCatalogClient, times(3)).getProductList();
        verify(cacheRepository, never()).save(any());
    }

    @Test
    @DisplayName("getCatalogFromCache() debe devolver productos cuando existen en MongoDB")
    void testGetCatalogFromCache_Success() {
        ProductDto p1 = ProductDto.builder().id("1").name("Prod 1").price(new BigDecimal("10.0")).build();
        ProductCatalogCache cache = ProductCatalogCache.builder()
                .id(ProductCatalogCache.CACHE_KEY)
                .products(List.of(p1))
                .updatedAt(LocalDateTime.now())
                .httpStatus(200)
                .build();

        when(cacheRepository.findById(ProductCatalogCache.CACHE_KEY)).thenReturn(Optional.of(cache));

        List<ProductDto> result = productCatalogService.getCatalogFromCache();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("1", result.get(0).getId());
        assertEquals("Prod 1", result.get(0).getName());
    }

    @Test
    @DisplayName("getCatalogFromCache() debe lanzar CatalogException cuando la caché no existe en MongoDB")
    void testGetCatalogFromCache_NotFound_ThrowsCatalogException() {
        when(cacheRepository.findById(ProductCatalogCache.CACHE_KEY)).thenReturn(Optional.empty());

        CatalogException exception = assertThrows(CatalogException.class, () ->
                productCatalogService.getCatalogFromCache()
        );

        assertEquals(ApiResponseEnum.CATALOGO_NO_DISPONIBLE, exception.getResponseStatus());
    }

    @Test
    @DisplayName("getCatalogFromCache() debe lanzar CatalogException si la lista de productos en caché está vacía")
    void testGetCatalogFromCache_EmptyProducts_ThrowsCatalogException() {
        ProductCatalogCache cache = ProductCatalogCache.builder()
                .id(ProductCatalogCache.CACHE_KEY)
                .products(Collections.emptyList())
                .updatedAt(LocalDateTime.now())
                .httpStatus(200)
                .build();

        when(cacheRepository.findById(ProductCatalogCache.CACHE_KEY)).thenReturn(Optional.of(cache));

        CatalogException exception = assertThrows(CatalogException.class, () ->
                productCatalogService.getCatalogFromCache()
        );

        assertEquals(ApiResponseEnum.CATALOGO_NO_DISPONIBLE, exception.getResponseStatus());
    }
}
