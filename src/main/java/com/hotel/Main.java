package com.hotel;

import com.hotel.util.ConexionDB;
import com.hotel.util.HotelConfig;
import com.hotel.vista.LoginForm;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Punto de entrada principal del Sistema de Hotel.
 *
 * Esta clase contiene el método main() que inicia la aplicación.
 *
 * CÓMO EJECUTAR:
 *   1. Abre el proyecto en NetBeans
 *   2. Clic derecho en el proyecto → "Clean and Build"
 *   3. Asegúrate de que MySQL esté corriendo
 *   4. Ejecuta la clase Main (botón verde de Play)
 *
 * REQUISITOS PREVIOS:
 *   1. MySQL instalado y corriendo en puerto 3306
 *   2. Haber ejecutado el script: sql/hotel_sistema.sql
 *   3. Haber configurado la contraseña en ConexionDB.java
 *
 * @author Fernando
 * @version 1.0
 */
public class Main {

    public static void main(String[] args) {

        // Configurar el Look and Feel del sistema operativo
        // Esto hace que los componentes Swing se vean como en Windows/Mac/Linux
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Si no se puede aplicar, usa el Look and Feel por defecto de Java
            System.out.println("Usando Look and Feel por defecto: " + e.getMessage());
        }

        // Personalizar algunos componentes globales de Swing
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);

        // Cerrar el pool HikariCP cuando la JVM se apague (Ctrl+C, botón Stop, etc.)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ConexionDB.cerrarPool();
            System.out.println("=== Hotel Sistema cerrado ===");
        }, "shutdown-hook"));

        // Verificar conexión a la BD antes de abrir la UI
        try {
            ConexionDB.getConexion().close(); // solo para verificar
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> mostrarErrorConexion(e));
            return;
        }

        // Iniciar la interfaz gráfica en el hilo de eventos de Swing (EDT)
        // SIEMPRE se debe crear la UI en el EDT para evitar problemas de concurrencia
        SwingUtilities.invokeLater(() -> {
            LoginForm login = new LoginForm();
            login.setVisible(true);
            System.out.println("=== " + HotelConfig.getNombre() + " iniciado ===");
        });
    }

    /**
     * Muestra un diálogo amigable cuando no se puede conectar a la base de datos.
     */
    private static void mostrarErrorConexion(Exception causa) {
        JDialog dlg = new JDialog((Frame) null, "Error de conexion", true);
        dlg.setLayout(new BorderLayout());
        dlg.setSize(490, 250);
        dlg.setLocationRelativeTo(null);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(183, 28, 28));
        header.setBorder(new EmptyBorder(16, 20, 16, 20));
        JLabel titulo = new JLabel("  No se pudo conectar a la base de datos");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titulo.setForeground(Color.WHITE);
        header.add(titulo, BorderLayout.WEST);

        JPanel cuerpo = new JPanel();
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));
        cuerpo.setBackground(new Color(255, 245, 245));
        cuerpo.setBorder(new EmptyBorder(18, 24, 18, 24));

        String[] pasos = {
            "1. Verifica que MySQL Server este en ejecucion.",
            "2. Revisa las credenciales en src/main/resources/database.properties.",
            "3. Asegurate de que el schema hotel_sistema exista.",
            "   Error: " + (causa.getMessage() != null ? causa.getMessage().split("\n")[0] : causa.getClass().getSimpleName())
        };
        for (String texto : pasos) {
            JLabel l = new JLabel(texto);
            l.setFont(new Font("Segoe UI", texto.startsWith("   ") ? Font.ITALIC : Font.PLAIN, 12));
            l.setForeground(new Color(100, 30, 30));
            l.setBorder(new EmptyBorder(2, 0, 2, 0));
            cuerpo.add(l);
        }

        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 8));
        pie.setBackground(new Color(245, 245, 250));
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCerrar.addActionListener(e -> { dlg.dispose(); System.exit(1); });
        pie.add(btnCerrar);

        dlg.add(header, BorderLayout.NORTH);
        dlg.add(cuerpo, BorderLayout.CENTER);
        dlg.add(pie,    BorderLayout.SOUTH);
        dlg.setVisible(true);
    }
}
