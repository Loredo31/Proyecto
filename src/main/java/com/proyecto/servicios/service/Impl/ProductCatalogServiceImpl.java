package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.ProductCatalogClient;
import com.proyecto.servicios.dto.ProductCatalogResponseDto;
import com.proyecto.servicios.dto.ProductDto;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.CatalogException;
import com.proyecto.servicios.exception.ExternalServiceException;
import com.proyecto.servicios.model.ProductCatalogCache;
import com.proyecto.servicios.repository.ProductCatalogCacheRepository;
import com.proyecto.servicios.service.ProductCatalogService;
import com.proyecto.servicios.service.ProductCatalogXmlParser;
import feign.FeignException;
import feign.Response;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Implementación del catálogo de productos.
 * <p>
 * Responsabilidades:
 * <ul>
 *   <li>Tarea programada diaria a las 06:00 AM (cron configurable).</li>
 *   <li>Consumo del servicio externo vía OpenFeign con autenticación Bearer Token y política de reintentos.</li>
 *   <li>Soporte de respuestas tanto en XML (Cat Product XML) como en JSON.</li>
 *   <li>Almacenamiento del catálogo en MongoDB como caché exclusiva.</li>
 *   <li>Consulta local de la caché para el controlador REST devolviendo JSON.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCatalogServiceImpl implements ProductCatalogService {

    private static final int HTTP_OK = 200;

    private final ProductCatalogClient productCatalogClient;
    private final ProductCatalogCacheRepository cacheRepository;
    private final ObjectMapper objectMapper;
    private final ProductCatalogXmlParser xmlParser;

    @Value("${catalog.external.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${catalog.external.retry.delay-ms:2000}")
    private long retryDelayMs;

    /**
     * Sincroniza el catálogo todos los días a las 06:00 AM.
     * En caso de agotarse los reintentos, se registra una alerta crítica
     * sin exponer datos sensibles ni credenciales.
     */
    @Override
    @Scheduled(cron = "${catalog.external.cron:0 0 6 * * *}")
    public void syncCatalog() {
        log.info("Iniciando tarea programada de sincronización del catálogo de productos");
        try {
            List<ProductDto> products = retryTemplate().execute(context -> {
                log.info("Intento de sincronización #{} de {}", context.getRetryCount() + 1, maxAttempts);
                return fetchProductsFromExternalService();
            });

            persistCatalogInCache(products, HTTP_OK);
            log.info("Catálogo sincronizado exitosamente y almacenado en MongoDB. Total productos: {}", products.size());
        } catch (Exception ex) {
            log.error("ALERTA CRÍTICA: La sincronización del catálogo falló tras {} intentos. "
                    + "La caché de MongoDB NO fue actualizada. Detalle del error: {}", maxAttempts, ex.getMessage());
        }
    }

    /**
     * Obtiene el catálogo de productos desde la caché de MongoDB.
     *
     * @return lista de productos en caché
     * @throws CatalogException si la caché no existe o no tiene productos
     */
    @Override
    public List<ProductDto> getCatalogFromCache() {
        log.info("Consultando catálogo de productos desde caché MongoDB (clave: {})", ProductCatalogCache.CACHE_KEY);
        return cacheRepository.findById(ProductCatalogCache.CACHE_KEY)
                .map(ProductCatalogCache::getProducts)
                .filter(products -> products != null && !products.isEmpty())
                .orElseThrow(() -> {
                    log.warn("Caché de catálogo de productos no disponible o vacía en MongoDB");
                    return new CatalogException(ApiResponseEnum.CATALOGO_NO_DISPONIBLE);
                });
    }

    /**
     * Invoca el servicio externo y valida que la respuesta sea HTTP 200.
     * Registra el inicio y fin de la invocación midiendo el tiempo de respuesta.
     */
    private List<ProductDto> fetchProductsFromExternalService() {
        log.info("Iniciando invocación al servicio externo: GET /sistema/service/getProductList.do");
        long startTime = System.currentTimeMillis();

        try {
            Response response = productCatalogClient.getProductList();
            long duration = System.currentTimeMillis() - startTime;
            log.info("Fin de la invocación al servicio externo. Estado HTTP: {}. Tiempo transcurrido: {} ms",
                    response.status(), duration);

            if (response.status() != HTTP_OK) {
                log.error("Respuesta no exitosa recibida del servicio externo: HTTP {}", response.status());
                throw new ExternalServiceException(
                        "Respuesta no exitosa del servicio externo de catálogo", response.status());
            }

            List<ProductDto> products = parseResponseBody(response);
            log.info("Procesamiento exitoso del catálogo. Productos leídos: {}", products.size());
            return products;

        } catch (RetryableException ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Timeout de conexión o lectura con el servicio externo tras {} ms", duration);
            throw new ExternalServiceException("Timeout al conectar con el servicio externo de catálogo", 504, ex);
        } catch (FeignException ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Error Feign al consumir el servicio externo [HTTP {}] tras {} ms", ex.status(), duration);
            throw new ExternalServiceException("Error de comunicación con el servicio externo de catálogo",
                    ex.status(), ex);
        } catch (ResourceAccessException ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Error de acceso a red o timeout al consumir servicio externo tras {} ms", duration);
            throw new ExternalServiceException("Timeout o error de conexión con el servicio externo de catálogo",
                    504, ex);
        }
    }

    /**
     * Parsea la respuesta del servicio externo detectando automáticamente si es XML o JSON.
     */
    private List<ProductDto> parseResponseBody(Response response) {
        String body = readBody(response);
        if (body.isBlank()) {
            return Collections.emptyList();
        }

        String trimmed = body.trim();
        // Si comienza con '<' se procesa como XML (Cat Product XML)
        if (trimmed.startsWith("<")) {
            log.debug("Detectada respuesta en formato XML desde el servicio externo");
            return xmlParser.parseXml(trimmed);
        }

        // De lo contrario se procesa como JSON
        log.debug("Detectada respuesta en formato JSON desde el servicio externo");
        try {
            if (trimmed.startsWith("[")) {
                return objectMapper.readValue(trimmed, new TypeReference<List<ProductDto>>() {});
            }
            ProductCatalogResponseDto responseDto = objectMapper.readValue(trimmed, ProductCatalogResponseDto.class);
            return responseDto.getProducts() == null ? Collections.emptyList() : responseDto.getProducts();
        } catch (Exception ex) {
            log.error("Error al deserializar el cuerpo JSON del catálogo: {}", ex.getMessage());
            throw new ExternalServiceException("No fue posible parsear la respuesta JSON del servicio externo",
                    response.status(), ex);
        }
    }

    private String readBody(Response response) {
        if (response.body() == null) {
            return "";
        }
        try (java.io.InputStream is = response.body().asInputStream()) {
            return new String(is.readAllBytes(), response.charset() != null ? response.charset() : StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.error("Error al leer el stream del cuerpo de respuesta: {}", ex.getMessage());
            throw new ExternalServiceException("Error de lectura del cuerpo de respuesta externa",
                    response.status(), ex);
        }
    }

    private void persistCatalogInCache(List<ProductDto> products, int httpStatus) {
        log.info("Guardando catálogo en MongoDB (colección product_catalog_cache, clave: {})", ProductCatalogCache.CACHE_KEY);
        ProductCatalogCache cache = ProductCatalogCache.builder()
                .id(ProductCatalogCache.CACHE_KEY)
                .products(products)
                .updatedAt(LocalDateTime.now())
                .httpStatus(httpStatus)
                .build();
        cacheRepository.save(cache);
    }

    /**
     * Construye la política de reintentos configurable desde application.properties.
     */
    private RetryTemplate retryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(maxAttempts)
                .fixedBackoff(retryDelayMs)
                .retryOn(ExternalServiceException.class)
                .retryOn(FeignException.class)
                .retryOn(RetryableException.class)
                .retryOn(ResourceAccessException.class)
                .build();
    }
}

