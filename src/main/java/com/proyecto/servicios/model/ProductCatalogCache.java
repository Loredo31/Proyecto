package com.proyecto.servicios.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.proyecto.servicios.dto.ProductDto;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "product_catalog_cache")
public class ProductCatalogCache {

    public static final String CACHE_KEY = "PRODUCT_LIST";

    @Id
    private String id;

    private List<ProductDto> products;

    private LocalDateTime updatedAt;

    private Integer httpStatus;
}

