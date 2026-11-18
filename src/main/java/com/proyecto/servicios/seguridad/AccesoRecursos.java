package com.proyecto.servicios.seguridad;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.repository.CuentaBancariaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Garantiza que un cliente solo consulte o modifique su propia información. */
@Component
@RequiredArgsConstructor
public class AccesoRecursos {

    private final CuentaBancariaRepository cuentaRepository;

    public UsuarioAutenticado actual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return usuario;
        }
        throw new BusinessException(ApiResponseEnum.TOKEN_INVALIDO);
    }

    public Long clienteActual() {
        return actual().clienteId();
    }

    public void validarCliente(Long clienteId) {
        if (clienteId == null || !clienteId.equals(clienteActual())) {
            throw new BusinessException(ApiResponseEnum.SIN_PERMISOS);
        }
    }

    @Transactional(readOnly = true)
    public void validarCuenta(String numeroCuenta) {
        Long clienteId = clienteActual();
        boolean esPropia = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .map(c -> c.getCliente() != null && c.getCliente().getId().equals(clienteId))
                .orElse(false);
        if (!esPropia) {
            // Mismo mensaje que "no encontrada" para no revelar que la cuenta existe.
            throw new CuentaNoEncontradaException("Cuenta no encontrada");
        }
    }
}
