package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.ProductDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas unitarias para ProductCatalogXmlParser")
class ProductCatalogXmlParserTest {

    private ProductCatalogXmlParser parser;

    @BeforeEach
    void setUp() {
        parser = new ProductCatalogXmlParser();
    }

    @Test
    @DisplayName("Debe parsear correctamente XML con estructura estándar <products><product>")
    void testParseXml_StandardEnglish() {
        String xml = """
                <products>
                    <product>
                        <id>101</id>
                        <sku>SKU-TEL-100</sku>
                        <name>Recarga Telcel 100</name>
                        <description>Tiempo aire de $100 MXN</description>
                        <price>100.00</price>
                        <stock>500</stock>
                        <category>Telefonia</category>
                        <active>true</active>
                    </product>
                    <product>
                        <id>102</id>
                        <sku>SKU-MOV-50</sku>
                        <name>Recarga Movistar 50</name>
                        <description>Tiempo aire de $50 MXN</description>
                        <price>50.00</price>
                        <stock>200</stock>
                        <category>Telefonia</category>
                        <active>false</active>
                    </product>
                </products>
                """;

        List<ProductDto> products = parser.parseXml(xml);

        assertNotNull(products);
        assertEquals(2, products.size());

        ProductDto p1 = products.get(0);
        assertEquals("101", p1.getId());
        assertEquals("SKU-TEL-100", p1.getSku());
        assertEquals("Recarga Telcel 100", p1.getName());
        assertEquals("Tiempo aire de $100 MXN", p1.getDescription());
        assertEquals(new BigDecimal("100.00"), p1.getPrice());
        assertEquals(500, p1.getStock());
        assertEquals("Telefonia", p1.getCategory());
        assertTrue(p1.getActive());

        ProductDto p2 = products.get(1);
        assertEquals("102", p2.getId());
        assertEquals("SKU-MOV-50", p2.getSku());
        assertFalse(p2.getActive());
    }

    @Test
    @DisplayName("Debe parsear correctamente XML con etiquetas en español (GestoPago CatProduct)")
    void testParseXml_SpanishTags() {
        String xml = """
                <CatProduct>
                    <producto>
                        <idProducto>201</idProducto>
                        <codigo>GP-CFE-01</codigo>
                        <nombre>Pago de Servicio CFE</nombre>
                        <descripcion>Comisión por pago de luz CFE</descripcion>
                        <precio>12.50</precio>
                        <existencia>999</existencia>
                        <categoria>Servicios</categoria>
                        <activo>true</activo>
                    </producto>
                </CatProduct>
                """;

        List<ProductDto> products = parser.parseXml(xml);

        assertNotNull(products);
        assertEquals(1, products.size());

        ProductDto p = products.get(0);
        assertEquals("201", p.getId());
        assertEquals("GP-CFE-01", p.getSku());
        assertEquals("Pago de Servicio CFE", p.getName());
        assertEquals(new BigDecimal("12.50"), p.getPrice());
        assertEquals(999, p.getStock());
        assertEquals("Servicios", p.getCategory());
        assertTrue(p.getActive());
    }

    @Test
    @DisplayName("Debe devolver lista vacía si el XML es nulo o vacío")
    void testParseXml_EmptyOrNull() {
        assertTrue(parser.parseXml(null).isEmpty());
        assertTrue(parser.parseXml("").isEmpty());
        assertTrue(parser.parseXml("   ").isEmpty());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el XML está mal formado")
    void testParseXml_MalformedXml_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> parser.parseXml("<invalido>sin cerrar"));
    }
}
