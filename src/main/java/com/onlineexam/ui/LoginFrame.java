package com.onlineexam.ui;

import com.onlineexam.model.User;
import com.onlineexam.service.AttemptService;
import com.onlineexam.service.AuthService;
import com.onlineexam.service.ExamService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Optional;

/**
 * Modern Portal Authentication Window.
 * Supports Student and Administrator sign-in, account self-registration,
 * one-click demo credentials filling, and interactive database diagnostic launcher.
 */
public class LoginFrame extends JFrame {

    private final AuthService authService;
    private final ExamService examService;
    private final AttemptService attemptService;

    // Sign in components
    private JTextField loginIdentifierField;
    private JPasswordField loginPasswordField;
    private JButton signInButton;

    // Registration components
    private JTextField regNameField;
    private JTextField regUsernameField;
    private JTextField regEmailField;
    private JPasswordField regPasswordField;
    private JPasswordField regConfirmPasswordField;
    private JButton registerButton;

    public LoginFrame(AuthService authService, ExamService examService, AttemptService attemptService) {
        this.authService = authService;
        this.examService = examService;
        this.attemptService = attemptService;

        initUI();
    }

    private void initUI() {
        setTitle("Online Examination System - Portal Sign In");
        setSize(540, 640);
        setMinimumSize(new Dimension(500, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_LIGHT);

        // Header
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR),
                new EmptyBorder(22, 25, 20, 25)
        ));

        JLabel titleLbl = new JLabel("Online Examination System");
        titleLbl.setFont(UITheme.FONT_TITLE_LARGE);
        titleLbl.setForeground(UITheme.PRIMARY);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLbl = new JLabel("Secure Student & Administrator Examination Portal");
        subLbl.setFont(UITheme.FONT_BODY);
        subLbl.setForeground(UITheme.TEXT_MUTED);
        subLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(titleLbl);
        header.add(Box.createVerticalStrut(4));
        header.add(subLbl);

        // Center Tabs: Sign In vs Register
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BOLD);
        tabbedPane.setBackground(Color.WHITE);

        tabbedPane.addTab("Sign In", buildSignInPanel());
        tabbedPane.addTab("Create Account", buildRegisterPanel());

        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setOpaque(false);
        centerWrap.setBorder(new EmptyBorder(15, 20, 10, 20));
        centerWrap.add(tabbedPane, BorderLayout.CENTER);

        // Footer Bar
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 12));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR));

        JButton dbBtn = UITheme.createButton("Configure / Test DB", UITheme.SECONDARY, Color.WHITE, 12, false);
        JButton welcomeBtn = UITheme.createButton("Overview Screen", new Color(241, 245, 249), UITheme.TEXT_MAIN, 12, false);

        dbBtn.addActionListener(e -> new DatabaseConfigDialog(this).setVisible(true));
        welcomeBtn.addActionListener(e -> {
            dispose();
            new WelcomeFrame().setVisible(true);
        });

        footer.add(dbBtn);
        footer.add(welcomeBtn);

        root.add(header, BorderLayout.NORTH);
        root.add(centerWrap, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel buildSignInPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        // Form Fields
        JPanel form = new JPanel(new GridLayout(4, 1, 6, 6));
        form.setOpaque(false);

        JLabel idLbl = new JLabel("Username or Email:");
        idLbl.setFont(UITheme.FONT_BOLD);
        loginIdentifierField = new JTextField();
        loginIdentifierField.setFont(UITheme.FONT_BODY);

        JLabel passLbl = new JLabel("Password:");
        passLbl.setFont(UITheme.FONT_BOLD);
        loginPasswordField = new JPasswordField();
        loginPasswordField.setFont(UITheme.FONT_BODY);

        form.add(idLbl);
        form.add(loginIdentifierField);
        form.add(passLbl);
        form.add(loginPasswordField);

        // Demo Credentials Quick Fill Box
        JPanel demoBox = new JPanel(new BorderLayout(5, 5));
        demoBox.setBackground(new Color(248, 250, 252));
        demoBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel demoTitle = new JLabel("One-Click Quick Fill Demo Credentials:");
        demoTitle.setFont(UITheme.FONT_SMALL);
        demoTitle.setForeground(UITheme.TEXT_MUTED);

        JPanel demoBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        demoBtns.setOpaque(false);

        JButton studentDemoBtn = UITheme.createButton("Alice (Student)", UITheme.SUCCESS_LIGHT, UITheme.SUCCESS_DARK, 11, true);
        JButton adminDemoBtn = UITheme.createButton("System Admin", UITheme.ACCENT_LIGHT, UITheme.ACCENT, 11, true);

        studentDemoBtn.addActionListener(e -> {
            loginIdentifierField.setText("student1");
            loginPasswordField.setText("student123");
        });

        adminDemoBtn.addActionListener(e -> {
            loginIdentifierField.setText("admin");
            loginPasswordField.setText("admin123");
        });

        demoBtns.add(studentDemoBtn);
        demoBtns.add(adminDemoBtn);

        demoBox.add(demoTitle, BorderLayout.NORTH);
        demoBox.add(demoBtns, BorderLayout.CENTER);

        // Sign In Action Button
        signInButton = UITheme.createButton("Sign In to Portal ►", UITheme.ACCENT, Color.WHITE, 14, true);
        signInButton.setPreferredSize(new Dimension(0, 42));
        signInButton.addActionListener(e -> handleSignIn());

        // Support Enter key for login
        loginPasswordField.addActionListener(e -> handleSignIn());
        loginIdentifierField.addActionListener(e -> handleSignIn());

        JPanel centerWrap = new JPanel(new BorderLayout(0, 15));
        centerWrap.setOpaque(false);
        centerWrap.add(form, BorderLayout.NORTH);
        centerWrap.add(demoBox, BorderLayout.CENTER);

        panel.add(centerWrap, BorderLayout.CENTER);
        panel.add(signInButton, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildRegisterPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 25, 15, 25));

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 10));
        form.setOpaque(false);

        regNameField = new JTextField();
        regUsernameField = new JTextField();
        regEmailField = new JTextField();
        regPasswordField = new JPasswordField();
        regConfirmPasswordField = new JPasswordField();

        form.add(new JLabel("Full Name:")); form.add(regNameField);
        form.add(new JLabel("Username:")); form.add(regUsernameField);
        form.add(new JLabel("Email Address:")); form.add(regEmailField);
        form.add(new JLabel("Password:")); form.add(regPasswordField);
        form.add(new JLabel("Confirm Password:")); form.add(regConfirmPasswordField);

        registerButton = UITheme.createButton("Create & Register Student Account", UITheme.SUCCESS_DARK, Color.WHITE, 13, true);
        registerButton.setPreferredSize(new Dimension(0, 40));
        registerButton.addActionListener(e -> handleRegister());

        panel.add(form, BorderLayout.CENTER);
        panel.add(registerButton, BorderLayout.SOUTH);
        return panel;
    }

    private void handleSignIn() {
        String identifier = loginIdentifierField.getText().trim();
        String password = new String(loginPasswordField.getPassword());

        if (identifier.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both username/email and password.",
                    "Input Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        signInButton.setEnabled(false);
        signInButton.setText("Verifying Credentials...");

        new Thread(() -> {
            Optional<User> userOpt = authService.authenticate(identifier, password);
            SwingUtilities.invokeLater(() -> {
                signInButton.setEnabled(true);
                signInButton.setText("Sign In to Portal ►");

                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    dispose();

                    if (!authService.isLastAuthUsedDatabase()) {
                        String dbErr = authService.getLastDatabaseError();
                        String warningMsg = "Notice: MySQL database is disconnected" +
                                (dbErr != null && !dbErr.isBlank() ? " (" + dbErr + ")" : "") + ".\n\n" +
                                "You are operating in offline demonstration mode.\n" +
                                "To access persistent database examinations, click 'DB Settings' on the dashboard\n" +
                                "and configure your MySQL password.";
                        JOptionPane.showMessageDialog(null, warningMsg, "Offline Demonstration Mode", JOptionPane.WARNING_MESSAGE);
                    }

                    if (user.getRole() == User.Role.ADMIN) {
                        AdminDashboardFrame adminFrame = new AdminDashboardFrame(user, authService, examService, attemptService);
                        adminFrame.setVisible(true);
                    } else {
                        StudentDashboardFrame studentFrame = new StudentDashboardFrame(user, authService, examService, attemptService);
                        studentFrame.setVisible(true);
                    }
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Invalid credentials. Please verify your username and password.\n\n" +
                            "Tip: Click the quick-fill demo buttons to sign in with pre-seeded accounts.",
                            "Authentication Failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();
    }

    private void handleRegister() {
        String fullName = regNameField.getText().trim();
        String username = regUsernameField.getText().trim();
        String email = regEmailField.getText().trim();
        String pass = new String(regPasswordField.getPassword());
        String confirmPass = new String(regConfirmPasswordField.getPassword());

        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required for registration.", "Missing Fields", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!pass.equals(confirmPass)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Password Mismatch", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Public candidate registration strictly creates STUDENT role accounts
            User registered = authService.registerStudent(username, pass, fullName, email);
            JOptionPane.showMessageDialog(this,
                    "Account registered successfully for " + registered.getUsername() + "!\nYou may now sign in.",
                    "Registration Successful",
                    JOptionPane.INFORMATION_MESSAGE);

            // Pre-fill sign in tab
            loginIdentifierField.setText(username);
            loginPasswordField.setText(pass);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Registration failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
