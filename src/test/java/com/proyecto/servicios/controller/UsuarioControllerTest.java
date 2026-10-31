package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.dto.cliente.UsuarioResponseDto;
import com.proyecto.servicios.dto.usuario.UsuarioActualizacionRequest;
import com.proyecto.servicios.dto.usuario.UsuarioBajaRequest;
import com.proyecto.servicios.dto.usuario.UsuarioConsultaIdRequest;
import com.proyecto.servicios.dto.usuario.UsuarioCreacionRequest;
import com.proyecto.servicios.dto.usuario.UsuarioFiltroRequest;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private com.proyecto.servicios.config.JwtAuthInterceptor jwtAuthInterceptor;

    private UsuarioResponseDto usuarioResponse;

    @BeforeEach
    void setUp() throws Exception {
        when(jwtAuthInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        usuarioResponse = UsuarioResponseDto.builder()
                .id(1L)
                .clienteId(10L)
                .correo("admin@empresa.com")
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
    }

    @Test
    void testCrearUsuario_Exito() throws Exception {
        when(usuarioService.crearUsuario(any(UsuarioCreacionRequest.class))).thenReturn(usuarioResponse);

        UsuarioCreacionRequest request = UsuarioCreacionRequest.builder()
                .clienteId(10L)
                .correo("admin@empresa.com")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/usuarios/crear")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(0))
                .andExpect(jsonPath("$.mensaje").value("Usuario creado exitosamente"))
                .andExpect(jsonPath("$.datos.correo").value("admin@empresa.com"));
    }

    @Test
    void testCrearUsuario_ValidacionFallida_ManejadaPorValidadorCampos() throws Exception {
        UsuarioCreacionRequest requestInvalido = UsuarioCreacionRequest.builder()
                .clienteId(null)
                .correo("correo-invalido")
                .password("123")
                .build();

        mockMvc.perform(post("/usuarios/crear")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(1))
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.datos").exists());
    }

    @Test
    void testConsultarUsuarios_FiltrosCombinados_EnRequestBody() throws Exception {
        when(usuarioService.consultarUsuarios(any(UsuarioFiltroRequest.class)))
                .thenReturn(Collections.singletonList(usuarioResponse));

        UsuarioFiltroRequest filtro = UsuarioFiltroRequest.builder()
                .correo("admin@empresa.com")
                .activo(true)
                .build();

        mockMvc.perform(post("/usuarios/consulta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filtro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(0))
                .andExpect(jsonPath("$.datos[0].correo").value("admin@empresa.com"));
    }

    @Test
    void testConsultarPorId_EnRequestBody_SinPathVariable() throws Exception {
        when(usuarioService.consultarPorId(any(UsuarioConsultaIdRequest.class))).thenReturn(usuarioResponse);

        UsuarioConsultaIdRequest idRequest = UsuarioConsultaIdRequest.builder()
                .id(1L)
                .build();

        mockMvc.perform(post("/usuarios/detalle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(idRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(0))
                .andExpect(jsonPath("$.datos.id").value(1L));
    }

    @Test
    void testConsultarPorId_NoEncontrado() throws Exception {
        when(usuarioService.consultarPorId(any(UsuarioConsultaIdRequest.class)))
                .thenThrow(new UsuarioNoEncontradoException("Usuario no encontrado con ID: 99"));

        UsuarioConsultaIdRequest idRequest = UsuarioConsultaIdRequest.builder()
                .id(99L)
                .build();

        mockMvc.perform(post("/usuarios/detalle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(idRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value(404))
                .andExpect(jsonPath("$.mensaje").value("Usuario no encontrado con ID: 99"));
    }

    @Test
    void testActualizarUsuario_Exito() throws Exception {
        when(usuarioService.actualizarUsuario(any(UsuarioActualizacionRequest.class))).thenReturn(usuarioResponse);

        UsuarioActualizacionRequest request = UsuarioActualizacionRequest.builder()
                .id(1L)
                .correo("nuevo@empresa.com")
                .activo(true)
                .build();

        mockMvc.perform(put("/usuarios/actualizar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(0))
                .andExpect(jsonPath("$.mensaje").value("Usuario actualizado exitosamente"));
    }

    @Test
    void testBajaUsuario_Exito() throws Exception {
        usuarioResponse.setActivo(false);
        when(usuarioService.bajaUsuario(any(UsuarioBajaRequest.class))).thenReturn(usuarioResponse);

        UsuarioBajaRequest request = UsuarioBajaRequest.builder()
                .id(1L)
                .build();

        mockMvc.perform(put("/usuarios/baja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(0))
                .andExpect(jsonPath("$.datos.activo").value(false));
    }
}
