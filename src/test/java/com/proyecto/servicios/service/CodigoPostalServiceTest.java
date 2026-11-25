package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.cliente.CodigoPostalDto;
import com.proyecto.servicios.enums.EstadoRepublica;
import com.proyecto.servicios.enums.RangoIngreso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodigoPostalServiceTest {

    private final CodigoPostalService service = new CodigoPostalService();

    @Test
    void buscar_LlenaEstadoMunicipioYColonias() {
        CodigoPostalDto cp = service.buscar("37800").orElseThrow();

        assertEquals(EstadoRepublica.GUANAJUATO, cp.estado());
        assertEquals("Dolores Hidalgo Cuna de la Independencia Nacional", cp.municipio());
        assertTrue(cp.colonias().contains("Centro Dolores Hidalgo"));
    }

    @Test
    void buscar_CodigoConVariasColonias() {
        CodigoPostalDto cp = service.buscar("01000").orElseThrow();

        assertEquals(EstadoRepublica.CIUDAD_DE_MEXICO, cp.estado());
        assertEquals("Álvaro Obregón", cp.municipio());
        assertTrue(cp.colonias().contains("San Ángel"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "abcde", "999999", "00000"})
    void buscar_CodigoInvalidoOInexistente_Vacio(String cp) {
        assertTrue(service.buscar(cp).isEmpty());
    }

    @Test
    void buscar_Null_Vacio() {
        assertTrue(service.buscar(null).isEmpty());
    }

    @Test
    void catalogoInexistente_Falla() {
        CodigoPostalService sinArchivo = new CodigoPostalService("/sepomex/no-existe.gz");
        assertThrows(IllegalStateException.class, () -> sinArchivo.buscar("37800"));
    }

    @Test
    void rangoIngreso_DesdeIngresoExacto() {
        assertNull(RangoIngreso.desde(null));
        assertEquals(RangoIngreso.HASTA_5000, RangoIngreso.desde(new BigDecimal("5000")));
        assertEquals(RangoIngreso.DE_5000_A_10000, RangoIngreso.desde(new BigDecimal("5000.01")));
        assertEquals(RangoIngreso.DE_10000_A_20000, RangoIngreso.desde(new BigDecimal("15000")));
        assertEquals(RangoIngreso.DE_20000_A_30000, RangoIngreso.desde(new BigDecimal("30000")));
        assertEquals(RangoIngreso.MAS_DE_30000, RangoIngreso.desde(new BigDecimal("25000000")));
        assertEquals("30,000 o más", RangoIngreso.MAS_DE_30000.getTexto());
    }
}
