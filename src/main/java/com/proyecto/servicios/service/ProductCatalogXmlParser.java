package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.ProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilería para parsear respuestas XML del catálogo de productos (Cat Product XML).
 * Cuenta con protección contra ataques XXE y soporta convenciones tanto en inglés
 * como en español para mayor tolerancia y robustez.
 */
@Slf4j
@Component
public class ProductCatalogXmlParser {

    /**
     * Parsea un string con contenido XML y extrae la lista de {@link ProductDto}.
     *
     * @param xml contenido XML en crudo
     * @return lista de productos parseados
     */
    public List<ProductDto> parseXml(String xml) {
        List<ProductDto> products = new ArrayList<>();
        if (xml == null || xml.isBlank()) {
            return products;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Mitigación estricta contra ataques XXE (XML External Entity)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml.trim())));
            document.getDocumentElement().normalize();

            // Buscar elementos de producto por diversas etiquetas comunes
            NodeList nodeList = findProductNodes(document);

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    ProductDto product = mapElementToProduct((Element) node);
                    if (product != null) {
                        products.add(product);
                    }
                }
            }

            log.debug("Se parsearon {} productos desde el XML", products.size());
        } catch (Exception ex) {
            log.error("Error al procesar el contenido XML del catálogo de productos: {}", ex.getMessage());
            throw new IllegalArgumentException("Formato XML de catálogo no válido: " + ex.getMessage(), ex);
        }

        return products;
    }

    private NodeList findProductNodes(Document document) {
        String[] possibleTags = {"product", "producto", "item", "cProduct", "catProduct"};
        for (String tag : possibleTags) {
            NodeList list = document.getElementsByTagName(tag);
            if (list.getLength() > 0) {
                return list;
            }
        }
        // Si no coincide con etiquetas comunes, usar hijos directos del nodo raíz
        return document.getDocumentElement().getChildNodes();
    }

    private ProductDto mapElementToProduct(Element element) {
        String id = getFirstTagValue(element, "id", "idProducto", "id_producto");
        String sku = getFirstTagValue(element, "sku", "codigo", "codigoProducto", "clave");
        String name = getFirstTagValue(element, "name", "nombre", "nombreProducto", "descripcionCorta");
        String description = getFirstTagValue(element, "description", "descripcion", "detalle");
        String priceStr = getFirstTagValue(element, "price", "precio", "monto", "costo");
        String stockStr = getFirstTagValue(element, "stock", "existencia", "cantidad");
        String category = getFirstTagValue(element, "category", "categoria", "familia");
        String activeStr = getFirstTagValue(element, "active", "activo", "estatus", "status");

        // Si el elemento no tiene datos relevantes, se ignora
        if (id == null && sku == null && name == null) {
            return null;
        }

        BigDecimal price = null;
        if (priceStr != null && !priceStr.isBlank()) {
            try {
                price = new BigDecimal(priceStr.trim());
            } catch (NumberFormatException ignored) {
                log.warn("No se pudo convertir el precio '{}' a BigDecimal", priceStr);
            }
        }

        Integer stock = null;
        if (stockStr != null && !stockStr.isBlank()) {
            try {
                stock = Integer.parseInt(stockStr.trim());
            } catch (NumberFormatException ignored) {
                log.warn("No se pudo convertir el stock '{}' a Integer", stockStr);
            }
        }

        Boolean active = null;
        if (activeStr != null && !activeStr.isBlank()) {
            active = "true".equalsIgnoreCase(activeStr.trim()) || "1".equals(activeStr.trim()) || "si".equalsIgnoreCase(activeStr.trim());
        }

        return ProductDto.builder()
                .id(id)
                .sku(sku)
                .name(name)
                .description(description)
                .price(price)
                .stock(stock)
                .category(category)
                .active(active)
                .build();
    }

    private String getFirstTagValue(Element parent, String... tags) {
        for (String tag : tags) {
            NodeList list = parent.getElementsByTagName(tag);
            if (list.getLength() > 0) {
                Node node = list.item(0);
                if (node != null && node.getTextContent() != null) {
                    return node.getTextContent().trim();
                }
            }
        }
        return null;
    }
}
