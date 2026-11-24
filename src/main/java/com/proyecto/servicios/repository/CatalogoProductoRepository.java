package com.proyecto.servicios.repository;

import com.proyecto.servicios.entity.gestopago.CatalogoProducto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogoProductoRepository extends JpaRepository<CatalogoProducto, Integer> {
}
