package com.proyecto.servicios.dto.cliente;

import com.proyecto.servicios.enums.EstadoCuenta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaFiltroRequest {

    private Long clienteId;
    private EstadoCuenta estatus;
}
