package com.proyecto.servicios.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Objeto de transferencia que representa un producto del catálogo.
 * Se usa tanto para mapear la respuesta del servicio externo (XML/JSON)
 * como para exponerla en JSON desde la caché local de MongoDB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "product")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProductDto {

    @XmlElement(name = "id")
    private String id;

    @XmlElement(name = "sku")
    private String sku;

    @XmlElement(name = "name")
    private String name;

    @XmlElement(name = "description")
    private String description;

    @XmlElement(name = "price")
    private BigDecimal price;

    @XmlElement(name = "stock")
    private Integer stock;

    @XmlElement(name = "category")
    private String category;

    @XmlElement(name = "active")
    private Boolean active;
}

