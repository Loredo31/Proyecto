package com.proyecto.servicios.repository;

import com.proyecto.servicios.entity.cliente.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByCorreo(String correo);

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByClienteId(Long clienteId);

    List<Usuario> findByCorreoContainingIgnoreCase(String correo);

    List<Usuario> findByActivo(Boolean activo);

    List<Usuario> findByCorreoContainingIgnoreCaseAndActivo(String correo, Boolean activo);
}

