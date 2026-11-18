package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.entity.seguridad.CodigoVerificacion;
import com.proyecto.servicios.enums.Plataforma;
import com.proyecto.servicios.enums.PropositoCodigo;

/**
 * Códigos de un solo uso enviados por correo. La validación ocurre siempre en el servidor:
 * el cliente solo recibe un ticket opaco y nunca conoce el código correcto.
 */
public interface CodigoVerificacionService {

    /** Genera el código, lo guarda cifrado y lo envía al correo del usuario. */
    Emision emitir(Usuario usuario, PropositoCodigo proposito, Plataforma plataforma, String dispositivo, String referencia);

    /** Envía un código nuevo para el mismo ticket (respeta la espera y el máximo de reenvíos). */
    Emision reenviar(String ticket);

    /** Código pendiente y vigente para el ticket; si no existe o expiró lanza CODIGO_INVALIDO. */
    CodigoVerificacion buscarVigente(String ticket, PropositoCodigo proposito);

    /** true si coincide (y lo marca como usado); false si no, sumando un intento al código. */
    boolean verificar(CodigoVerificacion codigo, String valor);

    record Emision(String ticket, String correoEnmascarado, long expiraEnSegundos) {

        @Override
        public String toString() {
            return "Emision[ticket=****, correo=" + correoEnmascarado + "]";
        }
    }
}
