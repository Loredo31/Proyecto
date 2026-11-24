package com.proyecto.servicios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.ProductCatalogClient;
import com.proyecto.servicios.dto.ProductDto;
import com.proyecto.servicios.entity.gestopago.CatalogoProducto;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.CatalogException;
import com.proyecto.servicios.repository.CatalogoProductoRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas unitarias de la capa de servicio: ProductCatalogService (catálogo en PostgreSQL)")
class ProductCatalogServiceTest {

    @Mock
    private ProductCatalogClient productCatalogClient;

    @Mock
    private CatalogoProductoRepository catalogoRepository;

    private ProductCatalogServiceImpl productCatalogService;
    private Request dummyRequest;

    @BeforeEach
    void setUp() {
        dummyRequest = mock(Request.class);
        productCatalogService = new ProductCatalogServiceImpl(
                productCatalogClient,
                catalogoRepository,
                new ObjectMapper(),
                new ProductCatalogXmlParser()
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

    @SuppressWarnings("unchecked")
    private List<CatalogoProducto> capturarGuardados() {
        ArgumentCaptor<List<CatalogoProducto>> captor = ArgumentCaptor.forClass(List.class);
        verify(catalogoRepository, times(1)).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("syncCatalog() con respuesta JSON guarda los productos en PostgreSQL")
    void testSyncCatalog_Successful_JsonResponse() {
        String json = """
                { "products": [ { "id": "1", "name": "Paquete Telcel", "description": "Recarga 100", "price": 100.0,
                                  "category": "Telcel", "active": true, "idServicio": 133, "tipoFront": 1 } ] }
                """;
        when(productCatalogClient.getProductList()).thenReturn(buildResponse(200, json));

        productCatalogService.syncCatalog();

        List<CatalogoProducto> guardados = capturarGuardados();
        assertEquals(1, guardados.size());
        CatalogoProducto p = guardados.get(0);
        assertEquals(1, p.getIdProducto());
        assertEquals(133, p.getIdServicio());
        assertEquals("Paquete Telcel", p.getNombreProducto());
        assertEquals("Telcel", p.getServicio());
        assertEquals(new BigDecimal("100.0"), p.getPrecio());
        assertTrue(p.getActivo());
        assertNotNull(p.getFechaActualizacion());
    }

    @Test
    @DisplayName("syncCatalog() con el XML real de GestoPago (atributos) guarda los productos")
    void testSyncCatalog_Successful_XmlGestoPago() {
        String xml = """
                <RESPONSE>
                    <MENSAJE><CODIGO>01</CODIGO><TEXTO>Operacion realizada con exito</TEXTO></MENSAJE>
                    <PRODUCTOS>
                        <producto servicio='Amazon' producto='Amazon $100' idServicio='71' idProducto='200'
                                  idCatTipoServicio='10' tipoFront='1' precio='100.0' tipoReferencia='a'>
                            <legend><![CDATA[Conserva tu comprobante]]></legend>
                        </producto>
                    </PRODUCTOS>
                </RESPONSE>
                """;
        when(productCatalogClient.getProductList()).thenReturn(buildResponse(200, xml));

        productCatalogService.syncCatalog();

        CatalogoProducto p = capturarGuardados().get(0);
        assertEquals(200, p.getIdProducto());
        assertEquals(71, p.getIdServicio());
        assertEquals("Amazon $100", p.getNombreProducto());
        assertEquals("Amazon", p.getServicio());
        assertEquals(1, p.getTipoFront());
        assertEquals(10, p.getIdCatTipoServicio());
    }

    @Test
    @DisplayName("syncCatalog() con cuerpo vacío no modifica el catálogo existente")
    void testSyncCatalog_CuerpoVacio_NoModificaCatalogo() {
        when(productCatalogClient.getProductList()).thenReturn(buildResponse(200, ""));

        productCatalogService.syncCatalog();

        verify(catalogoRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("syncCatalog() con XML sin productos no modifica el catálogo existente")
    void testSyncCatalog_XmlSinProductos_NoModificaCatalogo() {
        when(productCatalogClient.getProductList()).thenReturn(buildResponse(200, "<RESPONSE></RESPONSE>"));

        productCatalogService.syncCatalog();

        verify(catalogoRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("syncCatalog() omite productos con id no numérico o sin idServicio")
    void testSyncCatalog_OmiteProductosIncompletos() {
        String json = """
                [ { "id": "ABC", "name": "Sin id numérico", "idServicio": 1 },
                  { "id": "5", "name": "Sin servicio" },
                  { "id": "6", "name": "Valido", "idServicio": 2, "active": false } ]
                """;
        when(productCatalogClient.getProductList()).thenReturn(buildResponse(200, json));

        productCatalogService.syncCatalog();

        List<CatalogoProducto> guardados = capturarGuardados();
        assertEquals(1, guardados.size());
        assertEquals(6, guardados.get(0).getIdProducto());
        assertFalse(guardados.get(0).getActivo());
    }

    @Test
    @DisplayName("syncCatalog() con HTTP 500 reintenta y no modifica el catálogo")
    void testSyncCatalog_ExternalServiceReturns500_RetriesAndFails() {
        when(productCatalogClient.getProductList()).thenReturn(buildResponse(500, "Internal Server Error"));

        productCatalogService.syncCatalog();

        verify(productCatalogClient, times(3)).getProductList();
        verify(catalogoRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("syncCatalog() con HTTP 401 reintenta y no modifica el catálogo")
    void testSyncCatalog_Unauthorized401() {
        when(productCatalogClient.getProductList()).thenReturn(buildResponse(401, "Unauthorized"));

        productCatalogService.syncCatalog();

        verify(productCatalogClient, times(3)).getProductList();
        verify(catalogoRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("syncCatalog() ante Timeout (RetryableException) reintenta y falla controladamente")
    void testSyncCatalog_Timeout() {
        when(productCatalogClient.getProductList()).thenThrow(mock(RetryableException.class));

        productCatalogService.syncCatalog();

        verify(productCatalogClient, times(3)).getProductList();
        verify(catalogoRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("syncCatalog() ante ResourceAccessException reintenta y falla controladamente")
    void testSyncCatalog_ResourceAccessException() {
        when(productCatalogClient.getProductList()).thenThrow(new ResourceAccessException("Timeout de red"));

        productCatalogService.syncCatalog();

        verify(productCatalogClient, times(3)).getProductList();
        verify(catalogoRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("getCatalogFromCache() convierte las filas de PostgreSQL a ProductDto")
    void testGetCatalogFromCache_Success() {
        CatalogoProducto fila = CatalogoProducto.builder()
                .idProducto(582).idServicio(133).servicio("Telcel").nombreProducto("Telcel $100")
                .idCatTipoServicio(2).tipoFront(1).precio(new BigDecimal("100.00")).tipoReferencia("a")
                .legend("Conserva tu comprobante").activo(true).fechaActualizacion(LocalDateTime.now())
                .build();
        when(catalogoRepository.findAll()).thenReturn(List.of(fila));

        List<ProductDto> result = productCatalogService.getCatalogFromCache();

        assertEquals(1, result.size());
        ProductDto p = result.get(0);
        assertEquals("582", p.getId());
        assertEquals("Telcel $100", p.getName());
        assertEquals("Telcel", p.getCategory());
        assertEquals(133, p.getIdServicio());
        assertEquals(1, p.getTipoFront());
        assertEquals(new BigDecimal("100.00"), p.getPrice());
        assertEquals("Conserva tu comprobante", p.getDescription());
        assertTrue(p.getActive());
    }

    @Test
    @DisplayName("getCatalogFromCache() lanza CatalogException si la tabla está vacía")
    void testGetCatalogFromCache_Vacio_ThrowsCatalogException() {
        when(catalogoRepository.findAll()).thenReturn(Collections.emptyList());

        CatalogException exception = assertThrows(CatalogException.class, () ->
                productCatalogService.getCatalogFromCache());

        assertEquals(ApiResponseEnum.CATALOGO_NO_DISPONIBLE, exception.getResponseStatus());
    }
}
