package com.onlineexam.ui;

import com.onlineexam.model.Exam;
import com.onlineexam.model.ExamResult;
import com.onlineexam.model.Question;
import com.onlineexam.model.StudentExam;
import com.onlineexam.model.User;
import com.onlineexam.service.AttemptService;
import com.onlineexam.timer.ExamTimerListener;
import com.onlineexam.timer.ExamTimerService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enterprise Multi-Threaded Examination Taking Interface.
 * <p>
 * Connects directly to {@link ExamTimerService} with drift-free thread countdowns,
 * real-time palette tracking (Answered, Flagged, Unanswered), instant background
 * answer persistence, and double-submission protected manual / auto-submission.
 * </p>
 */
public class ExamTakingFrame extends JFrame implements ExamTimerListener {

    private static final Logger LOGGER = Logger.getLogger(ExamTakingFrame.class.getName());

    private final User student;
    private final Exam exam;
    private final StudentExam attempt;
    private final List<Question> questions;
    private final AttemptService attemptService;
    private final Window parentDashboard;

    private ExamTimerService timerService;
    private int currentQuestionIndex = 0;
    private final long totalDurationSeconds;

    // Local answer state and flagged questions
    private final Map<Long, Question.Option> userAnswers = new ConcurrentHashMap<>();
    private final Set<Long> flaggedQuestions = Collections.synchronizedSet(new HashSet<>());

    // UI Components - Top Header
    private JLabel timerLabel;
    private JProgressBar timerProgressBar;
    private JPanel timerCard;

    // UI Components - Navigation Palette
    private JButton[] paletteButtons;
    private JLabel answeredCountLabel;
    private JLabel flaggedCountLabel;
    private JLabel unansweredCountLabel;

    // UI Components - Active Question Workspace
    private JLabel questionHeaderLabel;
    private JLabel marksBadge;
    private JCheckBox flagCheckbox;
    private JTextArea questionTextArea;
    private JRadioButton optionARadio;
    private JRadioButton optionBRadio;
    private JRadioButton optionCRadio;
    private JRadioButton optionDRadio;
    private ButtonGroup optionGroup;
    private JButton prevButton;
    private JButton nextButton;
    private JButton clearButton;
    private JButton submitButton;

