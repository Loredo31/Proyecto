package com.proyecto.servicios.repository;

import com.proyecto.servicios.entity.cliente.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByCurp(String curp);

    boolean existsByRfc(String rfc);

    boolean existsByTelefonoMovil(String telefonoMovil);

    boolean existsByCorreoElectronico(String correoElectronico);

    boolean existsByTelefonoMovilAndIdNot(String telefonoMovil, Long id);

    boolean existsByCorreoElectronicoAndIdNot(String correoElectronico, Long id);

    List<Cliente> findAllByEliminadoLogicoFalse();

    Optional<Cliente> findByIdAndEliminadoLogicoFalse(Long id);

    Optional<Cliente> findByCurpAndEliminadoLogicoFalse(String curp);

    Optional<Cliente> findByRfcAndEliminadoLogicoFalse(String rfc);

    Optional<Cliente> findByCorreoElectronicoAndEliminadoLogicoFalse(String correoElectronico);

    List<Cliente> findByNombreContainingIgnoreCaseAndEliminadoLogicoFalse(String nombre);

    List<Cliente> findByApellidoPaternoContainingIgnoreCaseAndEliminadoLogicoFalse(String apellidoPaterno);

    List<Cliente> findByApellidoMaternoContainingIgnoreCaseAndEliminadoLogicoFalse(String apellidoMaterno);

    List<Cliente> findByActivoLoginTrueAndEliminadoLogicoFalse();

    List<Cliente> findByFechaCreacionBetweenAndEliminadoLogicoFalse(LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT c FROM Cliente c JOIN c.cuentas cb WHERE cb.numeroCuenta = :numeroCuenta AND c.eliminadoLogico = false")
    Optional<Cliente> findByNumeroCuentaAndEliminadoLogicoFalse(@Param("numeroCuenta") String numeroCuenta);
}

