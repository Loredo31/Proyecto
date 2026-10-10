package com.proyecto.servicios.dto.cliente;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponseDto {

    private Long id;
    private DatosPersonalesDto datosPersonales;
    private DatosContactoDto datosContacto;
    private DomicilioDto domicilio;
    private InformacionLaboralDto informacionLaboral;
    private CuentaBancariaResponseDto cuentaBancaria;
    private List<CuentaBancariaResponseDto> cuentas;
    private UsuarioResponseDto usuario;
    private Boolean activoLogin;
    private Boolean biometriaEnrolada;
    private LocalDateTime fechaCreacion;
}

