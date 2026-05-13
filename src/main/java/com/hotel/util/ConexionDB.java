package com.hotel.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.hotel.exception.HotelException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Gestor del pool de conexiones a la base de datos MySQL.
 *
 * Reemplaza el antiguo Singleton de una sola conexión con HikariCP,
 * el pool de conexiones más rápido para Java. Ventajas:
 *   - Múltiples conexiones simultáneas (hasta maximumPoolSize)
 *   - Reconexión automática si MySQL se reinicia
 *   - Monitoreo de conexiones caídas (keepaliveTime)
 *   - Credenciales en archivo externo (no en el código fuente)
 *
 * Las credenciales se leen de: src/main/resources/database.properties
 * Ese archivo está en .gitignore (nunca se sube a GitHub).
 *
 * Uso:
 *   try (Connection conn = ConexionDB.getConexion()) {
 *       // usar conn...
 *   } // se devuelve al pool automáticamente al cerrar
 *
 * @author Fernando
 * @version 2.0 - HikariCP + credentials en properties
 */
public final class ConexionDB {

    /** Pool de conexiones HikariCP (se inicializa una sola vez) */
    private static volatile HikariDataSource dataSource = null;

    /** Mutex para inicialización thread-safe */
    private static final Object LOCK = new Object();

    private ConexionDB() {}

    // =========================================================
    // MÉTODO PRINCIPAL
    // =========================================================

    /**
     * Obtiene una conexión del pool HikariCP.
     *
     * IMPORTANTE: úsala siempre con try-with-resources para
     * que la conexión vuelva al pool automáticamente al salir.
     *
     * @return Connection activa del pool
     * @throws SQLException si no hay conexiones disponibles
     */
    public static Connection getConexion() throws SQLException {
        if (dataSource == null) {
            inicializarPool();
        }
        return dataSource.getConnection();
    }

    // =========================================================
    // INICIALIZACIÓN DEL POOL
    // =========================================================

    /**
     * Crea el pool HikariCP leyendo la configuración de database.properties.
     * Se ejecuta solo la primera vez (patrón Double-Checked Locking).
     */
    private static void inicializarPool() {
        synchronized (LOCK) {
            if (dataSource != null) return; // otra hebra ya lo inicializó

            Properties props = cargarProperties();

            String host     = props.getProperty("db.host",     "localhost");
            String port     = props.getProperty("db.port",     "3306");
            String name     = props.getProperty("db.name",     "hotel_sistema");
            String user     = props.getProperty("db.user",     "root");
            String password = props.getProperty("db.password", "");

            String url = String.format(
                "jdbc:mysql://%s:%s/%s" +
                "?useSSL=false" +
                "&serverTimezone=UTC" +
                "&allowPublicKeyRetrieval=true" +
                "&useUnicode=true" +
                "&characterEncoding=UTF-8" +
                "&autoReconnect=true",
                host, port, name
            );

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Tamaño del pool
            config.setMaximumPoolSize(
                intProp(props, "db.pool.maximumPoolSize", 10));
            config.setMinimumIdle(
                intProp(props, "db.pool.minimumIdle", 2));

            // Timeouts
            config.setConnectionTimeout(
                longProp(props, "db.pool.connectionTimeout", 30_000L));
            config.setIdleTimeout(
                longProp(props, "db.pool.idleTimeout", 600_000L));
            config.setMaxLifetime(
                longProp(props, "db.pool.maxLifetime", 1_800_000L));

            // Validación de conexiones
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("HotelPool");

            try {
                dataSource = new HikariDataSource(config);
                System.out.println("✓ Pool HikariCP iniciado: " + name +
                    " (max=" + config.getMaximumPoolSize() + " conexiones)");
            } catch (Exception e) {
                throw new HotelException(
                    HotelException.CONEXION_FALLIDA,
                    "No se pudo conectar a MySQL.\n" +
                    "Verifica que MySQL esté corriendo y que database.properties sea correcto.\n" +
                    "Error: " + e.getMessage(), e
                );
            }
        }
    }

    // =========================================================
    // LECTURA DE PROPERTIES
    // =========================================================

    /**
     * Carga el archivo database.properties desde el classpath.
     * Si no existe, devuelve un Properties vacío (se usarán los defaults).
     */
    private static Properties cargarProperties() {
        Properties props = new Properties();
        try (InputStream is = ConexionDB.class
                .getClassLoader()
                .getResourceAsStream("database.properties")) {

            if (is != null) {
                props.load(is);
                System.out.println("✓ Configuración cargada de database.properties");
            } else {
                System.out.println("⚠ database.properties no encontrado, usando valores por defecto");
            }
        } catch (IOException e) {
            System.err.println("⚠ Error al leer database.properties: " + e.getMessage());
        }
        return props;
    }

    // =========================================================
    // UTILIDADES
    // =========================================================

    private static int intProp(Properties p, String key, int def) {
        try { return Integer.parseInt(p.getProperty(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }

    private static long longProp(Properties p, String key, long def) {
        try { return Long.parseLong(p.getProperty(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }

    // =========================================================
    // CICLO DE VIDA
    // =========================================================

    /**
     * Cierra el pool de conexiones.
     * Llama esto cuando la aplicación se cierre (en el shutdown hook).
     */
    public static void cerrarPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("Pool HikariCP cerrado correctamente.");
        }
    }

    /**
     * @deprecated Usar cerrarPool(). Mantenido por compatibilidad.
     */
    @Deprecated
    public static void cerrarConexion() {
        cerrarPool();
    }

    /**
     * Verifica si el pool está activo y puede entregar conexiones.
     */
    public static boolean isConectado() {
        if (dataSource == null || dataSource.isClosed()) return false;
        try (Connection c = dataSource.getConnection()) {
            return c.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
}
