package com.proyecto.servicios.service;

import java.util.List;
import java.util.Optional;

import com.proyecto.servicios.dto.ProductDto;

public interface ProductCatalogService {

    void syncCatalog();

    List<ProductDto> getCatalogFromCache();

    /** Un producto del catálogo por su id (vacío si no existe). */
    Optional<ProductDto> buscarPorId(Integer idProducto);
}

