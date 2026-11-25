package com.proyecto.servicios.dto.cliente;

import com.proyecto.servicios.enums.EstadoRepublica;

import java.util.List;

/**
 * Datos de un código postal según el Catálogo Nacional de Códigos Postales (SEPOMEX).
 * Se usa para llenar automáticamente estado, municipio y colonia en el registro y en Mi perfil.
 *
 * @param codigoPostal código de 5 dígitos
 * @param estado       estado (mismos valores que acepta `domicilio.estado`)
 * @param municipio    municipio o alcaldía
 * @param colonias     colonias (asentamientos) de ese código, en orden alfabético
 */
public record CodigoPostalDto(String codigoPostal, EstadoRepublica estado, String municipio, List<String> colonias) {
}
