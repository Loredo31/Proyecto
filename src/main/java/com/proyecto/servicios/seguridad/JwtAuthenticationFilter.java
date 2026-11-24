package com.proyecto.servicios.seguridad;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.service.SesionService;
import com.proyecto.servicios.util.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Autentica cada petición con el access token del header {@code Authorization: Bearer ...}.
 * Además de la firma y expiración del JWT, exige que la sesión siga ACTIVA en el servidor:
 * así una sesión cerrada desde otro dispositivo deja de ser válida de inmediato.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Atributo de la petición con el motivo del rechazo, leído por {@link RespuestaSeguridadHandler}. */
    public static final String ATRIBUTO_ERROR = "seguridad.error";
    private static final String BEARER = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final SesionService sesionService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)) {
            Optional<UsuarioAutenticado> usuario = jwtTokenService.validar(header.substring(BEARER.length()));
            if (usuario.isEmpty()) {
                request.setAttribute(ATRIBUTO_ERROR, ApiResponseEnum.TOKEN_INVALIDO);
            } else if (!sesionService.registrarActividad(usuario.get().sesionId())) {
                request.setAttribute(ATRIBUTO_ERROR, ApiResponseEnum.SESION_FINALIZADA);
            } else {
                autenticar(usuario.get(), request);
            }
        }
        chain.doFilter(request, response);
    }

    private static void autenticar(UsuarioAutenticado usuario, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken autenticacion = new UsernamePasswordAuthenticationToken(
                usuario, null, List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));
        autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }
}
