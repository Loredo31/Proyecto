package com.proyecto.servicios.repository;

import com.proyecto.servicios.entity.cliente.CuentaBancaria;
import com.proyecto.servicios.enums.EstadoCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
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
}

