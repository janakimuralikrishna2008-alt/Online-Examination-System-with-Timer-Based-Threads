package com.onlineexam;

import com.onlineexam.ui.WelcomeFrame;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point for Online Examination System with Timer-Based Threads.
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // Initialize Look and Feel (FlatLaf if present, otherwise platform system L&F)
        initializeLookAndFeel();

        // Launch UI on Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                LOGGER.info("Starting Online Examination System with Timer-Based Threads...");
                boolean directPortal = args.length > 0 && (args[0].equalsIgnoreCase("--login") || args[0].equalsIgnoreCase("--portal"));
                if (directPortal) {
                    com.onlineexam.service.AuthService authService = new com.onlineexam.service.AuthService();
                    com.onlineexam.service.ExamService examService = new com.onlineexam.service.ExamService();
                    com.onlineexam.service.AttemptService attemptService = new com.onlineexam.service.AttemptService();
                    new com.onlineexam.ui.LoginFrame(authService, examService, attemptService).setVisible(true);
                } else {
                    WelcomeFrame welcomeFrame = new WelcomeFrame();
                    welcomeFrame.setVisible(true);
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to initialize application window", ex);
                JOptionPane.showMessageDialog(
                        null,
                        "Error starting application: " + ex.getMessage(),
                        "Startup Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }

    private static void initializeLookAndFeel() {
        try {
            // Attempt to load modern FlatLaf Light Look & Feel if available on classpath
            Class<?> flatLafClass = Class.forName("com.formdev.flatlaf.FlatLightLaf");
            UIManager.setLookAndFeel((LookAndFeel) flatLafClass.getDeclaredConstructor().newInstance());
        } catch (ClassNotFoundException e) {
            // Fall back to native operating system Look & Feel
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "Falling back to default Swing look and feel", ex);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not set FlatLaf Look and Feel, using default", e);
        }
    }
}
