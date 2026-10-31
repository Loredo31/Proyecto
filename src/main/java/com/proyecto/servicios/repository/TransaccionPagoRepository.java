package com.proyecto.servicios.repository;

import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.enums.EstadoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransaccionPagoRepository extends JpaRepository<TransaccionPago, Long> {

    Optional<TransaccionPago> findByFolio(String folio);

    Optional<TransaccionPago> findByIdempotencyKey(String idempotencyKey);

    boolean existsByCuentaIdAndReferenciaAndIdProductoAndMontoAndEstadoInAndFechaCreacionAfter(
            Long cuentaId, String referencia, Integer idProducto, BigDecimal monto,
            Collection<EstadoTransaccion> estados, LocalDateTime desde);

    List<TransaccionPago> findByEstadoAndFechaActualizacionBefore(EstadoTransaccion estado, LocalDateTime limite);

    List<TransaccionPago> findByEstadoAndFechaCreacionBetween(EstadoTransaccion estado, LocalDateTime desde,
                                                              LocalDateTime hasta);

    long countByEstadoAndFechaCreacionBefore(EstadoTransaccion estado, LocalDateTime limite);
}
