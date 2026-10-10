package com.proyecto.servicios.client;

import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "productCatalogClient",
        url = "${catalog.external.base-url}",
        path = "/sistema/service"
)
public interface ProductCatalogClient extends GestoPagoCatProduct {

    @Override
    @GetMapping("/getProductList.do")
    Response getProductList();
}

