package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.cliente.UsuarioResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class UsuarioServiceIntegrationTest {

    @Autowired
    private UsuarioService usuarioService;

    @Test
    public void testConsultarFiltroReal() {
        try {
            List<UsuarioResponseDto> res = usuarioService.consultarConFiltro(null, null);
            System.out.println("TEST SUCCESS: Found " + res.size() + " users.");
            for (UsuarioResponseDto u : res) {
                System.out.println("User: " + u.getCorreo() + ", clienteId=" + u.getClienteId());
            }
        } catch (Exception e) {
            System.err.println("TEST FAILED WITH EXCEPTION:");
            e.printStackTrace();
            throw e;
        }
    }
}

