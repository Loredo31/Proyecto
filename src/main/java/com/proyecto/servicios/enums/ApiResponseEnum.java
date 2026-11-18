package com.proyecto.servicios.enums;

import org.springframework.http.HttpStatus;

public enum ApiResponseEnum {

    EXITO(HttpStatus.OK, "000", "Operación exitosa"),
    CREADO(HttpStatus.CREATED, "201", "Registro creado exitosamente"),
    CODIGO_ENVIADO(HttpStatus.OK, "20002", "Se envió un código de verificación a tu correo"),
    CURP_DUPLICADO(HttpStatus.CONFLICT, "40901", "La CURP ya se encuentra registrada"),
    RFC_DUPLICADO(HttpStatus.CONFLICT, "40902", "El RFC ya se encuentra registrado"),
    TELEFONO_DUPLICADO(HttpStatus.CONFLICT, "40903", "El teléfono móvil ya se encuentra registrado"),
    CORREO_DUPLICADO(HttpStatus.CONFLICT, "40904", "El correo electrónico ya se encuentra registrado"),
    CLIENTE_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "40402", "El cliente no fue encontrado"),
    USUARIO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "40404", "El usuario no fue encontrado"),
    CUENTA_NO_ENCONTRADA(HttpStatus.NOT_FOUND, "40403", "La cuenta bancaria no fue encontrada"),
    CUENTA_BLOQUEADA(HttpStatus.FORBIDDEN, "40301", "La cuenta bancaria se encuentra bloqueada"),
    ACCESO_DENEGADO(HttpStatus.FORBIDDEN, "40302", "El acceso a la aplicación se encuentra bloqueado"),
    DATOS_INVALIDOS(HttpStatus.BAD_REQUEST, "40001", "Los datos enviados son inválidos"),
    CATALOGO_NO_DISPONIBLE(HttpStatus.NOT_FOUND, "40401", "El catálogo de productos no se encuentra disponible en caché"),
    SERVICIO_EXTERNO_NO_DISPONIBLE(HttpStatus.SERVICE_UNAVAILABLE, "50301", "El servicio externo de catálogo no se encuentra disponible"),
    ERROR_AUTENTICACION(HttpStatus.UNAUTHORIZED, "40101", "Autenticación fallida contra el servicio externo de catálogo"),
    TIMEOUT_SERVICIO(HttpStatus.GATEWAY_TIMEOUT, "50401", "Tiempo de espera agotado al consultar el servicio externo de catálogo"),
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR, "50001", "Error interno al procesar la solicitud"),

    PAGO_EXITOSO(HttpStatus.OK, "000", "Pago aplicado exitosamente"),
    PAGO_EN_CONCILIACION(HttpStatus.ACCEPTED, "20201", "El pago está en verificación con el proveedor; el saldo permanece retenido hasta su conciliación"),
    CUENTA_INACTIVA(HttpStatus.FORBIDDEN, "40303", "La cuenta no se encuentra activa"),
    PIN_INVALIDO(HttpStatus.FORBIDDEN, "40304", "El PIN de seguridad es incorrecto"),
    CUENTA_NO_PERTENECE(HttpStatus.FORBIDDEN, "40305", "La cuenta no pertenece al cliente autenticado"),
    TRANSACCION_NO_ENCONTRADA(HttpStatus.NOT_FOUND, "40405", "La transacción no fue encontrada"),
    PIN_NO_CONFIGURADO(HttpStatus.CONFLICT, "40905", "La cuenta no tiene un PIN de seguridad configurado"),
    TRANSACCION_DUPLICADA(HttpStatus.CONFLICT, "40906", "Ya existe un pago idéntico en proceso o aplicado recientemente"),
    OPERACION_CONCURRENTE(HttpStatus.CONFLICT, "40907", "La cuenta está siendo modificada por otra operación, intente nuevamente"),
    SALDO_INSUFICIENTE(HttpStatus.UNPROCESSABLE_ENTITY, "42201", "Saldo insuficiente para realizar el pago"),
    PRODUCTO_INVALIDO(HttpStatus.UNPROCESSABLE_ENTITY, "42202", "El producto o servicio solicitado no es válido"),
    PAGO_RECHAZADO(HttpStatus.UNPROCESSABLE_ENTITY, "42203", "El proveedor rechazó el pago; no se realizó ningún cargo"),
    PROVEEDOR_PAGOS_NO_DISPONIBLE(HttpStatus.SERVICE_UNAVAILABLE, "50302", "El proveedor de pagos no está disponible; no se realizó ningún cargo"),

    CREDENCIALES_INVALIDAS(HttpStatus.UNAUTHORIZED, "40102", "Correo o contraseña incorrectos"),
    TOKEN_INVALIDO(HttpStatus.UNAUTHORIZED, "40103", "Token ausente, inválido o expirado"),
    SESION_FINALIZADA(HttpStatus.UNAUTHORIZED, "40104", "La sesión fue finalizada; inicie sesión nuevamente"),
    REFRESH_INVALIDO(HttpStatus.UNAUTHORIZED, "40105", "La sesión expiró; inicie sesión nuevamente"),
    SIN_PERMISOS(HttpStatus.FORBIDDEN, "40306", "No tiene permisos para realizar esta operación"),
    PIN_REQUERIDO(HttpStatus.BAD_REQUEST, "40002", "El PIN de seguridad es obligatorio en el header X-Auth-Pin"),
    SESION_ACTIVA(HttpStatus.CONFLICT, "40908", "Ya existe una sesión activa; se solicitó su cierre al otro dispositivo"),
    SOLICITUD_SESION_INVALIDA(HttpStatus.NOT_FOUND, "40406", "La solicitud de sesión no existe o ya no es válida"),
    CUENTA_BLOQUEADA_TEMPORAL(HttpStatus.LOCKED, "42301", "La cuenta está bloqueada temporalmente por intentos fallidos"),
    CODIGO_INVALIDO(HttpStatus.UNAUTHORIZED, "40106", "El código de verificación es incorrecto o ya no es válido"),
    DEMASIADAS_SOLICITUDES(HttpStatus.TOO_MANY_REQUESTS, "42901", "Demasiadas solicitudes; espere antes de intentarlo de nuevo"),
    CORREO_NO_DISPONIBLE(HttpStatus.SERVICE_UNAVAILABLE, "50303", "No fue posible enviar el correo de verificación; intente más tarde");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ApiResponseEnum(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

