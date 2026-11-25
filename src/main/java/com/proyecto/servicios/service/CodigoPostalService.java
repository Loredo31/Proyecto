package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.cliente.CodigoPostalDto;
import com.proyecto.servicios.enums.EstadoRepublica;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

/**
 * Consulta de códigos postales con el Catálogo Nacional de Códigos Postales de SEPOMEX (Correos de México).
 * <p>
 * El catálogo viene en {@code resources/sepomex/codigos_postales.txt.gz} (una línea por código:
 * {@code cp|ESTADO|municipio|colonia1;colonia2;...}) y se carga en memoria la primera vez que se consulta.
 * Para actualizarlo se descarga de nuevo en correosdemexico.gob.mx y se regenera ese archivo.
 */
@Slf4j
@Service
public class CodigoPostalService {

    static final String RECURSO = "/sepomex/codigos_postales.txt.gz";

    private final String recurso;
    private volatile Map<String, CodigoPostalDto> catalogo;

    public CodigoPostalService() {
        this(RECURSO);
    }

    CodigoPostalService(String recurso) {
        this.recurso = recurso;
    }

    /** Datos del código postal, o vacío si no tiene 5 dígitos o no existe en el catálogo. */
    public Optional<CodigoPostalDto> buscar(String codigoPostal) {
        if (codigoPostal == null || !codigoPostal.trim().matches("\\d{5}")) {
            return Optional.empty();
        }
        return Optional.ofNullable(catalogo().get(codigoPostal.trim()));
    }

    private Map<String, CodigoPostalDto> catalogo() {
        Map<String, CodigoPostalDto> actual = catalogo;
        if (actual == null) {
            synchronized (this) {
                actual = catalogo;
                if (actual == null) {
                    actual = cargar();
                    catalogo = actual;
                }
            }
        }
        return actual;
    }

    private Map<String, CodigoPostalDto> cargar() {
        InputStream archivo = getClass().getResourceAsStream(recurso);
        if (archivo == null) {
            throw new IllegalStateException("No se encontró el catálogo de códigos postales: " + recurso);
        }
        Map<String, CodigoPostalDto> mapa = new HashMap<>(40_000);
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(new GZIPInputStream(archivo), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.isBlank() || linea.startsWith("#")) {
                    continue;
                }
                String[] partes = linea.split("\\|", 4);
                if (partes.length < 4) {
                    continue;
                }
                List<String> colonias = Arrays.stream(partes[3].split(";")).filter(c -> !c.isBlank()).toList();
                mapa.put(partes[0], new CodigoPostalDto(partes[0], EstadoRepublica.valueOf(partes[1]), partes[2], colonias));
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo leer el catálogo de códigos postales", ex);
        }
        log.info("Catálogo de códigos postales cargado: {} códigos", mapa.size());
        return Map.copyOf(mapa);
    }
}
