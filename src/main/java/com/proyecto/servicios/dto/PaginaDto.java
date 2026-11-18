package com.proyecto.servicios.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Página de resultados con la información necesaria para paginar en web y móvil. */
public record PaginaDto<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas,
                           boolean esUltima) {

    public static <E, T> PaginaDto<T> de(Page<E> page, Function<E, T> mapeo) {
        return new PaginaDto<>(page.getContent().stream().map(mapeo).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    /** Pagina una lista ya cargada en memoria (p. ej. el catálogo en caché). */
    public static <T> PaginaDto<T> deLista(List<T> elementos, int pagina, int tamano) {
        int total = elementos.size();
        int desde = Math.min(pagina * tamano, total);
        int hasta = Math.min(desde + tamano, total);
        int totalPaginas = tamano == 0 ? 0 : (int) Math.ceil((double) total / tamano);
        return new PaginaDto<>(List.copyOf(elementos.subList(desde, hasta)), pagina, tamano, total, totalPaginas,
                pagina >= totalPaginas - 1);
    }
}
