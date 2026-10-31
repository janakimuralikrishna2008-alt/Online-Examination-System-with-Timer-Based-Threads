package com.onlineexam.ui;

import com.onlineexam.timer.ExamTimerListener;
import com.onlineexam.timer.ExamTimerService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Diagnostic demonstration window for the Multi-Threaded Exam Timer Subsystem.
 * <p>
 * Demonstrates thread-safe countdowns using {@link ScheduledExecutorService},
 * nanoTime deadline calculation, duplicate submission prevention, and EDT UI synchronization.
 * </p>
 */
public class TimerDemoDialog extends JDialog implements ExamTimerListener {

    private final JLabel countdownLabel = new JLabel("00:15", SwingConstants.CENTER);
    private final JProgressBar progressBar = new JProgressBar(0, 15);
    private final JLabel statusLabel = new JLabel("Status: Ready to Start", SwingConstants.CENTER);
    private final JTextArea logArea = new JTextArea(7, 40);

    private final JButton startButton = new JButton("Start Countdown (15s)");
    private final JButton manualSubmitButton = new JButton("Manual Submit");
    private final JButton cancelButton = new JButton("Cancel Timer");

    private ExamTimerService timerService;
    private static final long DEMO_DURATION = 15; // 15 seconds demo

    public TimerDemoDialog(Frame owner) {
        super(owner, "Timer Demonstration Test (ScheduledExecutorService)", true);
        initUI();
    }

    private void initUI() {
        setSize(580, 520);
        setMinimumSize(new Dimension(520, 460));
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(15, 18, 15, 18));
        root.setBackground(new Color(248, 249, 250));

        // 1. Notice Banner
        JPanel bannerPanel = new JPanel(new BorderLayout(8, 8));
        bannerPanel.setBackground(new Color(254, 243, 199));
        bannerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(217, 119, 6), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel bannerTitle = new JLabel("DIAGNOSTIC TIMER TEST (STAGE 3)");
        bannerTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        bannerTitle.setForeground(new Color(146, 64, 14));

        JLabel bannerText = new JLabel("<html>This is a thread-safety demonstration testing ScheduledExecutorService & System.nanoTime() countdowns.<br>It is a standalone testing component, not the full student exam interface.</html>");
        bannerText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        bannerText.setForeground(new Color(120, 53, 15));

        bannerPanel.add(bannerTitle, BorderLayout.NORTH);
        bannerPanel.add(bannerText, BorderLayout.CENTER);

