package com.proyecto.servicios.config;

import com.proyecto.servicios.seguridad.JwtAuthenticationFilter;
import com.proyecto.servicios.seguridad.RespuestaSeguridadHandler;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

/**
 * Configuración de Spring Security.
 * <ul>
 *   <li>API sin estado: no hay sesión HTTP; cada petición se autentica con el access token (JWT).</li>
 *   <li>La sesión única se controla en la tabla {@code sesiones} (ver {@link JwtAuthenticationFilter}).</li>
 *   <li>CSRF deshabilitado: la API no usa cookies para autenticar peticiones de negocio (Bearer token) y solo
 *       acepta JSON, lo que obliga al navegador a una verificación CORS previa. La única cookie (refresh token)
 *       es HttpOnly y solo sirve en {@code /auth}.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Evita que Spring Boot cree un usuario en memoria con contraseña generada. */
    @Bean
    public InMemoryUserDetailsManager sinUsuariosEnMemoria() {
        return new InMemoryUserDetailsManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
                                                   RespuestaSeguridadHandler respuestas,
                                                   CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h
                        .contentTypeOptions(c -> { })
                        .frameOptions(f -> f.deny())
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)))
                .exceptionHandling(e -> e.authenticationEntryPoint(respuestas).accessDeniedHandler(respuestas))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Públicos
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/2fa/verificar", "/auth/codigo/reenviar",
                                "/auth/refresh", "/auth/sesion/solicitud/estado", "/auth/sesion/solicitud/correo",
                                "/auth/sesion/solicitud/correo/confirmar", "/auth/recuperacion/solicitar",
                                "/auth/recuperacion/confirmar").permitAll()
                        .requestMatchers(HttpMethod.POST, "/clientes/onboarding", "/clientes/correo/disponible").permitAll()
                        .requestMatchers(HttpMethod.GET, "/clientes/codigo-postal/*").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info", "/error").permitAll()
                        // Funciones de administración: deshabilitadas mientras no exista el rol de administrador
                        .requestMatchers("/usuarios/**", "/personas", "/personasActualiza", "/personasElimina").denyAll()
                        .requestMatchers("/api/catalog/sync").denyAll()
                        .requestMatchers(HttpMethod.POST, "/clientes/consulta", "/clientes/curp", "/clientes/rfc", "/clientes/cuenta").denyAll()
                        .requestMatchers(HttpMethod.POST, "/cuentas").denyAll()
                        .requestMatchers(HttpMethod.PATCH, "/cuentas").denyAll()
                        // Cliente autenticado (solo su propia información)
                        .requestMatchers("/auth/**", "/api/catalog/**", "/api/pagos/**", "/clientes/**", "/cuentas/**").authenticated()
                        .anyRequest().denyAll())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** El filtro JWT solo debe ejecutarse dentro de la cadena de Spring Security, no como filtro global. */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> registroFiltroJwt(JwtAuthenticationFilter filtro) {
        FilterRegistrationBean<JwtAuthenticationFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(SeguridadProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
