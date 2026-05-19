package com.hotel.util;

import com.hotel.modelo.Factura;
import com.hotel.modelo.Reservacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Properties;

/**
 * Servicio de envío de correos electrónicos.
 *
 * Lee la configuración SMTP desde src/main/resources/email.properties.
 * Todos los envíos se realizan en un hilo separado para no bloquear la UI.
 *
 * USO TÍPICO:
 *   EmailService.enviarConfirmacionReservacion(reservacion);
 *
 * CONFIGURACIÓN:
 *   Editar src/main/resources/email.properties con las credenciales SMTP.
 *   Para Gmail usar una "Contraseña de aplicación" (no la contraseña normal).
 *
 * @author Fernando
 * @version 1.0
 */
public final class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final SimpleDateFormat FMT = new SimpleDateFormat("dd/MM/yyyy");

    // =========================================================
    // CONFIGURACIÓN (cargada una sola vez)
    // =========================================================

    private static final Properties SMTP_PROPS;
    private static final String     USUARIO;
    private static final String     PASSWORD;
    private static final String     FROM_NOMBRE;
    private static final String     FROM_EMAIL;
    private static       boolean    habilitado = true;

    static {
        Properties cfg = new Properties();
        Properties smtp = new Properties();
        String usuario = "";
        String password = "";
        String fromNombre = "Hotel";
        String fromEmail = "";

        try (InputStream is = EmailService.class
                .getClassLoader().getResourceAsStream("email.properties")) {

            if (is != null) {
                cfg.load(is);
                smtp.put("mail.smtp.host",                  cfg.getProperty("mail.smtp.host",     "smtp.gmail.com"));
                smtp.put("mail.smtp.port",                  cfg.getProperty("mail.smtp.port",     "587"));
                smtp.put("mail.smtp.auth",                  cfg.getProperty("mail.smtp.auth",     "true"));
                smtp.put("mail.smtp.starttls.enable",       cfg.getProperty("mail.smtp.starttls.enable", "true"));
                smtp.put("mail.smtp.connectiontimeout",     cfg.getProperty("mail.smtp.connectiontimeout", "5000"));
                smtp.put("mail.smtp.timeout",               cfg.getProperty("mail.smtp.timeout",  "5000"));

                usuario    = cfg.getProperty("mail.usuario",     "");
                password   = cfg.getProperty("mail.password",    "");
                fromNombre = cfg.getProperty("mail.from.nombre", "Hotel");
                fromEmail  = cfg.getProperty("mail.from.email",  usuario);

                // Si las credenciales son placeholder, deshabilitar silenciosamente
                if (usuario.startsWith("TU_") || usuario.isEmpty()) {
                    habilitado = false;
                    log.info("EmailService: email.properties no configurado — envíos deshabilitados.");
                }
            } else {
                habilitado = false;
                log.warn("EmailService: email.properties no encontrado — envíos deshabilitados.");
            }
        } catch (IOException e) {
            habilitado = false;
            log.error("EmailService: error cargando email.properties", e);
        }

        SMTP_PROPS  = smtp;
        USUARIO     = usuario;
        PASSWORD    = password;
        FROM_NOMBRE = fromNombre;
        FROM_EMAIL  = fromEmail;
    }

    private EmailService() {}

    // =========================================================
    // API PÚBLICA
    // =========================================================

    /**
     * Envía un correo de confirmación de reservación al cliente.
     * El envío es asíncrono — no bloquea la UI.
     *
     * @param res Reservación recién creada (con cliente y habitación cargados)
     */
    public static void enviarConfirmacionReservacion(Reservacion res) {
        if (!habilitado) return;
        if (res.getCliente() == null || res.getCliente().getEmail() == null) {
            log.debug("EmailService: cliente sin email — no se envía confirmación.");
            return;
        }

        new Thread(() -> {
            try {
                String destinatario = res.getCliente().getEmail();
                String asunto       = "✅ Confirmación de reservación — Hotel Vista";
                String cuerpo       = construirHtml(res);

                enviar(destinatario, asunto, cuerpo);
                log.info("Correo de confirmación enviado a {}", destinatario);

            } catch (Exception e) {
                log.error("Error enviando confirmación de reservación", e);
            }
        }, "EmailSender").start();
    }

    /**
     * Envía la factura en PDF como adjunto al correo del cliente.
     * El envío es asíncrono — no bloquea la UI.
     *
     * @param factura  Factura a enviar (con reservación y cliente cargados)
     * @param pdfFile  Archivo PDF ya generado
     * @return true si el envío se inició (el correo puede tardar segundos)
     */
    public static boolean enviarFacturaPDF(Factura factura, File pdfFile) {
        if (!habilitado) return false;
        Reservacion res = factura.getReservacion();
        if (res == null || res.getCliente() == null) return false;
        String email = res.getCliente().getEmail();
        if (email == null || email.isBlank()) return false;

        new Thread(() -> {
            try {
                String asunto = "🧾 Factura #" + factura.getId() + " — Hotel Vista";
                String html   = construirHtmlFactura(factura);

                enviarConAdjunto(email, asunto, html, pdfFile);
                log.info("Factura PDF enviada a {} (Factura #{})", email, factura.getId());
            } catch (Exception e) {
                log.error("Error enviando factura PDF a {}", email, e);
            }
        }, "EmailFactura").start();

        return true;
    }

    /**
     * Envía un recordatorio de check-in para mañana al cliente.
     * Asíncrono — no bloquea la UI.
     *
     * @param res Reservación con checkin = mañana (con cliente cargado)
     */
    public static void enviarRecordatorioPrecheckin(Reservacion res) {
        if (!habilitado) return;
        if (res.getCliente() == null || res.getCliente().getEmail() == null) return;

        new Thread(() -> {
            try {
                String destinatario = res.getCliente().getEmail();
                String hotelNombre  = HotelConfig.getNombre();
                String asunto       = "🔔 Recordatorio: su check-in es mañana — " + hotelNombre;
                String cuerpo       = construirHtmlRecordatorio(res);
                enviar(destinatario, asunto, cuerpo);
                log.info("Recordatorio pre-checkin enviado a {} (res #{})", destinatario, res.getId());
            } catch (Exception e) {
                log.error("Error enviando recordatorio pre-checkin (res #{})", res.getId(), e);
            }
        }, "EmailRecordatorio-" + res.getId()).start();
    }

    private static String construirHtmlRecordatorio(Reservacion res) {
        String nombre   = res.getCliente().getNombreCompleto();
        String numHab   = res.getHabitacion() != null ? res.getHabitacion().getNumero() : "—";
        String tipoHab  = (res.getHabitacion() != null && res.getHabitacion().getTipo() != null)
                          ? res.getHabitacion().getTipo().getNombre() : "—";
        String checkin  = res.getFechaCheckin()  != null ? FMT.format(res.getFechaCheckin())  : "—";
        String checkout = res.getFechaCheckout() != null ? FMT.format(res.getFechaCheckout()) : "—";
        String hotel    = HotelConfig.getNombre();
        String dir      = HotelConfig.getDireccion();
        String tel      = HotelConfig.getTelefono();

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='" +
            "font-family:Segoe UI,Arial,sans-serif;background:#f0f4ff;margin:0;padding:20px;'>" +
            "<div style='max-width:560px;margin:0 auto;background:#fff;border-radius:8px;" +
            "box-shadow:0 2px 8px rgba(0,0,0,0.12);overflow:hidden;'>" +
            "<div style='background:#e65100;padding:28px 32px;'>" +
            "<h1 style='color:#fff;margin:0;font-size:20px;'>🔔 " + hotel + "</h1>" +
            "<p style='color:#ffe0b2;margin:4px 0 0;font-size:13px;'>Recordatorio de check-in</p>" +
            "</div>" +
            "<div style='padding:28px 32px;'>" +
            "<p style='font-size:15px;color:#333;'>Estimado/a <strong>" + nombre + "</strong>,</p>" +
            "<p style='font-size:14px;color:#555;'>Le recordamos que <strong>mañana es su fecha de check-in</strong>. " +
            "Estamos preparados para recibirle.</p>" +
            "<table style='width:100%;border-collapse:collapse;margin:16px 0;font-size:14px;'>" +
            "<tr style='background:#fff3e0;'><td style='padding:8px 12px;color:#777;'>Habitación</td>" +
            "<td style='padding:8px 12px;font-weight:bold;color:#333;'>" + numHab + " — " + tipoHab + "</td></tr>" +
            "<tr><td style='padding:8px 12px;color:#777;'>Check-in</td>" +
            "<td style='padding:8px 12px;font-weight:bold;color:#e65100;'>" + checkin + "</td></tr>" +
            "<tr style='background:#f9f9f9;'><td style='padding:8px 12px;color:#777;'>Check-out</td>" +
            "<td style='padding:8px 12px;color:#333;'>" + checkout + "</td></tr>" +
            "</table>" +
            (dir.isBlank() ? "" : "<p style='font-size:13px;color:#555;'>📍 " + dir + "</p>") +
            (tel.isBlank() ? "" : "<p style='font-size:13px;color:#555;'>📞 " + tel + "</p>") +
            "<p style='font-size:13px;color:#888;margin-top:20px;'>Si tiene alguna pregunta, " +
            "no dude en contactarnos.</p>" +
            "</div>" +
            "<div style='background:#f5f5f5;padding:12px 32px;font-size:11px;color:#aaa;'>" +
            "Este correo fue generado automáticamente por " + hotel + " · Sistema de Gestión Hotelera" +
            "</div></div></body></html>";
    }

    // =========================================================
    // ENVÍO INTERNO
    // =========================================================

    private static void enviar(String destinatario, String asunto, String htmlBody)
            throws MessagingException, java.io.UnsupportedEncodingException {

        Session session = Session.getInstance(SMTP_PROPS, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USUARIO, PASSWORD);
            }
        });

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(FROM_EMAIL, FROM_NOMBRE));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
        msg.setSubject(asunto, "UTF-8");
        msg.setContent(htmlBody, "text/html; charset=UTF-8");

        Transport.send(msg);
    }

    private static void enviarConAdjunto(String destinatario, String asunto,
                                          String htmlBody, File adjunto)
            throws MessagingException, java.io.UnsupportedEncodingException {

        Session session = Session.getInstance(SMTP_PROPS, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USUARIO, PASSWORD);
            }
        });

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(FROM_EMAIL, FROM_NOMBRE));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
        msg.setSubject(asunto, "UTF-8");

        // Parte HTML
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");

        // Parte adjunto PDF
        MimeBodyPart pdfPart = new MimeBodyPart();
        try {
            pdfPart.attachFile(adjunto);
            pdfPart.setFileName("Factura_" + adjunto.getName());
        } catch (IOException e) {
            throw new MessagingException("No se pudo adjuntar el PDF: " + e.getMessage(), e);
        }

        MimeMultipart multipart = new MimeMultipart();
        multipart.addBodyPart(htmlPart);
        multipart.addBodyPart(pdfPart);
        msg.setContent(multipart);

        Transport.send(msg);
    }

    // =========================================================
    // PLANTILLA HTML
    // =========================================================

    private static String construirHtml(Reservacion res) {
        String nombreCliente = res.getCliente().getNombreCompleto();
        String numHab        = res.getHabitacion() != null ? res.getHabitacion().getNumero() : "—";
        String tipoHab       = (res.getHabitacion() != null && res.getHabitacion().getTipo() != null)
                               ? res.getHabitacion().getTipo().getNombre() : "—";
        String checkin       = res.getFechaCheckin()  != null ? FMT.format(res.getFechaCheckin())  : "—";
        String checkout      = res.getFechaCheckout() != null ? FMT.format(res.getFechaCheckout()) : "—";
        long   noches        = res.getNoches();
        double total         = res.getTotalConImpuesto();
        String estado        = res.getEstado() != null ? res.getEstado() : "PENDIENTE";

        return "<!DOCTYPE html>" +
            "<html><head><meta charset='UTF-8'></head><body style='" +
            "font-family:Segoe UI,Arial,sans-serif;background:#f0f4ff;margin:0;padding:20px;'>" +

            // Contenedor
            "<div style='max-width:560px;margin:0 auto;background:#fff;border-radius:8px;" +
            "box-shadow:0 2px 8px rgba(0,0,0,0.12);overflow:hidden;'>" +

            // Header
            "<div style='background:#1a237e;padding:28px 32px;'>" +
            "<h1 style='color:#fff;margin:0;font-size:22px;'>🏨 Hotel Vista</h1>" +
            "<p style='color:#c5caf0;margin:4px 0 0;font-size:13px;'>Confirmación de reservación</p>" +
            "</div>" +

            // Saludo
            "<div style='padding:28px 32px 0;'>" +
            "<p style='font-size:15px;color:#333;'>Estimado/a <strong>" + nombreCliente + "</strong>,</p>" +
            "<p style='font-size:14px;color:#555;'>Su reservación ha sido registrada exitosamente. " +
            "A continuación encontrará los detalles:</p>" +
            "</div>" +

            // Tabla de detalles
            "<div style='padding:0 32px 24px;'>" +
            "<table style='width:100%;border-collapse:collapse;font-size:14px;'>" +
            fila("🛏 Habitación",    "N° " + numHab + " — " + tipoHab) +
            fila("📅 Check-In",      checkin) +
            fila("📅 Check-Out",     checkout) +
            fila("🌙 Noches",        String.valueOf(noches)) +
            fila("💰 Total (c/IVA)", String.format("Q %.2f", total)) +
            fila("📋 Estado",        estado) +
            "</table>" +
            "</div>" +

            // Nota
            "<div style='background:#f8f9ff;padding:16px 32px;border-top:1px solid #e8eaf6;'>" +
            "<p style='font-size:12px;color:#888;margin:0;'>" +
            "Si tiene alguna consulta, no dude en contactarnos. " +
            "Le esperamos con gusto.</p>" +
            "</div>" +

            // Footer
            "<div style='padding:14px 32px;background:#1a237e;'>" +
            "<p style='color:#c5caf0;font-size:11px;margin:0;text-align:center;'>" +
            "Este correo fue generado automáticamente — por favor no responda a esta dirección." +
            "</p></div>" +

            "</div></body></html>";
    }

    /** Genera una fila <tr> para la tabla de detalles. */
    private static String fila(String campo, String valor) {
        return "<tr style='border-bottom:1px solid #f0f0f0;'>" +
               "<td style='padding:10px 12px;color:#555;width:45%;'>" + campo + "</td>" +
               "<td style='padding:10px 12px;color:#222;font-weight:600;'>" + valor + "</td>" +
               "</tr>";
    }

    // =========================================================
    // PLANTILLA HTML — FACTURA
    // =========================================================

    private static String construirHtmlFactura(Factura f) {
        Reservacion res = f.getReservacion();
        String nombre  = res != null && res.getCliente() != null
            ? res.getCliente().getNombreCompleto() : "Cliente";
        String hab     = res != null && res.getHabitacion() != null
            ? "N° " + res.getHabitacion().getNumero() : "—";
        String tipo    = (res != null && res.getHabitacion() != null
                         && res.getHabitacion().getTipo() != null)
            ? res.getHabitacion().getTipo().getNombre() : "—";
        String ci  = res != null && res.getFechaCheckin()  != null ? FMT.format(res.getFechaCheckin())  : "—";
        String co  = res != null && res.getFechaCheckout() != null ? FMT.format(res.getFechaCheckout()) : "—";
        long noches    = res != null ? res.getNoches() : 0;
        String emision = f.getFechaEmision() != null ? FMT.format(f.getFechaEmision()) : FMT.format(new java.util.Date());

        return "<!DOCTYPE html>" +
            "<html><head><meta charset='UTF-8'></head><body style='" +
            "font-family:Segoe UI,Arial,sans-serif;background:#f0f4ff;margin:0;padding:20px;'>" +

            "<div style='max-width:580px;margin:0 auto;background:#fff;border-radius:8px;" +
            "box-shadow:0 2px 8px rgba(0,0,0,0.12);overflow:hidden;'>" +

            // Header
            "<div style='background:#1a237e;padding:28px 32px;'>" +
            "<h1 style='color:#fff;margin:0;font-size:22px;'>🏨 Hotel Vista</h1>" +
            "<p style='color:#c5caf0;margin:4px 0 0;font-size:13px;'>Factura #" + f.getId() + "</p>" +
            "</div>" +

            // Cuerpo
            "<div style='padding:28px 32px 0;'>" +
            "<p style='font-size:15px;color:#333;'>Estimado/a <strong>" + nombre + "</strong>,</p>" +
            "<p style='font-size:14px;color:#555;'>Adjunto encontrará su factura en formato PDF. " +
            "A continuación un resumen de la estancia:</p>" +
            "</div>" +

            "<div style='padding:0 32px 24px;'>" +
            "<table style='width:100%;border-collapse:collapse;font-size:14px;'>" +
            fila("📅 Fecha de emisión",  emision) +
            fila("🛏 Habitación",        hab + " — " + tipo) +
            fila("📅 Check-In",          ci) +
            fila("📅 Check-Out",         co) +
            fila("🌙 Noches",            String.valueOf(noches)) +
            fila("💳 Método de pago",    f.getMetodoPago() != null ? f.getMetodoPago() : "—") +
            fila("💰 Subtotal",          String.format("Q %.2f", f.getSubtotal())) +
            fila("🏦 Impuesto (IVA)",    String.format("Q %.2f", f.getImpuesto())) +
            "<tr style='background:#1a237e;'>" +
            "<td style='padding:12px;color:#fff;font-weight:700;'>💎 TOTAL</td>" +
            "<td style='padding:12px;color:#fff;font-weight:700;font-size:16px;'>" +
            String.format("Q %.2f", f.getTotal()) + "</td></tr>" +
            "</table></div>" +

            "<div style='background:#f8f9ff;padding:16px 32px;border-top:1px solid #e8eaf6;'>" +
            "<p style='font-size:12px;color:#888;margin:0;'>" +
            "El PDF completo de su factura está adjunto a este correo. " +
            "Gracias por hospedarse con nosotros.</p></div>" +

            "<div style='padding:14px 32px;background:#1a237e;'>" +
            "<p style='color:#c5caf0;font-size:11px;margin:0;text-align:center;'>" +
            "Este correo fue generado automáticamente — por favor no responda a esta dirección." +
            "</p></div>" +

            "</div></body></html>";
    }
}
