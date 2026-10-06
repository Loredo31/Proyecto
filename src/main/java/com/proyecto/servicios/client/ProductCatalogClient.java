package com.proyecto.servicios.client;

import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Cliente OpenFeign del servicio externo de catálogo de productos (GestoPago Cat/Product).
 * La URL base y el token de autenticación se resuelven desde application.properties
 * (nunca hardcodeados en el código fuente).
 * <p>
 * Devuelve {@link Response} en crudo para que la capa de servicio
 * valide explícitamente que el código HTTP sea 200.
 */
@FeignClient(
        name = "productCatalogClient",
        url = "${catalog.external.base-url}",
        path = "/sistema/service"
)
public interface ProductCatalogClient extends GestoPagoCatProduct {

    /**
     * Consulta el listado completo de productos del catálogo.
     *
     * @return respuesta HTTP cruda del servicio externo
     */
    @Override
    @GetMapping("/getProductList.do")
    Response getProductList();
}

