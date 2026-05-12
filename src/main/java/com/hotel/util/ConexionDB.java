package com.hotel.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Clase utilitaria para gestionar la conexión a la base de datos MySQL.
 *
 * Usa el patrón de diseño SINGLETON para garantizar que solo exista
 * UNA conexión activa a la vez en toda la aplicación.
 *
 * CÓMO USARLA:
 *   Connection conn = ConexionDB.getConexion();
 *
 * ANTES DE USAR:
 *   - Instala MySQL en tu computadora
 *   - Crea la base de datos ejecutando: hotel_sistema.sql
 *   - Cambia DB_PASSWORD por tu contraseña de MySQL
 *
 * @author Fernando
 * @version 1.0
 */
public class ConexionDB {

    // =========================================================
    // CONFIGURACIÓN DE LA BASE DE DATOS
    // Modifica estos valores según tu instalación de MySQL
    // =========================================================

    /** Dirección del servidor MySQL (localhost = tu propia computadora) */
    private static final String DB_HOST = "localhost";

    /** Puerto por defecto de MySQL */
    private static final String DB_PORT = "3306";

    /** Nombre de la base de datos que creamos en el script SQL */
    private static final String DB_NAME = "hotel_sistema";

    /** Usuario de MySQL (por defecto: root) */
    private static final String DB_USER = "root";

    /** ¡¡ CAMBIA ESTO !! - Pon tu contraseña de MySQL aquí */
    private static final String DB_PASSWORD = "1739";

    /** URL completa de conexión JDBC con parámetros de configuración */
    private static final String DB_URL = String.format(
        "jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8",
        DB_HOST, DB_PORT, DB_NAME
    );

    // =========================================================
    // SINGLETON: Solo una conexión en toda la aplicación
    // =========================================================

    /** La única instancia de la conexión (null si está cerrada) */
    private static Connection conexion = null;

    /**
     * Constructor privado: nadie puede crear instancias de esta clase.
     * Esto es parte del patrón Singleton.
     */
    private ConexionDB() {
        // No se instancia esta clase
    }

    /**
     * Obtiene la conexión a la base de datos.
     * Si la conexión no existe o está cerrada, crea una nueva.
     *
     * @return Connection objeto de conexión activa
     * @throws SQLException si no se puede conectar a MySQL
     */
    public static Connection getConexion() throws SQLException {
        // Si no hay conexión o si fue cerrada, crear una nueva
        if (conexion == null || conexion.isClosed()) {
            try {
                // Registrar el driver MySQL
                Class.forName("com.mysql.cj.jdbc.Driver");

                // Crear la conexión
                conexion = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

                System.out.println("✓ Conexión exitosa a MySQL: " + DB_NAME);

            } catch (ClassNotFoundException e) {
                // El JAR de MySQL no está en el proyecto
                throw new SQLException(
                    "No se encontró el driver de MySQL. " +
                    "Verifica que mysql-connector-java esté en las dependencias Maven.\n" +
                    "Error: " + e.getMessage()
                );
            } catch (SQLException e) {
                // Error de conexión (credenciales, servidor apagado, etc.)
                throw new SQLException(
                    "No se pudo conectar a MySQL.\n" +
                    "Verifica que:\n" +
                    "  1. MySQL esté corriendo\n" +
                    "  2. La base de datos '" + DB_NAME + "' exista\n" +
                    "  3. El usuario y contraseña sean correctos\n" +
                    "Error original: " + e.getMessage()
                );
            }
        }
        return conexion;
    }

    /**
     * Cierra la conexión a la base de datos.
     * Llama esto cuando la aplicación se cierre.
     */
    public static void cerrarConexion() {
        if (conexion != null) {
            try {
                if (!conexion.isClosed()) {
                    conexion.close();
                    System.out.println("Conexión a MySQL cerrada correctamente.");
                }
            } catch (SQLException e) {
                System.err.println("Error al cerrar la conexión: " + e.getMessage());
            } finally {
                conexion = null;
            }
        }
    }

    /**
     * Verifica si la conexión está activa sin crear una nueva.
     *
     * @return true si hay conexión activa, false en caso contrario
     */
    public static boolean isConectado() {
        try {
            return conexion != null && !conexion.isClosed() && conexion.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
}
