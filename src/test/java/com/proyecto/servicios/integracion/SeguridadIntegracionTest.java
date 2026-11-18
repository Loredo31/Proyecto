package com.proyecto.servicios.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.CuentaBancaria;
import com.proyecto.servicios.entity.cliente.InformacionLaboral;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.EstadoCuenta;
import com.proyecto.servicios.enums.Sexo;
import com.proyecto.servicios.repository.ClienteRepository;
import com.proyecto.servicios.repository.CuentaBancariaRepository;
import com.proyecto.servicios.repository.UsuarioRepository;
import com.proyecto.servicios.enums.PropositoCodigo;
import com.proyecto.servicios.service.CorreoService;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración del escenario de evaluación: Spring Security real, filtros, controladores y base de datos.
 * Cada prueba corre en una transacción que se revierte al final, por lo que no deja datos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeguridadIntegracionTest {

    private static final String CLAVE = "Password123!";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private CuentaBancariaRepository cuentaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    /** El correo se simula: se captura el código que se habría enviado. */
    @MockBean
    private CorreoService correoService;

    private String correo;
    private Cliente cliente;
    private CuentaBancaria cuenta;

    @BeforeEach
    void crearCliente() {
        long n = ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L);
        String sufijo = String.valueOf(n).substring(0, 8);
        correo = "it" + n + "@example.com";
        cliente = clienteRepository.save(Cliente.builder()
                .nombre("Prueba").apellidoPaterno("Integracion").apellidoMaterno("Seguridad")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .curp("ITSE" + sufijo + "HDF01").rfc("ITSE" + sufijo + "A")
                .sexo(Sexo.MASCULINO).nacionalidad("MEXICANA").estadoCivil(EstadoCivil.SOLTERO)
                .correoElectronico(correo).telefonoMovil(String.valueOf(n))
                .informacionLaboral(InformacionLaboral.builder().ocupacion("QA").empresa("ACME")
                        .ingresoMensual(new BigDecimal("1000")).build())
                .cuentas(new ArrayList<>())
                .build());
        cuenta = cuentaRepository.save(CuentaBancaria.builder()
                .numeroCuenta("41523" + String.format("%011d", n % 100_000_000_000L))
                .saldo(new BigDecimal("500.00")).estado(EstadoCuenta.ACTIVA).cliente(cliente).build());
        usuarioRepository.save(Usuario.builder().correo(correo).password(passwordEncoder.encode(CLAVE))
                .activo(true).cliente(cliente).build());
    }

    private ResultActions login(String plataforma, String clave) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + correo + "\",\"password\":\"" + clave + "\",\"plataforma\":\""
                        + plataforma + "\",\"dispositivo\":\"Prueba " + plataforma + "\"}"));
    }

    /** Último código que el sistema "envió" por correo para el propósito indicado. */
    private String ultimoCodigo(PropositoCodigo proposito) {
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(correoService, atLeastOnce()).enviarCodigo(eq(correo), eq(proposito), codigo.capture(), anyLong());
        return codigo.getValue();
    }

    private ResultActions verificar(String ticket, String codigo) throws Exception {
        return mvc.perform(post("/auth/2fa/verificar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticket\":\"" + ticket + "\",\"codigo\":\"" + codigo + "\"}"));
    }

    /** Contraseña + código del correo. Devuelve la respuesta del segundo paso (200 tokens o 409). */
    private ResultActions acceder(String plataforma) throws Exception {
        String ticket = cuerpo(login(plataforma, CLAVE).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ApiResponseEnum.CODIGO_ENVIADO.getCode()))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andReturn()).get("data").get("ticket").asText();
        return verificar(ticket, ultimoCodigo(PropositoCodigo.LOGIN));
    }

    private JsonNode cuerpo(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void escenarioSesionUnicaEntreMovilYWeb() throws Exception {
        // Pantalla protegida sin autenticación
        mvc.perform(get("/api/catalog/categorias"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ApiResponseEnum.TOKEN_INVALIDO.getCode()));

        // Login en el móvil: tokens en el cuerpo
        JsonNode movil = cuerpo(acceder("MOVIL").andExpect(status().isOk()).andReturn()).get("data");
        String accessMovil = movil.get("accessToken").asText();
        assertNotNull(movil.get("refreshToken"));

        mvc.perform(post("/clientes/detalle").header(HttpHeaders.AUTHORIZATION, bearer(accessMovil))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"id\":" + cliente.getId() + "}"))
                .andExpect(status().isOk());
        // No puede ver datos de otro cliente ni entrar a funciones de administrador
        mvc.perform(post("/clientes/detalle").header(HttpHeaders.AUTHORIZATION, bearer(accessMovil))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"id\":" + (cliente.getId() + 999) + "}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/usuarios/consulta").header(HttpHeaders.AUTHORIZATION, bearer(accessMovil))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiResponseEnum.SIN_PERMISOS.getCode()));

        // Intento desde la web: se detecta la sesión activa y se pide su cierre
        JsonNode solicitud = cuerpo(acceder("WEB").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ApiResponseEnum.SESION_ACTIVA.getCode()))
                .andExpect(jsonPath("$.data.plataformaSesionActiva").value("MOVIL"))
                .andReturn()).get("data");
        String consulta = "{\"solicitudId\":\"" + solicitud.get("solicitudId").asText()
                + "\",\"secreto\":\"" + solicitud.get("secreto").asText() + "\"}";

        mvc.perform(post("/auth/sesion/solicitud/estado").contentType(MediaType.APPLICATION_JSON).content(consulta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("PENDIENTE"));

        // El móvil aprueba cerrar su sesión
        mvc.perform(post("/auth/sesion/solicitud/responder").header(HttpHeaders.AUTHORIZATION, bearer(accessMovil))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"solicitudId\":\"" + solicitud.get("solicitudId").asText() + "\",\"aprobar\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("APROBADA"));

        // El acceso anterior deja de estar autorizado de inmediato (validado en el servidor)
        mvc.perform(post("/clientes/detalle").header(HttpHeaders.AUTHORIZATION, bearer(accessMovil))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"id\":" + cliente.getId() + "}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ApiResponseEnum.SESION_FINALIZADA.getCode()));

        // La web obtiene sus tokens; el refresh token solo viaja en cookie HttpOnly
        MvcResult web = mvc.perform(post("/auth/sesion/solicitud/estado").contentType(MediaType.APPLICATION_JSON).content(consulta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("CONSUMIDA"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andReturn();
        JsonNode loginWeb = cuerpo(web).get("data").get("login");
        assertFalse(loginWeb.has("refreshToken"));
        Cookie refresh = web.getResponse().getCookie("refresh_token");
        assertNotNull(refresh);

        // La misma solicitud no se puede volver a usar
        mvc.perform(post("/auth/sesion/solicitud/estado").contentType(MediaType.APPLICATION_JSON).content(consulta))
                .andExpect(jsonPath("$.data.estado").value("CONSUMIDA"))
                .andExpect(jsonPath("$.data.login").doesNotExist());

        // Renovación con la cookie y cierre de sesión
        JsonNode renovado = cuerpo(mvc.perform(post("/auth/refresh").cookie(refresh))
                .andExpect(status().isOk()).andReturn()).get("data");
        String accessWeb = renovado.get("accessToken").asText();

        mvc.perform(post("/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer(accessWeb)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
        mvc.perform(post("/cuentas/consulta").header(HttpHeaders.AUTHORIZATION, bearer(accessWeb))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reutilizarUnRefreshTokenRevocaLaSesion() throws Exception {
        JsonNode movil = cuerpo(acceder("MOVIL").andReturn()).get("data");
        String refreshViejo = "{\"refreshToken\":\"" + movil.get("refreshToken").asText() + "\"}";

        JsonNode renovado = cuerpo(mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshViejo))
                .andExpect(status().isOk()).andReturn()).get("data");
        String accessNuevo = renovado.get("accessToken").asText();
        assertNotNull(renovado.get("refreshToken"));

        // Alguien usa el refresh token anterior (posible robo): se revoca la sesión completa
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshViejo))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").exists());
        mvc.perform(post("/cuentas/consulta").header(HttpHeaders.AUTHORIZATION, bearer(accessNuevo))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ApiResponseEnum.SESION_FINALIZADA.getCode()));
    }

    @Test
    void rechazoDeLaSolicitudMantieneLaSesionDelMovil() throws Exception {
        String accessMovil = cuerpo(acceder("MOVIL").andReturn()).get("data").get("accessToken").asText();
        JsonNode solicitud = cuerpo(acceder("WEB").andReturn()).get("data");

        mvc.perform(post("/auth/sesion/solicitud/responder").header(HttpHeaders.AUTHORIZATION, bearer(accessMovil))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"solicitudId\":\"" + solicitud.get("solicitudId").asText() + "\",\"aprobar\":false}"))
                .andExpect(jsonPath("$.data.estado").value("RECHAZADA"));

        mvc.perform(post("/cuentas/consulta").header(HttpHeaders.AUTHORIZATION, bearer(accessMovil))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].numeroCuenta").value(cuenta.getNumeroCuenta()));
    }

    @Test
    void cincoIntentosFallidosBloqueanLaCuentaUnaHora() throws Exception {
        for (int i = 0; i < 4; i++) {
            login("MOVIL", "Incorrecta1!").andExpect(status().isUnauthorized());
        }
        login("MOVIL", "Incorrecta1!").andExpect(status().isLocked());
        // Aun con la contraseña correcta, la cuenta sigue bloqueada
        login("MOVIL", CLAVE).andExpect(status().isLocked());
    }

    @Test
    void accionesQueModificanExigenElPinEnElHeader() throws Exception {
        String access = cuerpo(acceder("MOVIL").andReturn()).get("data").get("accessToken").asText();
        String patch = "{\"id\":" + cliente.getId() + ",\"ocupacion\":\"Desarrollador\"}";

        // Sin PIN configurado todavía
        mvc.perform(patch("/clientes").header(HttpHeaders.AUTHORIZATION, bearer(access))
                        .header("X-Auth-Pin", "1234").contentType(MediaType.APPLICATION_JSON).content(patch))
                .andExpect(status().isConflict());

        // Configurar el PIN por primera vez (no requiere PIN previo)
        mvc.perform(put("/cuentas/pin").header(HttpHeaders.AUTHORIZATION, bearer(access))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroCuenta\":" + cuenta.getNumeroCuenta() + ",\"pinNuevo\":\"1234\"}"))
                .andExpect(status().isOk());
        assertTrue(passwordEncoder.matches("1234", cuentaRepository.findById(cuenta.getId()).orElseThrow().getPinSeguridad()));

        mvc.perform(patch("/clientes").header(HttpHeaders.AUTHORIZATION, bearer(access))
                        .contentType(MediaType.APPLICATION_JSON).content(patch))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/clientes").header(HttpHeaders.AUTHORIZATION, bearer(access))
                        .header("X-Auth-Pin", "9999").contentType(MediaType.APPLICATION_JSON).content(patch))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/clientes").header(HttpHeaders.AUTHORIZATION, bearer(access))
                        .header("X-Auth-Pin", "1234").contentType(MediaType.APPLICATION_JSON).content(patch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.informacionLaboral.ocupacion").value("Desarrollador"));

        // Cambiar el PIN exige el PIN actual
        mvc.perform(put("/cuentas/pin").header(HttpHeaders.AUTHORIZATION, bearer(access))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroCuenta\":" + cuenta.getNumeroCuenta() + ",\"pinNuevo\":\"5678\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tokenAlteradoOExpiradoEsRechazado() throws Exception {
        String access = cuerpo(acceder("MOVIL").andReturn()).get("data").get("accessToken").asText();
        String alterado = access.substring(0, access.length() - 2) + (access.endsWith("A") ? "BB" : "AA");

        mvc.perform(post("/cuentas/consulta").header(HttpHeaders.AUTHORIZATION, bearer(alterado))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ApiResponseEnum.TOKEN_INVALIDO.getCode()));
    }

    @Test
    void segundoFactorIncorrectoYUnSoloUso() throws Exception {
        String ticket = cuerpo(login("MOVIL", CLAVE).andReturn()).get("data").get("ticket").asText();
        String codigo = ultimoCodigo(PropositoCodigo.LOGIN);
        String incorrecto = codigo.equals("000000") ? "111111" : "000000";

        verificar(ticket, incorrecto).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value(containsString("4")));
        verificar(ticket, codigo).andExpect(status().isOk());
        // El mismo código no se puede reutilizar
        verificar(ticket, codigo).andExpect(status().isUnauthorized());
        // Un código con formato inválido no llega a validarse
        verificar(ticket, "12ab").andExpect(status().isBadRequest());
    }

    @Test
    void cincoErroresEntreContrasenaYCodigoBloqueanLaCuenta() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("MOVIL", "Incorrecta1!").andExpect(status().isUnauthorized());
        }
        String ticket = cuerpo(login("MOVIL", CLAVE).andReturn()).get("data").get("ticket").asText();
        String incorrecto = ultimoCodigo(PropositoCodigo.LOGIN).equals("000000") ? "111111" : "000000";
        verificar(ticket, incorrecto).andExpect(status().isUnauthorized());
        verificar(ticket, incorrecto).andExpect(status().isLocked());
        login("MOVIL", CLAVE).andExpect(status().isLocked());
    }

    @Test
    void cierreDeLaOtraSesionConfirmandoPorCorreo() throws Exception {
        String accessMovil = cuerpo(acceder("MOVIL").andReturn()).get("data").get("accessToken").asText();
        JsonNode solicitud = cuerpo(acceder("WEB").andExpect(status().isConflict()).andReturn()).get("data");
        String idYSecreto = "\"solicitudId\":\"" + solicitud.get("solicitudId").asText()
                + "\",\"secreto\":\"" + solicitud.get("secreto").asText() + "\"";

        // El móvil no responde: la web pide el código por correo
        String ticket = cuerpo(mvc.perform(post("/auth/sesion/solicitud/correo").contentType(MediaType.APPLICATION_JSON)
                        .content("{" + idYSecreto + "}"))
                .andExpect(status().isOk()).andReturn()).get("data").get("ticket").asText();
        String codigo = ultimoCodigo(PropositoCodigo.CIERRE_SESION);

        mvc.perform(post("/auth/sesion/solicitud/correo/confirmar").contentType(MediaType.APPLICATION_JSON)
                        .content("{" + idYSecreto + ",\"ticket\":\"" + ticket + "\",\"codigo\":\"" + codigo + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.login.accessToken").exists())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));

        mvc.perform(post("/cuentas/consulta").header(HttpHeaders.AUTHORIZATION, bearer(accessMovil))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ApiResponseEnum.SESION_FINALIZADA.getCode()));
    }

    @Test
    void recuperacionDeContrasenaPorCorreo() throws Exception {
        String accessMovil = cuerpo(acceder("MOVIL").andReturn()).get("data").get("accessToken").asText();

        // Un correo inexistente recibe la misma respuesta
        mvc.perform(post("/auth/recuperacion/solicitar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"nadie" + System.nanoTime() + "@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.ticket").exists());

        String ticket = cuerpo(mvc.perform(post("/auth/recuperacion/solicitar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\"}"))
                .andExpect(status().isOk()).andReturn()).get("data").get("ticket").asText();
        String codigo = ultimoCodigo(PropositoCodigo.RECUPERACION);

        mvc.perform(post("/auth/recuperacion/confirmar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticket\":\"" + ticket + "\",\"codigo\":\"" + codigo + "\",\"nuevaPassword\":\"NuevaClave9$\"}"))
                .andExpect(status().isOk());

        // Se cerraron las sesiones abiertas y la contraseña anterior ya no sirve
        mvc.perform(post("/cuentas/consulta").header(HttpHeaders.AUTHORIZATION, bearer(accessMovil))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        login("MOVIL", CLAVE).andExpect(status().isUnauthorized());
        login("MOVIL", "NuevaClave9$").andExpect(status().isOk());
        // El aviso de "contraseña cambiada" se envía tras el commit; esta prueba revierte su transacción.
    }
}
