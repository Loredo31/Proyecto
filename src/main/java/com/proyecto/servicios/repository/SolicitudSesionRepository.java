package com.proyecto.servicios.repository;

import com.proyecto.servicios.entity.seguridad.SolicitudSesion;
import com.proyecto.servicios.enums.EstadoSolicitudSesion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudSesionRepository extends JpaRepository<SolicitudSesion, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SolicitudSesion s JOIN FETCH s.usuario WHERE s.id = :id")
    Optional<SolicitudSesion> findByIdForUpdate(@Param("id") String id);

    List<SolicitudSesion> findByUsuarioIdAndEstado(Long usuarioId, EstadoSolicitudSesion estado);
}
