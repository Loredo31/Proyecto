package com.proyecto.servicios.exception;

import com.proyecto.servicios.dto.ApiResponse;
import com.proyecto.servicios.enums.ApiResponseEnum;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

/**
 * Manejador global de excepciones.
 * Estandariza todas las respuestas de error usando {@link ApiResponseEnum}.
 * Garantiza que no se expongan datos sensibles en las respuestas ni en los logs.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CatalogException.class)
    public ResponseEntity<ApiResponse<Void>> handleCatalogException(CatalogException ex) {
        log.warn("Excepción de catálogo: {}", ex.getMessage());
        ApiResponseEnum status = ex.getResponseStatus();
        return ResponseEntity.status(status.getHttpStatus()).body(ApiResponse.error(status));
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalServiceException(ExternalServiceException ex) {
        log.error("Error en servicio externo de catálogo [código HTTP: {}]: {}", ex.getHttpStatus(), ex.getMessage());
        ApiResponseEnum status = ex.toResponseStatus();
        return ResponseEntity.status(status.getHttpStatus()).body(ApiResponse.error(status));
    }

    @ExceptionHandler(RetryableException.class)
    public ResponseEntity<ApiResponse<Void>> handleRetryableException(RetryableException ex) {
        log.error("Timeout de conexión o lectura en cliente Feign al consultar el catálogo externo");
        ApiResponseEnum status = ApiResponseEnum.TIMEOUT_SERVICIO;
        return ResponseEntity.status(status.getHttpStatus()).body(ApiResponse.error(status));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignException(FeignException ex) {
        log.error("Error de comunicación Feign con el servicio externo de catálogo [status: {}]", ex.status());
        ApiResponseEnum status = (ex.status() == 401 || ex.status() == 403)
                ? ApiResponseEnum.ERROR_AUTENTICACION
                : ApiResponseEnum.SERVICIO_EXTERNO_NO_DISPONIBLE;
        return ResponseEntity.status(status.getHttpStatus()).body(ApiResponse.error(status));
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleTimeout(ResourceAccessException ex) {
        log.error("Timeout consultando el servicio externo de catálogo: {}", ex.getMessage());
        ApiResponseEnum status = ApiResponseEnum.TIMEOUT_SERVICIO;
        return ResponseEntity.status(status.getHttpStatus()).body(ApiResponse.error(status));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Error interno inesperado: {}", ex.getMessage(), ex);
        ApiResponseEnum status = ApiResponseEnum.ERROR_INTERNO;
        return ResponseEntity.status(status.getHttpStatus()).body(ApiResponse.error(status));
    }
}

