package com.onlineexam.ui;

import com.onlineexam.model.*;
import com.onlineexam.service.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enterprise Student Learning & Examination Dashboard.
 * Features an integrated Learning Hub (Subjects, Topics, Study Notes, Code Examples, Attachments),
 * dynamically managed Practice Quizzes and Formal Examinations, Real-time Countdown Attempts,
 * and comprehensive Scorecard History.
 */
public class StudentDashboardFrame extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(StudentDashboardFrame.class.getName());

    private final User student;
    private final AuthService authService;
    private final ExamService examService;
    private final AttemptService attemptService;
    private final SubjectService subjectService;
    private final TopicService topicService;
    private final LearningService learningService;

    // --- Learning Hub UI Components ---
    private DefaultListModel<Subject> subjectsListModel;
    private JList<Subject> subjectsList;
    private DefaultListModel<Topic> topicsListModel;
    private JList<Topic> topicsList;
    private DefaultListModel<StudyMaterial> materialsListModel;
    private JList<StudyMaterial> materialsList;

    private JLabel materialTitleLabel;
    private JTextArea materialContentArea;
    private JButton openResourceUrlBtn;
    private JButton openAttachmentBtn;
    private JButton markStudiedBtn;
    private JButton launchTopicQuizBtn;
    private JLabel topicProgressLabel;
    private JProgressBar topicProgressBar;
    private StudyMaterial currentSelectedMaterial;

    // --- Available Examinations UI Components ---
    private JComboBox<String> examSubjectFilterCombo;
    private JComboBox<String> examTypeFilterCombo;
    private JTable catalogTable;
    private DefaultTableModel catalogModel;
    private List<Exam> allAvailableExams = new ArrayList<>();
    private List<Subject> cachedSubjects = new ArrayList<>();

    // --- History UI Components ---
    private JTable historyTable;
    private DefaultTableModel historyModel;
    private List<StudentExam> studentAttempts = new ArrayList<>();

    // --- Progress Summary UI Components ---
    private JLabel statSubjectsExploredLbl;
    private JLabel statTopicsStudiedLbl;
    private JLabel statMaterialsReadLbl;
    private JLabel statExamsCompletedLbl;
    private JLabel statAverageScoreLbl;

    // --- Common Status Banner ---
    private JButton refreshBtn;
    private JPanel statusBanner;
    private JLabel statusBannerText;
    private JButton statusBannerActionBtn;

    public StudentDashboardFrame(User student, AuthService authService, ExamService examService, AttemptService attemptService) {
        this(student, authService, examService, attemptService, new SubjectService(), new TopicService(), new LearningService());
    }

    public StudentDashboardFrame(User student, AuthService authService, ExamService examService, AttemptService attemptService,
                                 SubjectService subjectService, TopicService topicService, LearningService learningService) {
        this.student = student;
        this.authService = authService;
        this.examService = examService;
        this.attemptService = attemptService;
        this.subjectService = subjectService;
        this.topicService = topicService;
        this.learningService = learningService;

        initUI();
        refreshAllData();
    }

    private void initUI() {
        setTitle("Student Learning & Examination Portal - " + student.getFullName());
        setSize(1180, 780);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_LIGHT);

        // North: Header Bar + Live Database Status Banner
        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.add(buildHeaderBar(), BorderLayout.NORTH);
        northContainer.add(buildDbStatusBar(), BorderLayout.SOUTH);
        root.add(northContainer, BorderLayout.NORTH);

        // Center: Tabbed Experience
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BOLD);
        tabbedPane.setBackground(Color.WHITE);

        tabbedPane.addTab("Learning Hub & Study Notes", buildLearningHubTab());
        tabbedPane.addTab("Available Examinations & Quizzes", buildCatalogTab());
        tabbedPane.addTab("My Exam History & Scorecards", buildHistoryTab());
        tabbedPane.addTab("My Learning Progress", buildProgressTab());

        root.add(tabbedPane, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildHeaderBar() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR),
                new EmptyBorder(12, 24, 12, 24)
        ));

        // Brand & User Profile
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        left.setOpaque(false);

        JLabel brandIcon = new JLabel("\u270E");
        brandIcon.setFont(new Font("Segoe UI Emoji", Font.BOLD, 26));
        brandIcon.setForeground(UITheme.PRIMARY);

        JPanel userMeta = new JPanel(new GridLayout(2, 1, 0, 2));
        userMeta.setOpaque(false);
        JLabel nameLbl = new JLabel(student.getFullName() + " (Student)");
        nameLbl.setFont(UITheme.FONT_TITLE);
        nameLbl.setForeground(UITheme.TEXT_MAIN);

        JLabel emailLbl = new JLabel(student.getEmail());
        emailLbl.setFont(UITheme.FONT_SMALL);
        emailLbl.setForeground(UITheme.TEXT_MUTED);

        userMeta.add(nameLbl);
        userMeta.add(emailLbl);

        left.add(brandIcon);
        left.add(userMeta);

        // Actions
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        refreshBtn = UITheme.createButton("Refresh All", UITheme.PRIMARY, Color.WHITE, 12, true);
        refreshBtn.addActionListener(e -> refreshAllData());

        JButton dbSettingsBtn = UITheme.createButton("DB Settings", UITheme.SECONDARY, Color.WHITE, 12, false);
        dbSettingsBtn.addActionListener(e -> new DatabaseConfigDialog(this, this::refreshAllData).setVisible(true));

        JButton logoutBtn = UITheme.createButton("Logout", new Color(241, 245, 249), UITheme.TEXT_MAIN, 12, false);
        logoutBtn.addActionListener(e -> logout());

        right.add(refreshBtn);
        right.add(dbSettingsBtn);
        right.add(logoutBtn);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildDbStatusBar() {
        statusBanner = new JPanel(new BorderLayout());
        statusBanner.setBorder(new EmptyBorder(6, 24, 6, 24));
        statusBanner.setBackground(new Color(248, 250, 252));

        statusBannerText = new JLabel("Checking database connection...");
        statusBannerText.setFont(UITheme.FONT_SMALL);

        statusBannerActionBtn = UITheme.createButton("Configure DB", UITheme.ACCENT, Color.WHITE, 11, true);
        statusBannerActionBtn.setVisible(false);
        statusBannerActionBtn.addActionListener(e -> new DatabaseConfigDialog(this, this::refreshAllData).setVisible(true));

        statusBanner.add(statusBannerText, BorderLayout.WEST);
        statusBanner.add(statusBannerActionBtn, BorderLayout.EAST);

        return statusBanner;
    }

    // ===================================================================
    // TAB 1: LEARNING HUB & STUDY MATERIALS
    // ===================================================================
    private JPanel buildLearningHubTab() {
        JPanel tab = new JPanel(new BorderLayout(15, 15));
        tab.setBackground(UITheme.BG_LIGHT);
        tab.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Sub-split 1: Left List (Subjects) + Middle List (Topics)
        subjectsListModel = new DefaultListModel<>();
        subjectsList = new JList<>(subjectsListModel);
        subjectsList.setFont(UITheme.FONT_REGULAR);
        subjectsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        subjectsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onSubjectSelected();
        });

        topicsListModel = new DefaultListModel<>();
        topicsList = new JList<>(topicsListModel);
        topicsList.setFont(UITheme.FONT_REGULAR);
        topicsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        topicsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onTopicSelected();
        });

        materialsListModel = new DefaultListModel<>();
        materialsList = new JList<>(materialsListModel);
        materialsList.setFont(UITheme.FONT_REGULAR);
        materialsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        materialsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onMaterialSelected();
        });

        // Left Navigation Column: Subjects & Topics
        JPanel leftNav = new JPanel(new GridLayout(2, 1, 0, 12));
        leftNav.setPreferredSize(new Dimension(320, 500));
        leftNav.setOpaque(false);

        JPanel subjectsCard = UITheme.createCardPanel(new BorderLayout(0, 8));
        JLabel subjHeader = new JLabel("1. Browse Subjects");
        subjHeader.setFont(UITheme.FONT_BOLD);
        subjHeader.setForeground(UITheme.PRIMARY);
        subjectsCard.add(subjHeader, BorderLayout.NORTH);
        subjectsCard.add(new JScrollPane(subjectsList), BorderLayout.CENTER);

        JPanel topicsCard = UITheme.createCardPanel(new BorderLayout(0, 8));
        JLabel topicHeader = new JLabel("2. Select Topic");
        topicHeader.setFont(UITheme.FONT_BOLD);
        topicHeader.setForeground(UITheme.PRIMARY);
        topicsCard.add(topicHeader, BorderLayout.NORTH);
        topicsCard.add(new JScrollPane(topicsList), BorderLayout.CENTER);

        leftNav.add(subjectsCard);
        leftNav.add(topicsCard);

        // Center Area: Topic Materials List + Full Reading Pane
        JPanel contentCard = UITheme.createCardPanel(new BorderLayout(12, 12));

        // Top bar in Content Card: Material Selector & Progress
        JPanel materialHeaderPanel = new JPanel(new BorderLayout(10, 8));
        materialHeaderPanel.setOpaque(false);

        JPanel progressWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        progressWrap.setOpaque(false);
        topicProgressLabel = new JLabel("Topic Progress: 0%");
        topicProgressLabel.setFont(UITheme.FONT_BOLD);
        topicProgressBar = new JProgressBar(0, 100);
        topicProgressBar.setPreferredSize(new Dimension(160, 18));
        topicProgressBar.setStringPainted(true);
        topicProgressBar.setForeground(UITheme.ACCENT);
        progressWrap.add(topicProgressLabel);
        progressWrap.add(topicProgressBar);

        materialTitleLabel = new JLabel("Select a topic and study material to begin learning");
        materialTitleLabel.setFont(UITheme.FONT_TITLE);
        materialTitleLabel.setForeground(UITheme.TEXT_MAIN);

        materialHeaderPanel.add(materialTitleLabel, BorderLayout.NORTH);
        materialHeaderPanel.add(progressWrap, BorderLayout.SOUTH);

        // Materials navigation selector
        JPanel materialsPickCard = new JPanel(new BorderLayout(0, 6));
        materialsPickCard.setOpaque(false);
        JLabel matPickLbl = new JLabel("Available Notes & Modules for this Topic:");
        matPickLbl.setFont(UITheme.FONT_SMALL);
        matPickLbl.setForeground(UITheme.TEXT_MUTED);
        materialsPickCard.add(matPickLbl, BorderLayout.NORTH);
        JScrollPane matScroll = new JScrollPane(materialsList);
        matScroll.setPreferredSize(new Dimension(400, 90));
        materialsPickCard.add(matScroll, BorderLayout.CENTER);

        JPanel northWrap = new JPanel(new BorderLayout(0, 10));
        northWrap.setOpaque(false);
        northWrap.add(materialHeaderPanel, BorderLayout.NORTH);
        northWrap.add(materialsPickCard, BorderLayout.SOUTH);
        contentCard.add(northWrap, BorderLayout.NORTH);

        // Reading Content Pane
        materialContentArea = new JTextArea();
        materialContentArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        materialContentArea.setEditable(false);
        materialContentArea.setLineWrap(true);
        materialContentArea.setWrapStyleWord(true);
        materialContentArea.setMargin(new Insets(12, 12, 12, 12));
        materialContentArea.setBackground(new Color(253, 254, 255));
        JScrollPane contentScroll = new JScrollPane(materialContentArea);
        contentScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        contentCard.add(contentScroll, BorderLayout.CENTER);

        // Bottom Actions Bar (Links, Attachments, Mark Studied, Launch Quiz)
        JPanel bottomActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bottomActions.setOpaque(false);

        openResourceUrlBtn = UITheme.createButton("Open External Link", UITheme.SECONDARY, Color.WHITE, 12, false);
        openResourceUrlBtn.setEnabled(false);
        openResourceUrlBtn.addActionListener(e -> openMaterialUrl());

        openAttachmentBtn = UITheme.createButton("View Attachment", new Color(100, 116, 139), Color.WHITE, 12, false);
        openAttachmentBtn.setEnabled(false);
        openAttachmentBtn.addActionListener(e -> openMaterialAttachment());

        markStudiedBtn = UITheme.createButton("Mark as Studied", UITheme.ACCENT, Color.WHITE, 12, true);
        markStudiedBtn.setEnabled(false);
        markStudiedBtn.addActionListener(e -> markCurrentMaterialStudied());

        launchTopicQuizBtn = UITheme.createButton("Start Topic Practice Quiz", new Color(16, 185, 129), Color.WHITE, 12, true);
        launchTopicQuizBtn.setEnabled(false);
        launchTopicQuizBtn.addActionListener(e -> launchQuizForSelectedTopic());

        bottomActions.add(openResourceUrlBtn);
        bottomActions.add(openAttachmentBtn);
        bottomActions.add(markStudiedBtn);
        bottomActions.add(launchTopicQuizBtn);
        contentCard.add(bottomActions, BorderLayout.SOUTH);

        tab.add(leftNav, BorderLayout.WEST);
        tab.add(contentCard, BorderLayout.CENTER);
        return tab;
    }

    private void onSubjectSelected() {
        Subject selected = subjectsList.getSelectedValue();
        topicsListModel.clear();
        materialsListModel.clear();
        currentSelectedMaterial = null;
        materialTitleLabel.setText("Select a topic to view materials");
        materialContentArea.setText("");
        markStudiedBtn.setEnabled(false);
        openResourceUrlBtn.setEnabled(false);
        openAttachmentBtn.setEnabled(false);
        launchTopicQuizBtn.setEnabled(false);
        topicProgressBar.setValue(0);
        topicProgressLabel.setText("Topic Progress: 0%");

        if (selected == null) return;

        new Thread(() -> {
            try {
                List<Topic> topics = topicService.getPublishedTopics(selected.getId());
                SwingUtilities.invokeLater(() -> {
                    for (Topic t : topics) {
                        topicsListModel.addElement(t);
                    }
                    if (!topics.isEmpty()) {
                        topicsList.setSelectedIndex(0);
                    }
                });
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "Failed to load topics for subject: " + selected.getId(), ex);
            }
        }).start();
    }

    private void onTopicSelected() {
        Topic selected = topicsList.getSelectedValue();
        materialsListModel.clear();
        currentSelectedMaterial = null;
        materialTitleLabel.setText("Select a study module");
        materialContentArea.setText("");
        markStudiedBtn.setEnabled(false);
        openResourceUrlBtn.setEnabled(false);
        openAttachmentBtn.setEnabled(false);
        launchTopicQuizBtn.setEnabled(false);

        if (selected == null) return;

        new Thread(() -> {
            try {
                List<StudyMaterial> materials = learningService.getPublishedMaterials(selected.getId());
                double progress = learningService.getTopicProgressPercentage(student.getId(), selected.getId());
                List<Exam> topicExams = examService.getExamsByTopic(selected.getId());

                SwingUtilities.invokeLater(() -> {
                    for (StudyMaterial m : materials) {
                        materialsListModel.addElement(m);
                    }
                    topicProgressBar.setValue((int) Math.round(progress));
                    topicProgressLabel.setText(String.format("Topic Progress: %.0f%%", progress));
                    launchTopicQuizBtn.setEnabled(!topicExams.isEmpty());
                    launchTopicQuizBtn.setText(!topicExams.isEmpty() ?
                            "Take Assessment (" + topicExams.size() + " Available)" : "No Assessment Configured");

                    if (!materials.isEmpty()) {
                        materialsList.setSelectedIndex(0);
                    } else {
                        materialTitleLabel.setText(selected.getName());
                        materialContentArea.setText("No study materials configured for this topic yet. Check back soon!");
                    }
                });
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "Failed to load materials for topic: " + selected.getId(), ex);
            }
        }).start();
    }

    private void onMaterialSelected() {
        StudyMaterial m = materialsList.getSelectedValue();
        currentSelectedMaterial = m;
        if (m == null) return;

        materialTitleLabel.setText(m.getTitle());
        materialContentArea.setText(m.getContent() != null ? m.getContent() : "(No written content provided)");
        materialContentArea.setCaretPosition(0);

        openResourceUrlBtn.setEnabled(m.getResourceUrl() != null && !m.getResourceUrl().isBlank());
        openAttachmentBtn.setEnabled(m.getAttachmentPath() != null && !m.getAttachmentPath().isBlank());

        // Check if student already studied this
        new Thread(() -> {
            try {
                boolean studied = learningService.isMaterialStudied(student.getId(), m.getId());
                SwingUtilities.invokeLater(() -> {
                    markStudiedBtn.setEnabled(true);
                    if (studied) {
                        markStudiedBtn.setText("\u2713 Studied");
                        markStudiedBtn.setBackground(new Color(22, 163, 74));
                    } else {
                        markStudiedBtn.setText("Mark as Studied");
                        markStudiedBtn.setBackground(UITheme.ACCENT);
                    }
                });
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "Failed to check material studied status", ex);
            }
        }).start();
    }

    private void markCurrentMaterialStudied() {
        if (currentSelectedMaterial == null) return;
        new Thread(() -> {
            try {
                learningService.markMaterialStudied(student.getId(), currentSelectedMaterial.getId());
                double progress = learningService.getTopicProgressPercentage(student.getId(), currentSelectedMaterial.getTopicId());
                SwingUtilities.invokeLater(() -> {
                    markStudiedBtn.setText("\u2713 Studied");
                    markStudiedBtn.setBackground(new Color(22, 163, 74));
                    topicProgressBar.setValue((int) Math.round(progress));
                    topicProgressLabel.setText(String.format("Topic Progress: %.0f%%", progress));
                    updateProgressSummaryAsync();
                    JOptionPane.showMessageDialog(this, "Module marked as studied! Progress updated.", "Progress Saved", JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Failed to save studied status", ex);
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Could not save progress: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private void openMaterialUrl() {
        if (currentSelectedMaterial == null || currentSelectedMaterial.getResourceUrl() == null) return;
        String url = currentSelectedMaterial.getResourceUrl().trim();
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                showUrlDialog(url);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Could not open browser for URL: " + url, ex);
            showUrlDialog(url);
        }
    }

    private void showUrlDialog(String url) {
        JTextField urlField = new JTextField(url);
        urlField.setEditable(false);
        JOptionPane.showMessageDialog(this, urlField, "Learning Resource Link", JOptionPane.INFORMATION_MESSAGE);
    }

    private void openMaterialAttachment() {
        if (currentSelectedMaterial == null || currentSelectedMaterial.getAttachmentPath() == null) return;
        String path = currentSelectedMaterial.getAttachmentPath();
        java.io.File file = new java.io.File(path);
        if (!file.exists()) {
            file = new java.io.File(System.getProperty("user.dir"), path);
        }
        if (file.exists()) {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(file);
                } else {
                    JOptionPane.showMessageDialog(this, "Attachment located at:\n" + file.getAbsolutePath(), "Attachment File", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not open attachment automatically:\n" + file.getAbsolutePath(), "Attachment Notice", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Attachment file not found on disk:\n" + path, "File Not Found", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void launchQuizForSelectedTopic() {
        Topic selected = topicsList.getSelectedValue();
        if (selected == null) return;

        new Thread(() -> {
            try {
                List<Exam> topicExams = examService.getExamsByTopic(selected.getId());
                SwingUtilities.invokeLater(() -> {
                    if (topicExams.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "No examinations or quizzes currently configured for this topic.", "No Exams", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    Exam targetExam = topicExams.get(0);
                    if (topicExams.size() > 1) {
                        String[] titles = topicExams.stream().map(e -> e.getTitle() + " (" + e.getAssessmentType() + ")").toArray(String[]::new);
                        String chosen = (String) JOptionPane.showInputDialog(this, "Select assessment to launch:", "Topic Assessments",
                                JOptionPane.QUESTION_MESSAGE, null, titles, titles[0]);
                        if (chosen == null) return;
                        for (int i = 0; i < titles.length; i++) {
                            if (titles[i].equals(chosen)) {
                                targetExam = topicExams.get(i);
                                break;
                            }
                        }
                    }
                    startExamSession(targetExam);
                });
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Failed to launch topic quiz", ex);
            }
        }).start();
    }

    // ===================================================================
    // TAB 2: AVAILABLE EXAMINATIONS & QUIZZES
    // ===================================================================
    private JPanel buildCatalogTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 16));
        tab.setBackground(UITheme.BG_LIGHT);
        tab.setBorder(new EmptyBorder(16, 24, 20, 24));

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        filterBar.setOpaque(false);

        JLabel filterSubjLbl = new JLabel("Filter Subject:");
        filterSubjLbl.setFont(UITheme.FONT_BOLD);
        examSubjectFilterCombo = new JComboBox<>(new String[]{"All Subjects"});
        examSubjectFilterCombo.addActionListener(e -> applyCatalogFilters());

        JLabel filterTypeLbl = new JLabel("Assessment Type:");
        filterTypeLbl.setFont(UITheme.FONT_BOLD);
        examTypeFilterCombo = new JComboBox<>(new String[]{"All Types", "Formal Examinations", "Practice Quizzes"});
        examTypeFilterCombo.addActionListener(e -> applyCatalogFilters());

        filterBar.add(filterSubjLbl);
        filterBar.add(examSubjectFilterCombo);
        filterBar.add(Box.createHorizontalStrut(15));
        filterBar.add(filterTypeLbl);
        filterBar.add(examTypeFilterCombo);

        JPanel card = UITheme.createCardPanel(new BorderLayout(0, 12));
        card.add(filterBar, BorderLayout.NORTH);

        String[] cols = {"ID", "Exam / Quiz Title", "Assessment Type", "Duration", "Total Marks", "Passing Marks", "Status"};
        catalogModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        catalogTable = new JTable(catalogModel);
        UITheme.styleTable(catalogTable);
        catalogTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        catalogTable.getColumnModel().getColumn(1).setPreferredWidth(260);
        catalogTable.getColumnModel().getColumn(2).setPreferredWidth(120);

        JScrollPane scrollPane = new JScrollPane(catalogTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        card.add(scrollPane, BorderLayout.CENTER);

        // Action Footer
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actionPanel.setOpaque(false);

        JButton startBtn = UITheme.createButton("Start Selected Exam", UITheme.ACCENT, Color.WHITE, 13, true);
        startBtn.addActionListener(e -> launchSelectedCatalogExam());

        actionPanel.add(startBtn);
        card.add(actionPanel, BorderLayout.SOUTH);

        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void applyCatalogFilters() {
        catalogModel.setRowCount(0);
        String selectedSubj = (String) examSubjectFilterCombo.getSelectedItem();
        String selectedType = (String) examTypeFilterCombo.getSelectedItem();

        Long filterSubjId = null;
        if (selectedSubj != null && !selectedSubj.equals("All Subjects")) {
            for (Subject s : cachedSubjects) {
                if (s.getName().equals(selectedSubj)) {
                    filterSubjId = s.getId();
                    break;
                }
            }
        }

        Exam.AssessmentType filterType = null;
        if ("Formal Examinations".equals(selectedType)) filterType = Exam.AssessmentType.FORMAL_EXAM;
        if ("Practice Quizzes".equals(selectedType)) filterType = Exam.AssessmentType.PRACTICE_QUIZ;

        int rowCount = 0;
        for (Exam ex : allAvailableExams) {
            if (filterSubjId != null && !filterSubjId.equals(ex.getSubjectId())) {
                continue;
            }
            if (filterType != null && ex.getAssessmentType() != filterType) {
                continue;
            }

            String statusStr = "Active - Ready";
            for (StudentExam att : studentAttempts) {
                if (att.getExamId().equals(ex.getId())) {
                    if (att.isCompleted()) {
                        statusStr = "Completed (" + att.getTotalScore() + "/" + ex.getTotalMarks() + ")";
                    } else if (att.isInProgress()) {
                        statusStr = "In Progress";
                    }
                    break;
                }
            }

            catalogModel.addRow(new Object[]{
                    ex.getId(),
                    ex.getTitle(),
                    ex.getAssessmentType() == Exam.AssessmentType.PRACTICE_QUIZ ? "Practice Quiz" : "Formal Exam",
                    ex.getDurationMinutes() + " mins",
                    ex.getTotalMarks() + " marks",
                    ex.getPassingMarks() + " marks",
                    statusStr
            });
            rowCount++;
        }

        if (rowCount == 0) {
            catalogModel.addRow(new Object[]{"-", "No matching examinations found. Try adjusting filter criteria.", "-", "-", "-", "-", "-"});
        }
    }

    private void launchSelectedCatalogExam() {
        int row = catalogTable.getSelectedRow();
        if (row < 0 || row >= allAvailableExams.size()) {
            JOptionPane.showMessageDialog(this, "Please select an examination from the table to begin.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Object idObj = catalogModel.getValueAt(row, 0);
        if (!(idObj instanceof Long)) {
            JOptionPane.showMessageDialog(this, "Please select a valid examination row.", "Invalid Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long examId = (Long) idObj;
        Exam target = allAvailableExams.stream().filter(e -> e.getId().equals(examId)).findFirst().orElse(null);
        if (target != null) {
            startExamSession(target);
        }
    }

    private void startExamSession(Exam selectedExam) {
        String msg = String.format("""
                Examination: %s
                Assessment Type: %s
                Duration: %d minutes
                Total Marks: %d | Passing Marks: %d

                Instructions:
                - Once started, the timer runs continuously in a thread-safe background process.
                - Submitting early saves all responses immediately.
                - When the countdown expires, your answers will auto-submit automatically.

                Ready to start?
                """,
                selectedExam.getTitle(),
                selectedExam.getAssessmentType(),
                selectedExam.getDurationMinutes(),
                selectedExam.getTotalMarks(),
                selectedExam.getPassingMarks());

        int opt = JOptionPane.showConfirmDialog(this, msg, "Start Assessment Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (opt != JOptionPane.YES_OPTION) return;

        try {
            StudentExam attempt = attemptService.startAttempt(student.getId(), selectedExam.getId());
            List<Question> questions = examService.getExamQuestions(selectedExam.getId());

            if (questions.isEmpty()) {
                JOptionPane.showMessageDialog(this, "This examination has no questions configured yet. Please contact your administrator.", "No Questions", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ExamTakingFrame examFrame = new ExamTakingFrame(this, student, selectedExam, attempt, questions, attemptService);
            examFrame.setVisible(true);

        } catch (IllegalStateException ise) {
            JOptionPane.showMessageDialog(this, ise.getMessage(), "Cannot Start Exam", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to launch exam attempt", ex);
            JOptionPane.showMessageDialog(this, "Error starting exam attempt: " + ex.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===================================================================
    // TAB 3: HISTORY & SCORECARDS
    // ===================================================================
    private JPanel buildHistoryTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 16));
        tab.setBackground(UITheme.BG_LIGHT);
        tab.setBorder(new EmptyBorder(16, 24, 20, 24));

        JPanel card = UITheme.createCardPanel(new BorderLayout(0, 12));

        String[] cols = {"Attempt #", "Examination Title", "Assessment Type", "Score", "Status", "Completion Date"};
        historyModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        historyTable = new JTable(historyModel);
        UITheme.styleTable(historyTable);
        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        card.add(scrollPane, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        footer.setOpaque(false);

        JButton viewScorecardBtn = UITheme.createButton("View Detailed Scorecard", UITheme.PRIMARY, Color.WHITE, 12, true);
        viewScorecardBtn.addActionListener(e -> viewSelectedAttemptScorecard());

        footer.add(viewScorecardBtn);
        card.add(footer, BorderLayout.SOUTH);

        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void viewSelectedAttemptScorecard() {
        int row = historyTable.getSelectedRow();
        if (row < 0 || row >= studentAttempts.size()) {
            JOptionPane.showMessageDialog(this, "Please select an attempt from the history table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        StudentExam att = studentAttempts.get(row);
        if (!att.isCompleted()) {
            JOptionPane.showMessageDialog(this, "This attempt is still in progress and cannot be reviewed yet.", "In Progress", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            Optional<Exam> examOpt = examService.getExamById(att.getExamId());
            if (examOpt.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Exam definition not found.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Exam exam = examOpt.get();
            List<Question> questions = examService.getExamQuestions(exam.getId());

            List<StudentAnswer> savedAnswers = attemptService.getAttemptAnswers(att.getId());
            Map<Long, StudentAnswer> answerMap = new HashMap<>();
            for (StudentAnswer sa : savedAnswers) {
                if (sa.getQuestionId() != null) answerMap.put(sa.getQuestionId(), sa);
            }

            int correctCount = 0;
            int incorrectCount = 0;
            int unansweredCount = 0;
            for (Question q : questions) {
                StudentAnswer sa = answerMap.get(q.getId());
                if (sa == null || sa.getSelectedOption() == null) {
                    unansweredCount++;
                } else if (sa.isCorrect()) {
                    correctCount++;
                } else {
                    incorrectCount++;
                }
            }

            boolean passed = att.getTotalScore() >= exam.getPassingMarks();
            double pct = exam.getTotalMarks() > 0 ? (double) att.getTotalScore() / exam.getTotalMarks() * 100.0 : 0.0;

            ExamResult result = new ExamResult(
                    att.getId(),
                    student.getId(),
                    exam.getId(),
                    exam.getTitle(),
                    questions.size(),
                    correctCount,
                    incorrectCount,
                    unansweredCount,
                    exam.getTotalMarks(),
                    att.getTotalScore(),
                    exam.getPassingMarks(),
                    Math.round(pct * 100.0) / 100.0,
                    passed,
                    att.getStatus() == StudentExam.ExamStatus.AUTO_SUBMITTED,
                    att.getEndTime(),
                    savedAnswers
            );

            ExamResultDialog dlg = new ExamResultDialog(this, result, questions);
            dlg.setVisible(true);

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load scorecard for attemptId=" + att.getId(), ex);
            JOptionPane.showMessageDialog(this, "Database error while loading scorecard:\n" + ex.getMessage(), "Scorecard Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===================================================================
    // TAB 4: MY LEARNING PROGRESS SUMMARY
    // ===================================================================
    private JPanel buildProgressTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 20));
        tab.setBackground(UITheme.BG_LIGHT);
        tab.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel titleLbl = new JLabel("My Academic & Study Milestone Overview");
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.PRIMARY);
        tab.add(titleLbl, BorderLayout.NORTH);

        JPanel cardsGrid = new JPanel(new GridLayout(2, 3, 20, 20));
        cardsGrid.setOpaque(false);

        statSubjectsExploredLbl = new JLabel("0");
        statTopicsStudiedLbl = new JLabel("0");
        statMaterialsReadLbl = new JLabel("0");
        statExamsCompletedLbl = new JLabel("0");
        statAverageScoreLbl = new JLabel("0.0%");

        cardsGrid.add(createStatCard("Subjects Available", statSubjectsExploredLbl, UITheme.PRIMARY));
        cardsGrid.add(createStatCard("Topics In Catalog", statTopicsStudiedLbl, UITheme.SECONDARY));
        cardsGrid.add(createStatCard("Study Modules Read", statMaterialsReadLbl, UITheme.ACCENT));
        cardsGrid.add(createStatCard("Assessments Completed", statExamsCompletedLbl, new Color(16, 185, 129)));
        cardsGrid.add(createStatCard("Average Score Rate", statAverageScoreLbl, new Color(245, 158, 11)));

        tab.add(cardsGrid, BorderLayout.CENTER);
        return tab;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accent) {
        JPanel card = UITheme.createCardPanel(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                new EmptyBorder(16, 20, 16, 20)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UITheme.FONT_SMALL);
        titleLbl.setForeground(UITheme.TEXT_MUTED);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(UITheme.TEXT_MAIN);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    // ===================================================================
    // ASYNCHRONOUS DATA REFRESH PIPELINE
    // ===================================================================
    public void refreshAllData() {
        statusBannerText.setText("Connecting to database and synchronizing learning catalog...");
        statusBannerText.setForeground(UITheme.TEXT_MUTED);
        statusBannerActionBtn.setVisible(false);
        refreshBtn.setEnabled(false);

        new Thread(() -> {
            boolean dbConnected = false;
            String dbCatalogName = "";
            SQLException sqlException = null;

            List<Subject> fetchedSubjects = new ArrayList<>();
            List<Exam> fetchedExams = new ArrayList<>();
            List<StudentExam> fetchedAttempts = new ArrayList<>();

            try {
                com.onlineexam.config.DatabaseConnection.ConnectionTestResult testResult =
                        com.onlineexam.config.DatabaseConnection.testConnection();
                dbConnected = testResult.successful();
                dbCatalogName = testResult.databaseName() != null ? testResult.databaseName() : "online_exam_db";

                if (dbConnected) {
                    fetchedSubjects = subjectService.getPublishedSubjects();
                    fetchedExams = examService.getAvailableExams();
                    fetchedAttempts = attemptService.getStudentAttempts(student.getId());
                } else {
                    sqlException = new SQLException(testResult.message());
                }
            } catch (SQLException e) {
                sqlException = e;
                LOGGER.log(Level.WARNING, "Error loading student dashboard data", e);
            }

            final boolean isConnected = dbConnected;
            final String catalogName = dbCatalogName;
            final SQLException finalEx = sqlException;
            final List<Subject> finalSubjects = fetchedSubjects;
            final List<Exam> finalExams = fetchedExams;
            final List<StudentExam> finalAttempts = fetchedAttempts;

            SwingUtilities.invokeLater(() -> {
                refreshBtn.setEnabled(true);

                if (isConnected) {
                    statusBanner.setBackground(new Color(240, 253, 244));
                    statusBannerText.setText(" \u25CF Connected to MySQL (" + catalogName + ") - Live Mode");
                    statusBannerText.setForeground(new Color(22, 101, 52));
                    statusBannerActionBtn.setVisible(false);

                    cachedSubjects = finalSubjects;
                    allAvailableExams = finalExams;
                    studentAttempts = finalAttempts;

                    // Populate Subjects List for Learning Hub
                    subjectsListModel.clear();
                    for (Subject s : finalSubjects) {
                        subjectsListModel.addElement(s);
                    }
                    if (!finalSubjects.isEmpty() && subjectsList.getSelectedIndex() < 0) {
                        subjectsList.setSelectedIndex(0);
                    }

                    // Populate Exam filter combobox
                    examSubjectFilterCombo.removeAllItems();
                    examSubjectFilterCombo.addItem("All Subjects");
                    for (Subject s : finalSubjects) {
                        examSubjectFilterCombo.addItem(s.getName());
                    }

                    // Populate Exam Catalog table
                    applyCatalogFilters();

                    // Populate History table
                    historyModel.setRowCount(0);
                    int totalCompletedScore = 0;
                    int completedExamsCount = 0;

                    for (StudentExam att : finalAttempts) {
                        Optional<Exam> exOpt = finalExams.stream().filter(e -> e.getId().equals(att.getExamId())).findFirst();
                        String title = exOpt.map(Exam::getTitle).orElse("Exam #" + att.getExamId());
                        String type = exOpt.map(e -> e.getAssessmentType() == Exam.AssessmentType.PRACTICE_QUIZ ? "Practice Quiz" : "Formal Exam").orElse("Assessment");
                        int maxMarks = exOpt.map(Exam::getTotalMarks).orElse(0);

                        historyModel.addRow(new Object[]{
                                att.getId(),
                                title,
                                type,
                                att.getTotalScore() + (maxMarks > 0 ? " / " + maxMarks : ""),
                                att.getStatus(),
                                att.getEndTime() != null ? att.getEndTime().toString() : "In Progress"
                        });

                        if (att.isCompleted()) {
                            completedExamsCount++;
                            if (maxMarks > 0) {
                                totalCompletedScore += Math.round(((double) att.getTotalScore() / maxMarks) * 100.0);
                            }
                        }
                    }

                    // Update Milestone Summary Cards
                    statSubjectsExploredLbl.setText(String.valueOf(finalSubjects.size()));
                    statExamsCompletedLbl.setText(String.valueOf(completedExamsCount));
                    statAverageScoreLbl.setText(completedExamsCount > 0 ?
                            String.format("%.1f%%", (double) totalCompletedScore / completedExamsCount) : "N/A");

                    updateProgressSummaryAsync();

                } else {
                    statusBanner.setBackground(new Color(254, 242, 242));
                    String reason = finalEx != null ? finalEx.getMessage() : "Could not reach database.";
                    statusBannerText.setText(" \u26A0 Database Connection Error: " + reason);
                    statusBannerText.setForeground(new Color(153, 27, 27));
                    statusBannerActionBtn.setVisible(true);

                    catalogModel.setRowCount(0);
                    catalogModel.addRow(new Object[]{
                            "!", "DATABASE ERROR: " + reason, "-", "-", "-", "-", "Click 'DB Settings' or set DB_PASSWORD"
                    });
                }
            });
        }).start();
    }

    private void updateProgressSummaryAsync() {
        new Thread(() -> {
            try {
                int totalTopics = 0;
                for (Subject s : cachedSubjects) {
                    totalTopics += topicService.getPublishedTopics(s.getId()).size();
                }
                final int topicsCount = totalTopics;
                SwingUtilities.invokeLater(() -> statTopicsStudiedLbl.setText(String.valueOf(topicsCount)));
            } catch (SQLException ignored) {}
        }).start();
    }

    private void logout() {
        int opt = JOptionPane.showConfirmDialog(this, "Are you sure you want to log out of the student portal?", "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            dispose();
            new LoginFrame(authService, examService, attemptService).setVisible(true);
        }
    }
}