    public ExamTakingFrame(Window parentDashboard,
                           User student,
                           Exam exam,
                           StudentExam attempt,
                           List<Question> questions,
                           AttemptService attemptService) {
        this.parentDashboard = parentDashboard;
        this.student = student;
        this.exam = exam;
        this.attempt = attempt;
        this.questions = (questions != null && !questions.isEmpty()) ? questions : List.of();
        this.attemptService = attemptService;
        this.totalDurationSeconds = Math.max(1, attempt.getTimeRemainingSeconds());

        // Preload any previously saved answers from database
        try {
            Map<Long, Question.Option> saved = attemptService.getSavedAnswers(attempt.getId());
            if (saved != null) {
                userAnswers.putAll(saved);
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not load prior answers", e);
        }

        initUI();
        startTimer();
    }

    private void initUI() {
        setTitle("Online Examination System - " + exam.getTitle() + " | Candidate: " + student.getFullName());
        setSize(1120, 750);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmEarlyExit();
            }
        });

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_LIGHT);

        // 1. Top Header Bar (Timer + Title + Submit Button)
        root.add(buildHeaderBar(), BorderLayout.NORTH);

        // 2. Center Content Split: Left Navigation Palette + Right Question View
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildPalettePanel(), buildQuestionPanel());
        splitPane.setDividerLocation(300);
        splitPane.setDividerSize(6);
        splitPane.setBorder(null);
        root.add(splitPane, BorderLayout.CENTER);

        setContentPane(root);
        displayQuestion(0);
    }

    private JPanel buildHeaderBar() {
        JPanel header = new JPanel(new BorderLayout(15, 10));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR),
                new EmptyBorder(12, 20, 12, 20)
        ));

        // Left info: Exam Title & Candidate
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);

        JLabel titleLbl = new JLabel(exam.getTitle());
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.PRIMARY);

        JLabel candidateLbl = new JLabel("Candidate: " + student.getFullName() + " (" + student.getUsername() + ")");
        candidateLbl.setFont(UITheme.FONT_BODY);
        candidateLbl.setForeground(UITheme.TEXT_MUTED);

        leftPanel.add(titleLbl);
        leftPanel.add(Box.createVerticalStrut(2));
        leftPanel.add(candidateLbl);

        // Center info: Countdown Timer Card
        timerCard = new JPanel();
        timerCard.setLayout(new BoxLayout(timerCard, BoxLayout.Y_AXIS));
        timerCard.setBackground(new Color(248, 250, 252));
        timerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1, true),
                new EmptyBorder(6, 20, 6, 20)
        ));

        timerLabel = new JLabel(UITheme.formatSeconds(totalDurationSeconds), SwingConstants.CENTER);
        timerLabel.setFont(UITheme.FONT_DIGITAL);
        timerLabel.setForeground(UITheme.PRIMARY);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        timerProgressBar = new JProgressBar(0, (int) totalDurationSeconds);
        timerProgressBar.setValue((int) totalDurationSeconds);
        timerProgressBar.setPreferredSize(new Dimension(160, 6));
        timerProgressBar.setForeground(UITheme.SUCCESS_DARK);
        timerProgressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        timerCard.add(timerLabel);
        timerCard.add(Box.createVerticalStrut(4));
        timerCard.add(timerProgressBar);

        // Right info: Submit Exam Button
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        rightPanel.setOpaque(false);

        submitButton = UITheme.createButton("Submit Exam", UITheme.DANGER, Color.WHITE, 14, true);
        submitButton.addActionListener(e -> promptManualSubmission());
        rightPanel.add(submitButton);

        header.add(leftPanel, BorderLayout.WEST);
        header.add(timerCard, BorderLayout.CENTER);
        header.add(rightPanel, BorderLayout.EAST);
        return header;
    }

    private JPanel buildPalettePanel() {
        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.setBackground(Color.WHITE);
        container.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Legend Summary
        JPanel legendPanel = new JPanel(new GridLayout(3, 1, 6, 6));
        legendPanel.setOpaque(false);
        legendPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));

        answeredCountLabel = new JLabel("Answered: 0 / " + questions.size());
        answeredCountLabel.setFont(UITheme.FONT_BOLD);
        answeredCountLabel.setForeground(UITheme.SUCCESS_DARK);

        flaggedCountLabel = new JLabel("Flagged for Review: 0");
        flaggedCountLabel.setFont(UITheme.FONT_BOLD);
        flaggedCountLabel.setForeground(UITheme.WARNING_DARK);

        unansweredCountLabel = new JLabel("Unanswered: " + questions.size());
        unansweredCountLabel.setFont(UITheme.FONT_BOLD);
        unansweredCountLabel.setForeground(UITheme.SECONDARY);

        legendPanel.add(answeredCountLabel);
        legendPanel.add(flaggedCountLabel);
        legendPanel.add(unansweredCountLabel);

        // Question Buttons Grid
        JPanel gridPanel = new JPanel(new GridLayout(0, 4, 8, 8));
        gridPanel.setOpaque(false);

        paletteButtons = new JButton[questions.size()];
        for (int i = 0; i < questions.size(); i++) {
            final int index = i;
            JButton btn = new JButton(String.valueOf(i + 1));
            btn.setFont(UITheme.FONT_BOLD);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setPreferredSize(new Dimension(50, 42));
            btn.addActionListener(e -> displayQuestion(index));
            paletteButtons[i] = btn;
            gridPanel.add(btn);
        }

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        JLabel paletteTitle = new JLabel("Question Navigation");
        paletteTitle.setFont(UITheme.FONT_SUBTITLE);
        paletteTitle.setForeground(UITheme.PRIMARY);

        JPanel topWrap = new JPanel(new BorderLayout(0, 10));
        topWrap.setOpaque(false);
        topWrap.add(paletteTitle, BorderLayout.NORTH);
        topWrap.add(legendPanel, BorderLayout.SOUTH);

        container.add(topWrap, BorderLayout.NORTH);
        container.add(scrollPane, BorderLayout.CENTER);
        return container;
    }

    private JPanel buildQuestionPanel() {
        JPanel container = new JPanel(new BorderLayout(15, 15));
        container.setBackground(UITheme.BG_LIGHT);
        container.setBorder(new EmptyBorder(20, 25, 20, 25));

        // Question Header (Question X of N, Marks badge, Flag for Review toggle)
        JPanel qHeader = new JPanel(new BorderLayout(10, 10));
        qHeader.setOpaque(false);

        questionHeaderLabel = new JLabel("Question 1 of " + questions.size());
        questionHeaderLabel.setFont(UITheme.FONT_TITLE);
        questionHeaderLabel.setForeground(UITheme.PRIMARY);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);

        marksBadge = UITheme.createBadge("+1 Mark", UITheme.ACCENT_LIGHT, UITheme.ACCENT);
        flagCheckbox = new JCheckBox("Flag for Review");
        flagCheckbox.setFont(UITheme.FONT_BOLD);
        flagCheckbox.setForeground(UITheme.WARNING_DARK);
        flagCheckbox.setOpaque(false);
        flagCheckbox.addActionListener(e -> toggleFlagCurrentQuestion());

        headerRight.add(marksBadge);
        headerRight.add(flagCheckbox);

        qHeader.add(questionHeaderLabel, BorderLayout.WEST);
        qHeader.add(headerRight, BorderLayout.EAST);

        // Center Card (Question Text + Options)
        JPanel centerCard = UITheme.createCard();
        centerCard.setLayout(new BorderLayout(15, 15));

        questionTextArea = new JTextArea();
        questionTextArea.setFont(UITheme.FONT_QUESTION);
        questionTextArea.setWrapStyleWord(true);
        questionTextArea.setLineWrap(true);
        questionTextArea.setEditable(false);
        questionTextArea.setOpaque(false);
        questionTextArea.setBorder(new EmptyBorder(10, 10, 15, 10));

        JPanel optionsPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        optionsPanel.setOpaque(false);
        optionsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        optionARadio = createOptionRadio(Question.Option.A);
        optionBRadio = createOptionRadio(Question.Option.B);
        optionCRadio = createOptionRadio(Question.Option.C);
        optionDRadio = createOptionRadio(Question.Option.D);

        optionGroup = new ButtonGroup();
        optionGroup.add(optionARadio);
        optionGroup.add(optionBRadio);
        optionGroup.add(optionCRadio);
        optionGroup.add(optionDRadio);

        optionsPanel.add(optionARadio);
        optionsPanel.add(optionBRadio);
        optionsPanel.add(optionCRadio);
        optionsPanel.add(optionDRadio);

        centerCard.add(questionTextArea, BorderLayout.NORTH);
        centerCard.add(optionsPanel, BorderLayout.CENTER);

        // Bottom Controls (Prev, Clear, Next)
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);

        prevButton = UITheme.createButton("◄ Previous", UITheme.SECONDARY, Color.WHITE, 13, true);
        nextButton = UITheme.createButton("Next ►", UITheme.ACCENT, Color.WHITE, 13, true);
        clearButton = UITheme.createButton("Clear Choice", new Color(241, 245, 249), UITheme.TEXT_MAIN, 12, false);

        prevButton.addActionListener(e -> displayQuestion(currentQuestionIndex - 1));
        nextButton.addActionListener(e -> displayQuestion(currentQuestionIndex + 1));
        clearButton.addActionListener(e -> clearSelectedOption());

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftBtns.setOpaque(false);
        leftBtns.add(prevButton);
        leftBtns.add(clearButton);

        bottomBar.add(leftBtns, BorderLayout.WEST);
        bottomBar.add(nextButton, BorderLayout.EAST);

        container.add(qHeader, BorderLayout.NORTH);
        container.add(centerCard, BorderLayout.CENTER);
        container.add(bottomBar, BorderLayout.SOUTH);
        return container;
    }

    private JRadioButton createOptionRadio(Question.Option opt) {
        JRadioButton radio = new JRadioButton();
        radio.setFont(UITheme.FONT_BODY);
        radio.setOpaque(true);
        radio.setBackground(new Color(248, 250, 252));
        radio.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));
        radio.setCursor(new Cursor(Cursor.HAND_CURSOR));

        radio.addActionListener(e -> {
            if (radio.isSelected()) {
                selectOption(opt);
            }
        });
        return radio;
    }

    private void displayQuestion(int index) {
        if (index < 0 || index >= questions.size()) return;

        currentQuestionIndex = index;
        Question q = questions.get(index);

        questionHeaderLabel.setText(String.format("Question %d of %d", index + 1, questions.size()));
        marksBadge.setText(String.format("+%d Mark%s", q.getMarks(), q.getMarks() > 1 ? "s" : ""));
        questionTextArea.setText(q.getQuestionText());

        optionARadio.setText("A) " + q.getOptionA());
        optionBRadio.setText("B) " + q.getOptionB());
        optionCRadio.setText("C) " + q.getOptionC());
        optionDRadio.setText("D) " + q.getOptionD());

        Question.Option chosen = userAnswers.get(q.getId());
        optionGroup.clearSelection();
        if (chosen == Question.Option.A) optionARadio.setSelected(true);
        else if (chosen == Question.Option.B) optionBRadio.setSelected(true);
        else if (chosen == Question.Option.C) optionCRadio.setSelected(true);
        else if (chosen == Question.Option.D) optionDRadio.setSelected(true);

        flagCheckbox.setSelected(flaggedQuestions.contains(q.getId()));

        prevButton.setEnabled(index > 0);
        nextButton.setEnabled(index < questions.size() - 1);

        updatePaletteStyles();
    }

    private void selectOption(Question.Option choice) {
        Question q = questions.get(currentQuestionIndex);
        userAnswers.put(q.getId(), choice);
        updatePaletteStyles();

        // Asynchronously persist answer to DB
        new Thread(() -> {
            try {
                attemptService.saveAnswer(attempt.getId(), q.getId(), choice);
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "Failed to save answer for question: " + q.getId(), ex);
            }
        }).start();
    }

    private void clearSelectedOption() {
        Question q = questions.get(currentQuestionIndex);
        userAnswers.remove(q.getId());
        optionGroup.clearSelection();
        updatePaletteStyles();

        new Thread(() -> {
            try {
                attemptService.saveAnswer(attempt.getId(), q.getId(), null);
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "Failed to clear answer for question: " + q.getId(), ex);
            }
        }).start();
    }

    private void toggleFlagCurrentQuestion() {
        Question q = questions.get(currentQuestionIndex);
        if (flagCheckbox.isSelected()) {
            flaggedQuestions.add(q.getId());
        } else {
            flaggedQuestions.remove(q.getId());
        }
        updatePaletteStyles();
    }

    private void updatePaletteStyles() {
        int answeredCount = 0;
        int flaggedCount = 0;

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            JButton btn = paletteButtons[i];
            boolean isAnswered = userAnswers.containsKey(q.getId());
            boolean isFlagged = flaggedQuestions.contains(q.getId());
            boolean isCurrent = (i == currentQuestionIndex);

            if (isAnswered) answeredCount++;
            if (isFlagged) flaggedCount++;

            // Visual Styling
            if (isAnswered) {
                btn.setBackground(UITheme.SUCCESS_DARK);
                btn.setForeground(Color.WHITE);
            } else if (isFlagged) {
                btn.setBackground(UITheme.WARNING);
                btn.setForeground(Color.WHITE);
            } else {
                btn.setBackground(new Color(241, 245, 249));
                btn.setForeground(UITheme.TEXT_MAIN);
            }

            if (isCurrent) {
                btn.setBorder(BorderFactory.createLineBorder(UITheme.ACCENT, 3, true));
            } else if (isFlagged) {
                btn.setBorder(BorderFactory.createLineBorder(UITheme.WARNING_DARK, 2, true));
            } else {
                btn.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1, true));
            }
        }

        int unansweredCount = questions.size() - answeredCount;
        answeredCountLabel.setText("Answered: " + answeredCount + " / " + questions.size());
        flaggedCountLabel.setText("Flagged for Review: " + flaggedCount);
        unansweredCountLabel.setText("Unanswered: " + unansweredCount);
    }

    private void startTimer() {
        try {
            timerService = attemptService.createAndStartTimer(attempt.getId(), this);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Could not start timer via attemptService, using standalone fallback", e);
            timerService = new ExamTimerService(attempt.getId(), totalDurationSeconds, null, () -> {
                SwingUtilities.invokeLater(this::onTimeExpired);
            });
            timerService.addListener(this);
            timerService.start();
        }
    }

    // --- ExamTimerListener Callbacks (All guaranteed on EDT) ---

    @Override
    public void onTick(long remainingSeconds, String formattedTime) {
        timerLabel.setText(formattedTime);
        timerProgressBar.setValue((int) remainingSeconds);

        if (remainingSeconds <= 10) {
            timerCard.setBackground(UITheme.DANGER_LIGHT);
            timerLabel.setForeground(UITheme.DANGER_DARK);
            timerProgressBar.setForeground(UITheme.DANGER_DARK);
        } else if (remainingSeconds <= 60) {
            timerCard.setBackground(UITheme.WARNING_LIGHT);
            timerLabel.setForeground(UITheme.WARNING_DARK);
            timerProgressBar.setForeground(UITheme.WARNING_DARK);
        }
    }

    @Override
    public void onWarning(long remainingSeconds) {
        Toolkit.getDefaultToolkit().beep();
    }

    @Override
    public void onTimeExpired() {
        LOGGER.info("Exam deadline reached. Initiating auto-submission on EDT...");
        submitButton.setEnabled(false);
        prevButton.setEnabled(false);
        nextButton.setEnabled(false);
        clearButton.setEnabled(false);

        JOptionPane.showMessageDialog(this,
                "Time has expired!\n\nYour examination answers are being finalized and scored automatically.",
                "Time Expired - Auto Submission",
                JOptionPane.WARNING_MESSAGE);

        finalizeSubmission(true);
    }

    private void promptManualSubmission() {
        int answered = userAnswers.size();
        int unanswered = questions.size() - answered;

        String msg = String.format(
                "Are you sure you want to finalize and submit your examination?\n\n" +
                "• Questions Answered   : %d\n" +
                "• Questions Unanswered : %d\n" +
                "• Total Questions      : %d\n\n" +
                "Warning: You cannot return to this exam after submission.",
                answered, unanswered, questions.size()
        );

        int opt = JOptionPane.showConfirmDialog(this, msg, "Confirm Exam Submission",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            finalizeSubmission(false);
        }
    }

    private void finalizeSubmission(boolean isAuto) {
        if (timerService != null) {
            timerService.cancel();
        }

        // Run submission in background thread to avoid blocking EDT
        JDialog loadingDialog = new JDialog(this, "Submitting...", true);
        loadingDialog.setSize(300, 100);
        loadingDialog.setLocationRelativeTo(this);
        loadingDialog.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 25));
        loadingDialog.add(new JLabel("Evaluating responses and calculating score..."));
        loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        new Thread(() -> {
            try {
                ExamResult result = attemptService.submitAttempt(attempt.getId(), userAnswers, isAuto);
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.dispose();
                    dispose();
                    ExamResultDialog resultDialog = new ExamResultDialog(parentDashboard, result, questions);
                    resultDialog.setVisible(true);
                });
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Submission failed", ex);
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.dispose();
                    JOptionPane.showMessageDialog(this,
                            "Error finalizing exam submission:\n" + ex.getMessage(),
                            "Submission Error",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();

        loadingDialog.setVisible(true);
    }

    private void confirmEarlyExit() {
        int opt = JOptionPane.showConfirmDialog(this,
                "Your examination is currently in progress!\n\n" +
                "Exiting will leave your attempt active in the background until the timer expires.\n" +
                "Are you sure you wish to exit to dashboard?",
                "Leave Exam in Progress?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            if (timerService != null) {
                timerService.cancel();
            }
            dispose();
        }
    }
}
