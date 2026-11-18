package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.PaginaDto;
import com.proyecto.servicios.dto.ProductDto;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.util.ImagenesGestoPago;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Consulta del catálogo para web y móvil: filtra, busca y pagina sobre la caché sincronizada desde GestoPago
 * (GestoPago no ofrece paginación ni búsqueda), y agrega las URLs de imágenes publicadas por el proveedor.
 */
@Service
@RequiredArgsConstructor
public class ConsultaCatalogoService {

    private final ProductCatalogService productCatalogService;

    public PaginaDto<ProductDto> consultar(FiltroCatalogo filtro) {
        List<ProductDto> filtrados = productCatalogService.getCatalogFromCache().stream()
                .filter(criterios(filtro))
                .map(ConsultaCatalogoService::conImagenes)
                .sorted(Comparator.comparing(ProductDto::getCategory, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(ProductDto::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        return PaginaDto.deLista(filtrados, filtro.pagina(), filtro.tamano());
    }

    public ProductDto detalle(String idProducto) {
        return productCatalogService.getCatalogFromCache().stream()
                .filter(p -> Objects.equals(p.getId(), idProducto))
                .findFirst()
                .map(ConsultaCatalogoService::conImagenes)
                .orElseThrow(() -> new BusinessException(ApiResponseEnum.PRODUCTO_INVALIDO, "El producto no existe en el catálogo"));
    }

    public List<String> categorias() {
        return productCatalogService.getCatalogFromCache().stream()
                .map(ProductDto::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private static Predicate<ProductDto> criterios(FiltroCatalogo filtro) {
        Predicate<ProductDto> criterio = p -> true;
        if (filtro.soloActivos()) {
            criterio = criterio.and(p -> !Boolean.FALSE.equals(p.getActive()));
        }
        if (filtro.categoria() != null && !filtro.categoria().isBlank()) {
            criterio = criterio.and(p -> filtro.categoria().equalsIgnoreCase(p.getCategory()));
        }
        if (filtro.idServicio() != null) {
            criterio = criterio.and(p -> filtro.idServicio().equals(p.getIdServicio()));
        }
        if (filtro.busqueda() != null && !filtro.busqueda().isBlank()) {
            String buscado = normalizar(filtro.busqueda());
            criterio = criterio.and(p -> normalizar(p.getName()).contains(buscado)
                    || normalizar(p.getDescription()).contains(buscado)
                    || normalizar(p.getCategory()).contains(buscado));
        }
        return criterio;
    }

    /** Minúsculas y sin acentos, para que "telefonia" encuentre "Telefonía". */
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private static ProductDto conImagenes(ProductDto original) {
        return original.toBuilder()
                .logoUrl(ImagenesGestoPago.logoServicio(original.getIdServicio()))
                .imagenReferenciaUrl(ImagenesGestoPago.imagenReferencia(original.getIdServicio()))
                .build();
    }

    public record FiltroCatalogo(int pagina, int tamano, String categoria, String busqueda, Integer idServicio,
                                 boolean soloActivos) {
    }
}