        // 2. Center Card - Timer Display
        JPanel displayCard = new JPanel();
        displayCard.setLayout(new BoxLayout(displayCard, BoxLayout.Y_AXIS));
        displayCard.setBackground(Color.WHITE);
        displayCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(15, 20, 15, 20)
        ));

        countdownLabel.setFont(new Font("Consolas", Font.BOLD, 46));
        countdownLabel.setForeground(new Color(30, 41, 59));
        countdownLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        progressBar.setValue((int) DEMO_DURATION);
        progressBar.setStringPainted(true);
        progressBar.setForeground(new Color(37, 99, 235));
        progressBar.setMaximumSize(new Dimension(420, 20));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusLabel.setForeground(new Color(71, 85, 105));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        displayCard.add(Box.createVerticalStrut(5));
        displayCard.add(countdownLabel);
        displayCard.add(Box.createVerticalStrut(10));
        displayCard.add(progressBar);
        displayCard.add(Box.createVerticalStrut(10));
        displayCard.add(statusLabel);
        displayCard.add(Box.createVerticalStrut(5));

        // 3. Controls Panel
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        controlsPanel.setOpaque(false);

        startButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        startButton.setBackground(new Color(37, 99, 235));
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);
        startButton.addActionListener(e -> startTimerDemo());

        manualSubmitButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        manualSubmitButton.setEnabled(false);
        manualSubmitButton.setFocusPainted(false);
        manualSubmitButton.addActionListener(e -> {
            if (timerService != null) {
                logMessage("User initiated manual submission...");
                boolean submitted = timerService.manualSubmit();
                if (!submitted) {
                    logMessage("Duplicate submission attempt prevented by AtomicBoolean guard.");
                }
            }
        });

        cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cancelButton.setEnabled(false);
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(e -> {
            if (timerService != null) {
                timerService.cancel();
            }
        });

        controlsPanel.add(startButton);
        controlsPanel.add(manualSubmitButton);
        controlsPanel.add(cancelButton);

        // 4. Real-time Event Log Panel
        JPanel logPanel = new JPanel(new BorderLayout(5, 5));
        logPanel.setOpaque(false);

        JLabel logHeader = new JLabel("Thread Execution Log:");
        logHeader.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logHeader.setForeground(new Color(71, 85, 105));

        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setBackground(new Color(241, 245, 249));
        JScrollPane scrollPane = new JScrollPane(logArea);

        logPanel.add(logHeader, BorderLayout.NORTH);
        logPanel.add(scrollPane, BorderLayout.CENTER);

        // Assemble Dialog
        JPanel centerContainer = new JPanel(new BorderLayout(10, 10));
        centerContainer.setOpaque(false);
        centerContainer.add(displayCard, BorderLayout.NORTH);
        centerContainer.add(controlsPanel, BorderLayout.CENTER);
        centerContainer.add(logPanel, BorderLayout.SOUTH);

        root.add(bannerPanel, BorderLayout.NORTH);
        root.add(centerContainer, BorderLayout.CENTER);

        setContentPane(root);

        // Clean up thread resources if dialog is closed
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (timerService != null) {
                    timerService.shutdown();
                }
            }
        });

        logMessage("Timer demonstration initialized. Ready to test 15-second countdown.");
    }

    private void startTimerDemo() {
        if (timerService != null && timerService.isRunning()) {
            timerService.shutdown();
        }

        logMessage("Starting ScheduledExecutorService thread pool...");
        progressBar.setMaximum((int) DEMO_DURATION);
        progressBar.setValue((int) DEMO_DURATION);
        countdownLabel.setForeground(new Color(30, 41, 59));

        // Attempt ID -1 for standalone UI testing
        timerService = new ExamTimerService(-1L, DEMO_DURATION, null, null);
        timerService.addListener(this);

        try {
            timerService.start();
            startButton.setEnabled(false);
            manualSubmitButton.setEnabled(true);
            cancelButton.setEnabled(true);
            statusLabel.setText("Status: Countdown in progress (Active Thread)");
            logMessage("Timer started. Authoritative deadline calculated with System.nanoTime().");
        } catch (Exception ex) {
            logMessage("Error starting timer: " + ex.getMessage());
        }
    }

    // ExamTimerListener implementations (Guaranteed by ExamTimerService to run on Swing EDT)

    @Override
    public void onTick(long remainingSeconds, String formattedTime) {
        countdownLabel.setText(formattedTime);
        progressBar.setValue((int) remainingSeconds);
        progressBar.setString(remainingSeconds + "s remaining");

        if (remainingSeconds <= 5) {
            countdownLabel.setForeground(new Color(220, 38, 38)); // Warning Red
            statusLabel.setText("Status: Time running out! (" + remainingSeconds + "s)");
        }
    }

    @Override
    public void onWarning(long remainingSeconds) {
        if (remainingSeconds == 5) {
            logMessage("WARNING: Threshold alert reached (5s remaining).");
        }
    }

    @Override
    public void onTimeExpired() {
        countdownLabel.setText("00:00");
        progressBar.setValue(0);
        statusLabel.setText("Status: TIME EXPIRED - AUTO-SUBMITTING");
        countdownLabel.setForeground(new Color(185, 28, 28));
        logMessage(">> DEADLINE EXPIRED: auto-submission event fired!");
    }

    @Override
    public void onSubmitted(boolean autoSubmitted) {
        startButton.setEnabled(true);
        manualSubmitButton.setEnabled(false);
        cancelButton.setEnabled(false);

        String mode = autoSubmitted ? "AUTOMATIC (Time Expired)" : "MANUAL (Student Clicked Submit)";
        statusLabel.setText("Status: Successfully Submitted [" + mode + "]");
        logMessage(">> SUBMISSION CONFIRMED: Mode = " + mode);
        logMessage("Timer thread cleaned up. Double-submission lock held.");
    }

    @Override
    public void onCancelled() {
        startButton.setEnabled(true);
        manualSubmitButton.setEnabled(false);
        cancelButton.setEnabled(false);
        statusLabel.setText("Status: Timer Cancelled");
        logMessage("Timer cancelled by user. Background tasks stopped.");
    }

    @Override
    public void onError(Throwable error) {
        logMessage("ERROR in timer thread: " + error.getMessage());
    }

    private void logMessage(String message) {
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        logArea.append("[" + timestamp + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
