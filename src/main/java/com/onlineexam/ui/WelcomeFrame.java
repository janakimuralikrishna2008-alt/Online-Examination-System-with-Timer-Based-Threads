package com.onlineexam.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Welcome Window for Online Examination System with Timer-Based Threads.
 * Displays the project title, runtime environment overview, and stage status.
 */
public class WelcomeFrame extends JFrame {

    public WelcomeFrame() {
        initUI();
    }

    private void initUI() {
        setTitle("Online Examination System with Timer-Based Threads");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(760, 520);
        setMinimumSize(new Dimension(640, 440));
        setLocationRelativeTo(null);

        // Root container with modern padding
        JPanel rootPanel = new JPanel(new BorderLayout(15, 15));
        rootPanel.setBorder(new EmptyBorder(25, 30, 25, 30));
        rootPanel.setBackground(new Color(248, 249, 250));

        // Header Section
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Online Examination System with Timer-Based Threads");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(new Color(24, 43, 73));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Dynamic Student Learning Hub, Practice Quizzes & Examination Platform");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(100, 116, 139));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(8));
        headerPanel.add(subtitleLabel);
        headerPanel.add(Box.createVerticalStrut(15));

        // Center Content Card
        JPanel cardPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(20, 25, 20, 25)
        ));

        // Section 1: Technology Specs
        JPanel specsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        specsPanel.setOpaque(false);
        specsPanel.add(createBadge("Java 21 LTS", new Color(230, 242, 255), new Color(12, 74, 110)));
        specsPanel.add(createBadge("Learning Hub", new Color(240, 253, 244), new Color(21, 128, 61)));
        specsPanel.add(createBadge("Topic Practice Quizzes", new Color(254, 243, 199), new Color(180, 83, 9)));
        specsPanel.add(createBadge("Formal Timed Exams", new Color(236, 253, 245), new Color(4, 120, 87)));
        specsPanel.add(createBadge("Admin Control Center", new Color(243, 232, 255), new Color(107, 33, 168)));

        // Section 2: Runtime Status
        JPanel statusPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        statusPanel.setOpaque(false);
        String javaVersion = System.getProperty("java.version", "Unknown");
        String osName = System.getProperty("os.name", "Windows");

        JLabel runtimeLabel = new JLabel("Runtime: Java " + javaVersion + " on " + osName + " | Complete Enterprise Desktop Platform", SwingConstants.CENTER);
        runtimeLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        runtimeLabel.setForeground(new Color(51, 65, 85));

        JLabel infoLabel = new JLabel("Subjects, Topics, Rich Study Notes, Attachments, Practice Quizzes & Multi-Threaded Timers operational.", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        infoLabel.setForeground(new Color(100, 116, 139));

        statusPanel.add(runtimeLabel);
        statusPanel.add(infoLabel);

        // Section 3: Completed Stages
        JPanel hintPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        hintPanel.setOpaque(false);
        JLabel nextStepsLabel = new JLabel("✓ All Phases Complete: Database Migrations, Multi-Threaded Timers, Learning Hub & Dual Swing Portals.");
        nextStepsLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nextStepsLabel.setForeground(new Color(5, 150, 105));
        hintPanel.add(nextStepsLabel);


        cardPanel.add(specsPanel);
        cardPanel.add(statusPanel);
        cardPanel.add(hintPanel);

        // Bottom Action Panel
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        footerPanel.setOpaque(false);

        JButton enterPortalButton = new JButton("Enter Exam Portal ►");
        enterPortalButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        enterPortalButton.setBackground(new Color(79, 70, 229)); // Indigo
        enterPortalButton.setForeground(Color.WHITE);
        enterPortalButton.setFocusPainted(false);
        enterPortalButton.addActionListener(e -> {
            dispose();
            com.onlineexam.service.AuthService authService = new com.onlineexam.service.AuthService();
            com.onlineexam.service.ExamService examService = new com.onlineexam.service.ExamService();
            com.onlineexam.service.AttemptService attemptService = new com.onlineexam.service.AttemptService();
            LoginFrame loginFrame = new LoginFrame(authService, examService, attemptService);
            loginFrame.setVisible(true);
        });

        JButton timerDemoButton = new JButton("Launch Timer Demo");
        timerDemoButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        timerDemoButton.setBackground(new Color(241, 245, 249));
        timerDemoButton.setForeground(new Color(15, 23, 42));
        timerDemoButton.setFocusPainted(false);
        timerDemoButton.addActionListener(e -> {
            TimerDemoDialog dialog = new TimerDemoDialog(this);
            dialog.setVisible(true);
        });

        JButton testDbButton = new JButton("DB Diagnostics");
        testDbButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        testDbButton.setBackground(new Color(15, 118, 110));
        testDbButton.setForeground(Color.WHITE);
        testDbButton.setFocusPainted(false);
        testDbButton.addActionListener(e -> {
            DatabaseConfigDialog dialog = new DatabaseConfigDialog(this);
            dialog.setVisible(true);
        });

        JButton exitButton = new JButton("Exit");
        exitButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        exitButton.setFocusPainted(false);
        exitButton.addActionListener(e -> System.exit(0));

        footerPanel.add(enterPortalButton);
        footerPanel.add(timerDemoButton);
        footerPanel.add(testDbButton);
        footerPanel.add(exitButton);

        // Assemble root
        rootPanel.add(headerPanel, BorderLayout.NORTH);
        rootPanel.add(cardPanel, BorderLayout.CENTER);
        rootPanel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(rootPanel);
    }

    private JLabel createBadge(String text, Color bg, Color fg) {
        JLabel badge = new JLabel(text);
        badge.setOpaque(true);
        badge.setBackground(bg);
        badge.setForeground(fg);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));
        return badge;
    }
}
