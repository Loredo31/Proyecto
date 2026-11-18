package com.proyecto.servicios.repository;

import com.proyecto.servicios.entity.seguridad.CodigoVerificacion;
import com.proyecto.servicios.enums.EstadoCodigo;
import com.proyecto.servicios.enums.PropositoCodigo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CodigoVerificacion c JOIN FETCH c.usuario WHERE c.ticketHash = :ticketHash")
    Optional<CodigoVerificacion> findByTicketHashForUpdate(@Param("ticketHash") String ticketHash);

    List<CodigoVerificacion> findByUsuarioIdAndPropositoAndEstado(Long usuarioId, PropositoCodigo proposito,
                                                                  EstadoCodigo estado);

    long countByUsuarioIdAndPropositoAndFechaCreacionAfter(Long usuarioId, PropositoCodigo proposito,
                                                          LocalDateTime desde);
}
