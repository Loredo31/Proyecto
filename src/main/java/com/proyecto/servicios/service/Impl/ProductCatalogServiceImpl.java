package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.ProductCatalogClient;
import com.proyecto.servicios.dto.ProductCatalogResponseDto;
import com.proyecto.servicios.dto.ProductDto;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.CatalogException;
import com.proyecto.servicios.exception.ExternalServiceException;
import com.proyecto.servicios.entity.gestopago.CatalogoProducto;
import com.proyecto.servicios.repository.CatalogoProductoRepository;
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
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCatalogServiceImpl implements ProductCatalogService {

    private static final int HTTP_OK = 200;
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    private final ProductCatalogClient productCatalogClient;
    private final CatalogoProductoRepository catalogoRepository;
    private final ObjectMapper objectMapper;
    private final ProductCatalogXmlParser xmlParser;

    @Value("${catalog.external.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${catalog.external.retry.delay-ms:2000}")
    private long retryDelayMs;

    @Override
    @Scheduled(cron = "${catalog.external.cron:0 0 6 * * *}")
    public void syncCatalog() {
        log.info("Iniciando tarea programada de sincronización del catálogo de productos");
        try {
            List<ProductDto> products = retryTemplate().execute(context -> {
                log.info("Intento de sincronización #{} de {}", context.getRetryCount() + 1, maxAttempts);
                return fetchProductsFromExternalService();
            });

            int guardados = guardarCatalogo(products);
            log.info("Sincronización del catálogo terminada. Productos recibidos: {}, guardados en PostgreSQL: {}",
                    products.size(), guardados);
        } catch (Exception ex) {
            log.error("ALERTA CRÍTICA: La sincronización del catálogo falló tras {} intentos. "
                    + "El catálogo en PostgreSQL NO fue modificado. Detalle del error: {}", maxAttempts, ex.getMessage());
        }
    }

    /** Lee el catálogo de la tabla {@code catalogo_productos}; nunca consulta a GestoPago. */
    @Override
    public List<ProductDto> getCatalogFromCache() {
        List<ProductDto> productos = catalogoRepository.findAll().stream()
                .map(ProductCatalogServiceImpl::aDto)
                .toList();
        if (productos.isEmpty()) {
            log.warn("El catálogo de productos en PostgreSQL está vacío");
            throw new CatalogException(ApiResponseEnum.CATALOGO_NO_DISPONIBLE);
        }
        return productos;
    }

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

    private List<ProductDto> parseResponseBody(Response response) {
        String body = readBody(response);
        if (body.isBlank()) {
            return Collections.emptyList();
        }

        String trimmed = body.trim();

        if (trimmed.startsWith("<")) {
            log.debug("Detectada respuesta en formato XML desde el servicio externo");
            return xmlParser.parseXml(trimmed);
        }

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

    /**
     * Inserta o actualiza los productos recibidos. Si GestoPago responde sin productos (p. ej. un XML vacío)
     * no se toca la tabla, para no perder el catálogo existente. Los productos que ya no lleguen se conservan.
     */
    private int guardarCatalogo(List<ProductDto> products) {
        LocalDateTime ahora = LocalDateTime.now(ZONE_ID);
        List<CatalogoProducto> entidades = products.stream()
                .map(p -> aEntidad(p, ahora))
                .flatMap(Optional::stream)
                .toList();
        if (entidades.isEmpty()) {
            log.warn("GestoPago no devolvió productos válidos; se conserva el catálogo actual en PostgreSQL");
            return 0;
        }
        catalogoRepository.saveAll(entidades);
        return entidades.size();
    }

    private static Optional<CatalogoProducto> aEntidad(ProductDto p, LocalDateTime ahora) {
        Integer idProducto = enteroONulo(p.getId());
        if (idProducto == null || p.getIdServicio() == null || p.getName() == null || p.getName().isBlank()) {
            log.warn("Producto omitido por datos incompletos: id={}, idServicio={}", p.getId(), p.getIdServicio());
            return Optional.empty();
        }
        return Optional.of(CatalogoProducto.builder()
                .idProducto(idProducto)
                .idServicio(p.getIdServicio())
                .servicio(p.getCategory())
                .nombreProducto(p.getName().trim())
                .idCatTipoServicio(p.getIdCatTipoServicio())
                .tipoFront(p.getTipoFront())
                .precio(p.getPrice())
                .tipoReferencia(p.getTipoReferencia())
                .legend(p.getDescription())
                .activo(!Boolean.FALSE.equals(p.getActive()))
                .fechaActualizacion(ahora)
                .build());
    }

    private static ProductDto aDto(CatalogoProducto c) {
        return ProductDto.builder()
                .id(String.valueOf(c.getIdProducto()))
                .name(c.getNombreProducto())
                .description(c.getLegend())
                .price(c.getPrecio())
                .category(c.getServicio())
                .active(c.getActivo())
                .idServicio(c.getIdServicio())
                .idCatTipoServicio(c.getIdCatTipoServicio())
                .tipoFront(c.getTipoFront())
                .tipoReferencia(c.getTipoReferencia())
                .build();
    }

    private static Integer enteroONulo(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

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

