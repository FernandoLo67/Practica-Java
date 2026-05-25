package com.hotel.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Servicio de backup de la base de datos MySQL.
 *
 * Usa mysqldump (debe estar en PATH o en una ruta conocida de MySQL)
 * para generar un archivo .sql completo con estructura y datos.
 *
 * Credenciales leídas de database.properties (mismo archivo que ConexionDB).
 *
 * USO:
 *   BackupService.BackupResult r = BackupService.generarBackup(destino);
 *   if (r.exitoso) // usar r.archivo, r.tamanio
 *
 * @author Fernando
 * @version 1.0
 */
public final class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    /** Rutas comunes de mysqldump en Windows (en orden de preferencia). */
    private static final String[] RUTAS_WIN = {
        "mysqldump",                                                          // en PATH
        "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump.exe",
        "C:\\Program Files\\MySQL\\MySQL Server 8.4\\bin\\mysqldump.exe",
        "C:\\Program Files\\MySQL\\MySQL Server 5.7\\bin\\mysqldump.exe",
        "C:\\xampp\\mysql\\bin\\mysqldump.exe",
        "C:\\wamp64\\bin\\mysql\\mysql8.0.31\\bin\\mysqldump.exe",
    };

    private BackupService() {}

    // =========================================================
    // RESULTADO
    // =========================================================

    public static class BackupResult {
        public final boolean exitoso;
        public final File    archivo;
        public final long    tamanioBytes;
        public final String  error;

        BackupResult(boolean exitoso, File archivo, long tamanioBytes, String error) {
            this.exitoso      = exitoso;
            this.archivo      = archivo;
            this.tamanioBytes = tamanioBytes;
            this.error        = error;
        }

        public String tamanioFormateado() {
            if (tamanioBytes < 1024)            return tamanioBytes + " B";
            if (tamanioBytes < 1024 * 1024)     return String.format("%.1f KB", tamanioBytes / 1024.0);
            return String.format("%.2f MB", tamanioBytes / (1024.0 * 1024));
        }
    }

    // =========================================================
    // API PÚBLICA
    // =========================================================

    /**
     * Genera el backup en el directorio especificado.
     * Nombre del archivo: hotel_backup_YYYY-MM-DD_HH-mm-ss.sql
     *
     * @param directorio Carpeta destino (debe existir o poder crearse)
     * @return BackupResult con estado, archivo y tamaño
     */
    public static BackupResult generarBackup(File directorio) {
        Properties db = cargarProps();
        String host   = db.getProperty("db.host",     "localhost");
        String port   = db.getProperty("db.port",     "3306");
        String nombre = db.getProperty("db.name",     "hotel_sistema");
        String user   = db.getProperty("db.user",     "root");
        String pass   = db.getProperty("db.password", "");

        if (!directorio.exists()) directorio.mkdirs();

        String ts        = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File   destino   = new File(directorio, "hotel_backup_" + ts + ".sql");
        String mysqldump = buscarMysqldump();

        if (mysqldump == null) {
            return new BackupResult(false, null, 0,
                "No se encontró mysqldump.\n" +
                "Instala MySQL o agrega el bin de MySQL al PATH del sistema.");
        }

        // Construir comando
        // [C-03] SEGURIDAD: la contraseña NUNCA va como argumento CLI (visible en ps/Task Manager).
        // Se pasa exclusivamente via variable de entorno MYSQL_PWD, que NO es visible
        // para otros procesos del sistema en ningún SO soportado.
        List<String> cmd = new ArrayList<>();
        cmd.add(mysqldump);
        cmd.add("--host=" + host);
        cmd.add("--port=" + port);
        cmd.add("--user=" + user);
        // SIN --password=xxx aquí — se inyecta vía MYSQL_PWD en el entorno del proceso
        cmd.add("--single-transaction");   // backup consistente sin bloquear tablas
        cmd.add("--routines");
        cmd.add("--triggers");
        cmd.add("--add-drop-table");
        cmd.add("--result-file=" + destino.getAbsolutePath());
        cmd.add(nombre);

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            // Contraseña en variable de entorno del proceso hijo — no expuesta al sistema
            if (!pass.isEmpty()) {
                pb.environment().put("MYSQL_PWD", pass);
            }

            Process proc = pb.start();

            // Capturar salida por si hay error
            StringBuilder salida = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()))) {
                String linea;
                while ((linea = br.readLine()) != null) salida.append(linea).append('\n');
            }

            int exitCode = proc.waitFor();

            if (exitCode != 0 || !destino.exists() || destino.length() == 0) {
                return new BackupResult(false, null, 0,
                    "mysqldump falló (código " + exitCode + "):\n" + salida);
            }

            log.info("Backup generado: {} ({} bytes)", destino.getName(), destino.length());
            return new BackupResult(true, destino, destino.length(), null);

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return new BackupResult(false, null, 0, "Error ejecutando mysqldump: " + e.getMessage());
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    /** Busca la primera ruta de mysqldump disponible. */
    private static String buscarMysqldump() {
        for (String ruta : RUTAS_WIN) {
            try {
                if (ruta.equals("mysqldump")) {
                    // Probar si está en PATH
                    Process p = new ProcessBuilder("mysqldump", "--version")
                        .redirectErrorStream(true).start();
                    if (p.waitFor() == 0) return "mysqldump";
                } else {
                    File f = new File(ruta);
                    if (f.exists() && f.canExecute()) return ruta;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static Properties cargarProps() {
        Properties p = new Properties();
        try (InputStream is = BackupService.class
                .getClassLoader().getResourceAsStream("database.properties")) {
            if (is != null) p.load(is);
        } catch (IOException e) {
            log.warn("No se pudo leer database.properties: {}", e.getMessage());
        }
        return p;
    }
}
