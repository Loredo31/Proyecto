package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.exception.CatalogException;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ContrasenaInvalidaException;
import com.proyecto.servicios.exception.CorreoElectronicoDuplicadoException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.ExternalServiceException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.model.GenericResponse;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.converter.HttpMessageNotReadableException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ValidadorCampos {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse<Map<String, String>>> dtoValidation(MethodArgumentNotValidException ex) {
        GenericResponse<Map<String, String>> genericResponse = new GenericResponse<>();
        Map<String, String> listError = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            listError.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        genericResponse.setCodigo(1);
        genericResponse.setMensaje("Datos no validos: " + listError);
        genericResponse.setDatos(listError);
        log.warn("Error de validación DTO: {}", listError);
        return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<GenericResponse<Void>> handleUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        log.warn("Usuario no encontrado: {}", ex.getMessage());
        return new ResponseEntity<>(
                GenericResponse.error(HttpStatus.NOT_FOUND.value(), ex.getMessage()),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<GenericResponse<Void>> handleClienteNoEncontrado(ClienteNoEncontradoException ex) {
        log.warn("Cliente no encontrado: {}", ex.getMessage());
        return new ResponseEntity<>(
                GenericResponse.error(HttpStatus.NOT_FOUND.value(), ex.getMessage()),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(CorreoElectronicoDuplicadoException.class)
    public ResponseEntity<GenericResponse<Void>> handleCorreoDuplicado(CorreoElectronicoDuplicadoException ex) {
        log.warn("Correo electrónico duplicado: {}", ex.getMessage());
        return new ResponseEntity<>(
                GenericResponse.error(HttpStatus.CONFLICT.value(), ex.getMessage()),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(CurpDuplicadaException.class)
    public ResponseEntity<GenericResponse<Void>> handleCurpDuplicada(CurpDuplicadaException ex) {
        log.warn("CURP duplicada: {}", ex.getMessage());
        return new ResponseEntity<>(
                GenericResponse.error(HttpStatus.CONFLICT.value(), ex.getMessage()),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(RfcDuplicadoException.class)
    public ResponseEntity<GenericResponse<Void>> handleRfcDuplicado(RfcDuplicadoException ex) {
        log.warn("RFC duplicado: {}", ex.getMessage());
        return new ResponseEntity<>(
                GenericResponse.error(HttpStatus.CONFLICT.value(), ex.getMessage()),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(ContrasenaInvalidaException.class)
    public ResponseEntity<GenericResponse<Void>> handleContrasenaInvalida(ContrasenaInvalidaException ex) {
        log.warn("Contraseña inválida: {}", ex.getMessage());
        return new ResponseEntity<>(
                GenericResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(ValidacionNegocioException.class)
    public ResponseEntity<GenericResponse<Void>> handleValidacionNegocio(ValidacionNegocioException ex) {
        log.warn("Validación de negocio: {}", ex.getMessage());
        return new ResponseEntity<>(
                GenericResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<GenericResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Excepción de negocio: {}", ex.getMessage());
        HttpStatus status = ex.getResponseStatus() != null ? ex.getResponseStatus().getHttpStatus() : HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(
                GenericResponse.error(status.value(), ex.getMessage()),
                status
        );
    }

    @ExceptionHandler(CatalogException.class)
    public ResponseEntity<GenericResponse<Void>> handleCatalogException(CatalogException ex) {
        log.warn("Excepción de catálogo: {}", ex.getMessage());
        ApiResponseEnum status = ex.getResponseStatus();
        return new ResponseEntity<>(
                GenericResponse.error(status.getHttpStatus().value(), status.getMessage()),
                status.getHttpStatus()
        );
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<GenericResponse<Void>> handleExternalServiceException(ExternalServiceException ex) {
        log.error("Error en servicio externo de catálogo [código HTTP: {}]: {}", ex.getHttpStatus(), ex.getMessage());
        ApiResponseEnum status = ex.toResponseStatus();
        return new ResponseEntity<>(
                GenericResponse.error(status.getHttpStatus().value(), status.getMessage()),
                status.getHttpStatus()
        );
    }

    @ExceptionHandler(RetryableException.class)
    public ResponseEntity<GenericResponse<Void>> handleRetryableException(RetryableException ex) {
        log.error("Timeout en cliente Feign al consultar el catálogo externo");
        return new ResponseEntity<>(
                GenericResponse.error(HttpStatus.GATEWAY_TIMEOUT.value(), "Tiempo de espera agotado al consultar el servicio externo"),
                HttpStatus.GATEWAY_TIMEOUT
        );
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<GenericResponse<Void>> handleFeignException(FeignException ex) {
        log.error("Error de comunicación Feign con el servicio externo [status: {}]", ex.status());
        HttpStatus status = (ex.status() == 401 || ex.status() == 403) ? HttpStatus.UNAUTHORIZED : HttpStatus.SERVICE_UNAVAILABLE;
        return new ResponseEntity<>(
                GenericResponse.error(status.value(), "Error de comunicación con el servicio externo"),
                status
        );
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<GenericResponse<Void>> handleTimeout(ResourceAccessException ex) {
        log.error("Timeout consultando el servicio externo: {}", ex.getMessage());
        return new ResponseEntity<>(
                GenericResponse.error(HttpStatus.GATEWAY_TIMEOUT.value(), "Tiempo de espera agotado al consultar el servicio externo"),
                HttpStatus.GATEWAY_TIMEOUT
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Cuerpo de la petición JSON inválido o mal formado: {}", ex.getMessage());
        return new ResponseEntity<>(
                GenericResponse.error(HttpStatus.BAD_REQUEST.value(), "El cuerpo de la solicitud (JSON) es inválido o está mal formado"),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<GenericResponse<Void>> handleUsuarioInactivo(UsuarioInactivoException ex) {
        log.warn("Usuario inactivo: {}", ex.getMessage());
        return new ResponseEntity<>(
                GenericResponse.error(HttpStatus.FORBIDDEN.value(), ex.getMessage()),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Error no controlado: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(
                GenericResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error interno en el servidor al procesar la solicitud"),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
