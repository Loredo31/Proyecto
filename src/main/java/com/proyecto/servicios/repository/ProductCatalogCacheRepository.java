package com.proyecto.servicios.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.proyecto.servicios.model.ProductCatalogCache;

/**
 * Repositorio de la caché del catálogo en MongoDB.
 */
@Repository
public interface ProductCatalogCacheRepository extends MongoRepository<ProductCatalogCache, String> {
}
