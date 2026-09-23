package com.proyecto.servicios.client;

import feign.Response;

/**
 * Contrato de integración para la API de GestoPago Cat/Product (API GP Cat/product).
 * Define la operación para obtener el listado de productos desde el servicio externo.
 */
public interface GestoPagoCatProduct {

    /**
     * Consulta el catálogo de productos desde el servicio externo (GET /sistema/service/getProductList.do).
     *
     * @return respuesta HTTP cruda para validación de código de estado y payload (XML/JSON)
     */
    Response getProductList();
}

