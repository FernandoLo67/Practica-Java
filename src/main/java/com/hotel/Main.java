package com.hotel;

import com.hotel.vista.LoginForm;

import javax.swing.*;

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

        // Iniciar la interfaz gráfica en el hilo de eventos de Swing (EDT)
        // SIEMPRE se debe crear la UI en el EDT para evitar problemas de concurrencia
        SwingUtilities.invokeLater(() -> {
            LoginForm login = new LoginForm();
            login.setVisible(true);
            System.out.println("=== Hotel Sistema iniciado ===");
        });
    }
}
