package com.proyecto.servicios.controller;

import com.proyecto.servicios.dto.ApiResponse;
import com.proyecto.servicios.dto.PaginaDto;
import com.proyecto.servicios.dto.ProductDto;
import com.proyecto.servicios.service.ConsultaCatalogoService;
import com.proyecto.servicios.service.ConsultaCatalogoService.FiltroCatalogo;
import com.proyecto.servicios.service.ProductCatalogService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class ProductCatalogController {

    private final ProductCatalogService productCatalogService;
    private final ConsultaCatalogoService consultaCatalogoService;

    @GetMapping(value = {"/products", ""}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PaginaDto<ProductDto>>> getProducts(
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano,
            @RequestParam(required = false) @Size(max = 60) String categoria,
            @RequestParam(required = false) @Size(max = 60) String busqueda,
            @RequestParam(required = false) Integer idServicio,
            @RequestParam(defaultValue = "true") boolean soloActivos) {
        PaginaDto<ProductDto> productos = consultaCatalogoService.consultar(
                new FiltroCatalogo(pagina, tamano, categoria, busqueda, idServicio, soloActivos));
        log.info("Catálogo consultado: página {} con {} de {} productos", pagina,
                productos.contenido().size(), productos.totalElementos());
        return ResponseEntity.ok(ApiResponse.success(productos));
    }

    @GetMapping(value = "/products/detalle", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ProductDto>> getProducto(@RequestParam @Size(min = 1, max = 20) String id) {
        return ResponseEntity.ok(ApiResponse.success(consultaCatalogoService.detalle(id)));
    }

    @GetMapping(value = "/categorias", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<String>>> getCategorias() {
        return ResponseEntity.ok(ApiResponse.success(consultaCatalogoService.categorias()));
    }

    @PostMapping(value = "/sync", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> syncCatalog() {
        log.info("Petición manual recibida para sincronizar catálogo de productos");
        productCatalogService.syncCatalog();
        return ResponseEntity.ok(ApiResponse.success("Proceso de sincronización ejecutado correctamente"));
    }
}
