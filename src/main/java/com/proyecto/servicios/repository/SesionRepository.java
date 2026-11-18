package com.proyecto.servicios.repository;

import com.proyecto.servicios.entity.seguridad.Sesion;
import com.proyecto.servicios.enums.EstadoSesion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SesionRepository extends JpaRepository<Sesion, String> {

    List<Sesion> findByUsuarioIdAndEstado(Long usuarioId, EstadoSesion estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sesion s JOIN FETCH s.usuario WHERE s.refreshHash = :hash")
    Optional<Sesion> findByRefreshHashForUpdate(@Param("hash") String refreshHash);

    Optional<Sesion> findByRefreshHashAnterior(String refreshHashAnterior);
}
