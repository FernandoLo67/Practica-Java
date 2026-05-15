package com.hotel.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

/**
 * Configuración estática del hotel leída desde hotel.properties.
 *
 * Orden de búsqueda del archivo:
 *   1. hotel.properties en el directorio de trabajo (junto al JAR en producción,
 *      en la raíz del proyecto en desarrollo con NetBeans).
 *   2. hotel.properties en el classpath (src/main/resources/) como fallback.
 *
 * Los cambios se persisten siempre en el directorio de trabajo para que
 * sobrevivan a recompilaciones.
 *
 * Uso:
 *   String nombre = HotelConfig.getNombre();
 *
 * @author Fernando
 * @version 1.0
 */
public final class HotelConfig {

    private static final Logger log = LoggerFactory.getLogger(HotelConfig.class);
    private static final String ARCHIVO = "hotel.properties";

    // Valores por defecto (usados si el archivo no existe)
    private static final String DEF_NOMBRE    = "Hotel Vista";
    private static final String DEF_SLOGAN    = "Bienvenido a su hogar lejos del hogar";
    private static final String DEF_DIRECCION = "";
    private static final String DEF_TELEFONO  = "";
    private static final String DEF_EMAIL     = "";
    private static final String DEF_NIT       = "";
    private static final String DEF_WEB       = "";

    private static Properties props;

    static { recargar(); }

    private HotelConfig() {}

    // =========================================================
    // API DE LECTURA
    // =========================================================

    public static String getNombre()    { return get("hotel.nombre",    DEF_NOMBRE);    }
    public static String getSlogan()    { return get("hotel.slogan",    DEF_SLOGAN);    }
    public static String getDireccion() { return get("hotel.direccion", DEF_DIRECCION); }
    public static String getTelefono()  { return get("hotel.telefono",  DEF_TELEFONO);  }
    public static String getEmail()     { return get("hotel.email",     DEF_EMAIL);     }
    public static String getNit()       { return get("hotel.nit",       DEF_NIT);       }
    public static String getWeb()       { return get("hotel.web",       DEF_WEB);       }

    private static String get(String clave, String defecto) {
        return props.getProperty(clave, defecto).trim();
    }

    // =========================================================
    // PERSISTENCIA
    // =========================================================

    /**
     * Recarga los valores desde el archivo (o el classpath si no existe el archivo).
     * Se llama automáticamente al iniciar la clase y después de guardar.
     */
    public static void recargar() {
        Properties p = defaults();

        // 1. Intentar archivo en directorio de trabajo
        File f = new File(ARCHIVO);
        if (f.exists()) {
            try (InputStream is = new FileInputStream(f)) {
                p.load(is);
                log.debug("HotelConfig: cargado desde {}", f.getAbsolutePath());
            } catch (IOException e) {
                log.warn("HotelConfig: error leyendo archivo externo, usando classpath", e);
                cargarDesdeClasspath(p);
            }
        } else {
            cargarDesdeClasspath(p);
        }

        props = p;
    }

    /**
     * Persiste los datos del hotel.
     * Guarda en el directorio de trabajo y recarga la caché.
     *
     * @param nuevos Map con los nuevos valores (claves: hotel.nombre, etc.)
     * @throws IOException si no se puede escribir el archivo
     */
    public static void guardar(Properties nuevos) throws IOException {
        // Combinar: partimos de los actuales y sobreescribimos solo los enviados
        Properties merged = defaults();
        for (String k : props.stringPropertyNames()) {
            merged.setProperty(k, props.getProperty(k));
        }
        for (String k : nuevos.stringPropertyNames()) {
            merged.setProperty(k, nuevos.getProperty(k).trim());
        }

        File f = new File(ARCHIVO);
        try (OutputStream os = new FileOutputStream(f)) {
            merged.store(os, "Configuración del Hotel — Hotel Sistema");
        }
        log.info("HotelConfig: guardado en {}", f.getAbsolutePath());

        recargar();
    }

    // =========================================================
    // INTERNOS
    // =========================================================

    private static void cargarDesdeClasspath(Properties p) {
        try (InputStream is = HotelConfig.class
                .getClassLoader().getResourceAsStream(ARCHIVO)) {
            if (is != null) {
                p.load(is);
                log.debug("HotelConfig: cargado desde classpath");
            } else {
                log.info("HotelConfig: hotel.properties no encontrado — usando valores por defecto");
            }
        } catch (IOException e) {
            log.warn("HotelConfig: error leyendo classpath", e);
        }
    }

    private static Properties defaults() {
        Properties p = new Properties();
        p.setProperty("hotel.nombre",    DEF_NOMBRE);
        p.setProperty("hotel.slogan",    DEF_SLOGAN);
        p.setProperty("hotel.direccion", DEF_DIRECCION);
        p.setProperty("hotel.telefono",  DEF_TELEFONO);
        p.setProperty("hotel.email",     DEF_EMAIL);
        p.setProperty("hotel.nit",       DEF_NIT);
        p.setProperty("hotel.web",       DEF_WEB);
        return p;
    }
}
