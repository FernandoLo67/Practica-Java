package com.hotel.util;

import com.hotel.dao.impl.ReservacionDAOImpl;
import com.hotel.modelo.Reservacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Servicio que envía recordatorios de check-in por email un día antes.
 *
 * Cómo funciona:
 *   - Al iniciarse calcula cuántos segundos faltan hasta las 08:00 AM del
 *     día de hoy (o mañana si ya pasaron las 08:00).
 *   - Ejecuta la tarea en ese momento y luego cada 24 horas.
 *   - Por cada reservación con check-in mañana y cliente con email,
 *     delega a EmailService.enviarRecordatorioPrecheckin().
 *
 * Uso (desde Main.java):
 *   RecordatorioCheckinService.iniciar();
 *
 * El servicio se detiene automáticamente con la JVM via daemon thread.
 *
 * @author Fernando
 * @version 1.0
 */
public final class RecordatorioCheckinService {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioCheckinService.class);

    /** Hora del día (24h) en que se envían los recordatorios */
    private static final int HORA_ENVIO = 8;   // 08:00 AM

    private static final ScheduledExecutorService SCHEDULER =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "recordatorio-checkin");
            t.setDaemon(true);   // no bloquea shutdown de la JVM
            return t;
        });

    private static volatile boolean iniciado = false;

    private RecordatorioCheckinService() {}

    // =========================================================
    // INICIO
    // =========================================================

    /**
     * Inicia el servicio. Solo tiene efecto la primera vez que se llama.
     * Programa la tarea para las {@value #HORA_ENVIO}:00 h de hoy (o mañana)
     * y luego cada 24 horas.
     */
    public static synchronized void iniciar() {
        if (iniciado) return;
        iniciado = true;

        long demoraSeg = calcularDemoraHastaProximaEjecucion();
        log.info("RecordatorioCheckinService iniciado — primera ejecución en {} minutos.",
            TimeUnit.SECONDS.toMinutes(demoraSeg));

        SCHEDULER.scheduleAtFixedRate(
            RecordatorioCheckinService::ejecutar,
            demoraSeg,
            TimeUnit.DAYS.toSeconds(1),   // repetir cada 24 h
            TimeUnit.SECONDS
        );
    }

    // =========================================================
    // TAREA PRINCIPAL
    // =========================================================

    /**
     * Consulta la BD, filtra reservaciones con check-in mañana y envía
     * el email a cada cliente. Se ejecuta en el hilo del scheduler.
     */
    static void ejecutar() {
        log.info("RecordatorioCheckinService — buscando check-ins para mañana...");
        try {
            List<Reservacion> manana = new ReservacionDAOImpl().listarCheckinManana();

            if (manana.isEmpty()) {
                log.info("Sin check-ins para mañana — ningún recordatorio enviado.");
                return;
            }

            log.info("Enviando {} recordatorio(s) pre-checkin.", manana.size());
            int enviados = 0;
            for (Reservacion res : manana) {
                try {
                    EmailService.enviarRecordatorioPrecheckin(res);
                    enviados++;
                } catch (Exception e) {
                    log.error("Error enviando recordatorio para reservación #{}", res.getId(), e);
                }
            }
            log.info("Recordatorios pre-checkin: {}/{} enviados.", enviados, manana.size());

        } catch (Exception e) {
            log.error("Error en RecordatorioCheckinService.ejecutar()", e);
        }
    }

    // =========================================================
    // UTILIDAD
    // =========================================================

    /**
     * Calcula los segundos hasta la próxima ejecución a las HORA_ENVIO:00.
     * Si ya pasó la hora hoy, programa para mañana.
     */
    private static long calcularDemoraHastaProximaEjecucion() {
        ZoneId zona = ZoneId.systemDefault();
        ZonedDateTime ahora = ZonedDateTime.now(zona);
        ZonedDateTime objetivo = ahora.toLocalDate()
            .atTime(LocalTime.of(HORA_ENVIO, 0))
            .atZone(zona);

        // Si ya pasó la hora de hoy, programar para mañana
        if (!objetivo.isAfter(ahora)) {
            objetivo = objetivo.plusDays(1);
        }

        return Duration.between(ahora, objetivo).getSeconds();
    }
}
