package com.proyecto.servicios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proyecto.servicios.enums.ApiResponseEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    private String code;
    private String message;
    private T data;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now(ZONE_ID);

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(ApiResponseEnum.EXITO.getCode())
                .message(ApiResponseEnum.EXITO.getMessage())
                .data(data)
                .timestamp(LocalDateTime.now(ZONE_ID))
                .build();
    }

    public static <T> ApiResponse<T> success(String code, String message, T data) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now(ZONE_ID))
                .build();
    }

    public static <T> ApiResponse<T> error(ApiResponseEnum responseEnum) {
        return ApiResponse.<T>builder()
                .code(responseEnum.getCode())
                .message(responseEnum.getMessage())
                .timestamp(LocalDateTime.now(ZONE_ID))
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now(ZONE_ID))
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message, T data) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now(ZONE_ID))
                .build();
    }
}

