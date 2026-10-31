package com.proyecto.servicios.repository;

import com.proyecto.servicios.entity.cliente.CuentaBancaria;
import com.proyecto.servicios.enums.EstadoCuenta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaBancariaRepository extends JpaRepository<CuentaBancaria, Long> {

    boolean existsByNumeroCuenta(String numeroCuenta);

    Optional<CuentaBancaria> findByNumeroCuenta(String numeroCuenta);

    List<CuentaBancaria> findByClienteId(Long clienteId);

    List<CuentaBancaria> findByEstado(EstadoCuenta estado);

    List<CuentaBancaria> findByEstadoAndClienteEliminadoLogicoFalse(EstadoCuenta estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CuentaBancaria c WHERE c.numeroCuenta = :numeroCuenta")
    Optional<CuentaBancaria> findByNumeroCuentaForUpdate(@Param("numeroCuenta") String numeroCuenta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CuentaBancaria c WHERE c.id = :id")
    Optional<CuentaBancaria> findByIdForUpdate(@Param("id") Long id);
}

