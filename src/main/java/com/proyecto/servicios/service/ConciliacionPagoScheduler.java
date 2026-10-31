package com.proyecto.servicios.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConciliacionPagoScheduler {

    private final TransaccionPagoService transaccionPagoService;
    private final PagoServicioService pagoServicioService;

    @Scheduled(fixedDelayString = "${gestopago.tx.conciliacion-intervalo-ms:120000}",
            initialDelayString = "${gestopago.tx.conciliacion-intervalo-ms:120000}")
    public void conciliar() {
        try {
            int movidas = transaccionPagoService.moverProcesandoVencidasAConciliacion();
            if (movidas > 0) {
                log.warn("{} transacciones atascadas en PROCESANDO pasaron a PENDIENTE_CONCILIACION", movidas);
            }

            int resueltas = pagoServicioService.conciliarPendientes();
            if (resueltas > 0) {
                log.info("{} pagos pendientes resueltos con confirmTx", resueltas);
            }

            long vencidas = transaccionPagoService.contarPendientesFueraDeVentana();
            if (vencidas > 0) {
                log.error("ALERTA: {} pagos pendientes superaron la vigencia de confirmTx (24 h) "
                        + "y requieren conciliación manual con GestoPago", vencidas);
            }
        } catch (Exception ex) {
            log.error("Error en la conciliación de pagos: {}", ex.getMessage());
        }
    }
}
