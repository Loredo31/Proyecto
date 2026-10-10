package com.proyecto.servicios.service;

import java.util.List;

import com.proyecto.servicios.dto.ProductDto;

public interface ProductCatalogService {

    void syncCatalog();

    List<ProductDto> getCatalogFromCache();
}

