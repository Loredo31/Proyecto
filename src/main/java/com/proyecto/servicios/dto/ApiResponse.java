package com.proyecto.servicios.dto;

import com.proyecto.servicios.enums.ApiResponseEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Envoltorio estándar de toda respuesta de la API.
 *
 * @param <T> tipo del payload de datos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private String code;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return build(ApiResponseEnum.EXITO, data);
    }

    public static <T> ApiResponse<T> error(ApiResponseEnum status) {
        return build(status, null);
    }

    private static <T> ApiResponse<T> build(ApiResponseEnum status, T data) {
        return ApiResponse.<T>builder()
                .code(status.getCode())
                .message(status.getMessage())
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
