package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = PagoController.class)
public class PagoExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex) {
        ApiResponseEnum estado = ex.getResponseStatus() != null ? ex.getResponseStatus() : ApiResponseEnum.DATOS_INVALIDOS;
        log.warn("Pago rechazado por regla de negocio [{}]: {}", estado.getCode(), ex.getMessage());
        return problema(estado.getHttpStatus(), estado, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(e -> errores.putIfAbsent(e.getObjectName(), e.getDefaultMessage()));
        log.warn("Petición de pago inválida: {}", errores.keySet());

        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, ApiResponseEnum.DATOS_INVALIDOS,
                ApiResponseEnum.DATOS_INVALIDOS.getMessage());
        problema.setProperty("errores", errores);
        return problema;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingRequestHeaderException.class})
    public ProblemDetail handlePeticionMalFormada(Exception ex) {
        log.warn("Petición de pago mal formada: {}", ex.getClass().getSimpleName());
        return problema(HttpStatus.BAD_REQUEST, ApiResponseEnum.DATOS_INVALIDOS,
                "La solicitud está mal formada o le faltan encabezados obligatorios");
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    public ProblemDetail handleConcurrencia(ConcurrencyFailureException ex) {
        log.warn("Conflicto de concurrencia en pago: {}", ex.getClass().getSimpleName());
        return problema(HttpStatus.CONFLICT, ApiResponseEnum.OPERACION_CONCURRENTE,
                ApiResponseEnum.OPERACION_CONCURRENTE.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleInesperado(Exception ex) {
        log.error("Error no controlado en el módulo de pagos", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, ApiResponseEnum.ERROR_INTERNO,
                ApiResponseEnum.ERROR_INTERNO.getMessage());
    }

    private static ProblemDetail problema(HttpStatus status, ApiResponseEnum codigo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setTitle(status.getReasonPhrase());
        problema.setProperty("code", codigo.getCode());
        problema.setProperty("timestamp", OffsetDateTime.now().toString());
        return problema;
    }
}
