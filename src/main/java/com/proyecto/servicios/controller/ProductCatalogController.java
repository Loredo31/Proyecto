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

@Slf4j
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class ProductCatalogController {

    private final ProductCatalogService productCatalogService;

    @GetMapping(value = {"/products", ""}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<ProductDto>>> getProducts() {
        log.info("Recibida petición GET para consultar catálogo de productos desde MongoDB");
        List<ProductDto> products = productCatalogService.getCatalogFromCache();
        log.info("Catálogo recuperado exitosamente con {} productos. Respondiendo HTTP 200 JSON.", products.size());
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PostMapping(value = "/sync", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> syncCatalog() {
        log.info("Petición manual recibida para sincronizar catálogo de productos");
        productCatalogService.syncCatalog();
        return ResponseEntity.ok(ApiResponse.success("Proceso de sincronización ejecutado correctamente"));
    }
}

