package com.proyecto.servicios.service;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.BusinessException;

/**
 * Valida el PIN de seguridad (header {@code X-Auth-Pin}) antes de una acción que modifica información.
 * El PIN se guarda con BCrypt; los intentos fallidos se cuentan y la cuenta se bloquea al llegar al máximo.
 */
public interface PinSeguridadService {

    String HEADER = "X-Auth-Pin";
    int MIN_DIGITOS = 4;
    int MAX_DIGITOS = 6;

    /**
     * @param numeroCuenta cuenta sobre la que se opera; si es nula se usa la cuenta activa principal del cliente
     */
    void validar(Long clienteId, String pin, String numeroCuenta);

    /** Para cambiar el PIN: solo exige el PIN actual si la cuenta ya tiene uno configurado. */
    void validarSiConfigurado(Long clienteId, String pin, String numeroCuenta);

    /** Verifica que el PIN venga y tenga entre 4 y 6 dígitos. */
    static void exigirFormato(String pin) {
        if (pin == null || pin.isBlank()) {
            throw new BusinessException(ApiResponseEnum.PIN_REQUERIDO);
        }
        if (pin.length() < MIN_DIGITOS || pin.length() > MAX_DIGITOS || !pin.chars().allMatch(Character::isDigit)) {
            throw new BusinessException(ApiResponseEnum.DATOS_INVALIDOS, "El PIN debe tener entre 4 y 6 dígitos");
        }
    }
}
