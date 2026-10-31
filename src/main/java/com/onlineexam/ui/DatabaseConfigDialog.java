package com.onlineexam.ui;

import com.onlineexam.config.DatabaseConfig;
import com.onlineexam.config.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Diagnostic and runtime configuration dialog for MySQL database settings.
 * Allows users to test, configure, and verify connectivity interactively within the UI.
 */
public class DatabaseConfigDialog extends JDialog {

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(DatabaseConfigDialog.class.getName());

    private final JTextField urlField;
    private final JTextField userField;
    private final JPasswordField passwordField;
    private final JLabel statusLabel;
    private final JButton testBtn;
    private final JButton saveBtn;
    private final Runnable onSaveCallback;

    public DatabaseConfigDialog(Window owner) {
        this(owner, null);
    }

    public DatabaseConfigDialog(Window owner, Runnable onSaveCallback) {
        super(owner, "Database Connection Configuration & Diagnostics", ModalityType.APPLICATION_MODAL);
        this.onSaveCallback = onSaveCallback;
        setSize(580, 420);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BG_LIGHT);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(18, 24, 18, 24));

        JLabel titleLbl = new JLabel("Database Connectivity Settings");
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.PRIMARY);

        JLabel subtitleLbl = new JLabel("Configure MySQL 8+ connection parameters for persistent transactions.");
        subtitleLbl.setFont(UITheme.FONT_BODY);
        subtitleLbl.setForeground(UITheme.TEXT_MUTED);

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setOpaque(false);
        headerText.add(titleLbl);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(subtitleLbl);

        headerPanel.add(headerText, BorderLayout.CENTER);
        headerPanel.add(UITheme.createBadge("MySQL 8.0+", UITheme.ACCENT_LIGHT, UITheme.ACCENT), BorderLayout.EAST);

        // Form Card
        JPanel card = UITheme.createCard();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.25;
        JLabel urlLbl = new JLabel("JDBC URL:");
        urlLbl.setFont(UITheme.FONT_BOLD);
        card.add(urlLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.75;
        urlField = new JTextField(DatabaseConfig.getJdbcUrl());
        urlField.setFont(UITheme.FONT_BODY);
        card.add(urlField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.25;
        JLabel userLbl = new JLabel("Database User:");
        userLbl.setFont(UITheme.FONT_BOLD);
        card.add(userLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.75;
        userField = new JTextField(DatabaseConfig.getUsername());
        userField.setFont(UITheme.FONT_BODY);
        card.add(userField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.25;
        JLabel passLbl = new JLabel("DB Password:");
        passLbl.setFont(UITheme.FONT_BOLD);
        card.add(passLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.75;
        passwordField = new JPasswordField(DatabaseConfig.getPassword());
        passwordField.setFont(UITheme.FONT_BODY);
        card.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        statusLabel = new JLabel("Tip: Set MySQL password above and click 'Test Connection'.");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_MUTED);
        card.add(statusLabel, gbc);

        // Footer Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 15));
        footer.setOpaque(false);

        testBtn = UITheme.createButton("Test Connection", UITheme.SECONDARY, Color.WHITE, 12, true);
        saveBtn = UITheme.createButton("Apply & Save", UITheme.ACCENT, Color.WHITE, 12, true);
        JButton closeBtn = UITheme.createButton("Close", new Color(241, 245, 249), UITheme.TEXT_MAIN, 12, false);

        testBtn.addActionListener(e -> performConnectionTest());
        saveBtn.addActionListener(e -> {
            applySettings();
            if (onSaveCallback != null) {
                try {
                    onSaveCallback.run();
                } catch (Exception ex) {
                    LOGGER.log(java.util.logging.Level.WARNING, "Error running onSaveCallback", ex);
                }
            }
            JOptionPane.showMessageDialog(this, "Database parameters updated successfully.", "Settings Saved", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        });
        closeBtn.addActionListener(e -> dispose());

        footer.add(testBtn);
        footer.add(saveBtn);
        footer.add(closeBtn);

        add(headerPanel, BorderLayout.NORTH);
        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setOpaque(false);
        centerWrap.setBorder(new EmptyBorder(15, 24, 0, 24));
        centerWrap.add(card, BorderLayout.CENTER);
        add(centerWrap, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    private void applySettings() {
        String url = urlField.getText().trim();
        String user = userField.getText().trim();
        String pass = new String(passwordField.getPassword());
        DatabaseConfig.setRuntimeCredentials(url, user, pass);
    }

    private void performConnectionTest() {
        applySettings();
        testBtn.setEnabled(false);
        testBtn.setText("Testing...");
        statusLabel.setText("Connecting to database server...");
        statusLabel.setForeground(UITheme.ACCENT);

        new Thread(() -> {
            DatabaseConnection.ConnectionTestResult res = DatabaseConnection.testConnection();
            SwingUtilities.invokeLater(() -> {
                testBtn.setEnabled(true);
                testBtn.setText("Test Connection");
                if (res.successful()) {
                    statusLabel.setText(String.format("SUCCESS: Connected to %s (%d ms)", res.databaseName(), res.latencyMs()));
                    statusLabel.setForeground(UITheme.SUCCESS_DARK);
                    JOptionPane.showMessageDialog(this,
                            "Database Connected Successfully!\n\n" +
                            "Product: " + res.databaseProductName() + " " + res.databaseProductVersion() + "\n" +
                            "Catalog: " + res.databaseName() + "\n" +
                            "User: " + res.connectedUser() + "\n" +
                            "Latency: " + res.latencyMs() + " ms",
                            "Connection Verified",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    statusLabel.setText("FAILED: " + (res.cause() != null ? res.cause().getMessage() : "Unknown error"));
                    statusLabel.setForeground(UITheme.DANGER_DARK);
                    JOptionPane.showMessageDialog(this,
                            res.message(),
                            "Connection Failed",
                            JOptionPane.WARNING_MESSAGE);
                }
            });
        }).start();
    }
}
