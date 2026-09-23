package com.proyecto.servicios.service;

import java.util.List;

import com.proyecto.servicios.dto.ProductDto;

/**
 * Servicio del catálogo de productos: tarea programada,
 * integración con el servicio externo y consulta de la caché MongoDB.
 */
public interface ProductCatalogService {

    /**
     * Ejecuta la sincronización del catálogo con el servicio externo.
     * Invocada por la tarea programada a las 06:00 AM.
     */
    void syncCatalog();

    /**
     * Obtiene el catálogo de productos desde la caché de MongoDB.
     *
     * @return lista de productos cacheados
     * @throws com.proyecto.servicios.exception.CatalogException si la caché está vacía
     */
    List<ProductDto> getCatalogFromCache();
}
