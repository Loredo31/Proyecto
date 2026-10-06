package com.proyecto.servicios.controller;

import com.proyecto.servicios.dto.ApiResponse;
import com.proyecto.servicios.dto.ProductDto;
import com.proyecto.servicios.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador que expone el servicio de consulta del catálogo de productos
 * almacenado en la caché de MongoDB (Servicio en el diagrama de arquitectura).
 * <p>
 * Devuelve HTTP 200 con el catálogo en formato JSON si está disponible en MongoDB;
 * en caso contrario, delega al manejador global de excepciones para responder con el error.
 */
@Slf4j
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class ProductCatalogController {

    private final ProductCatalogService productCatalogService;

    /**
     * Consulta el catálogo de productos desde la caché local de MongoDB.
     * Corresponde a la rama "Consultar -> 200 -> Cat Product Json" del diagrama.
     *
     * @return HTTP 200 con el JSON de productos si está en MongoDB; error en caso contrario
     */
    @GetMapping(value = {"/products", ""}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<ProductDto>>> getProducts() {
        log.info("Recibida petición GET para consultar catálogo de productos desde MongoDB");
        List<ProductDto> products = productCatalogService.getCatalogFromCache();
        log.info("Catálogo recuperado exitosamente con {} productos. Respondiendo HTTP 200 JSON.", products.size());
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * Dispara manualmente la sincronización con el servicio externo sin esperar la tarea de las 06:00 AM.
     * Útil para verificación, pruebas y administración.
     *
     * @return HTTP 200 indicando que el proceso de sincronización fue ejecutado
     */
    @PostMapping(value = "/sync", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> syncCatalog() {
        log.info("Petición manual recibida para sincronizar catálogo de productos");
        productCatalogService.syncCatalog();
        return ResponseEntity.ok(ApiResponse.success("Proceso de sincronización ejecutado correctamente"));
    }
}

