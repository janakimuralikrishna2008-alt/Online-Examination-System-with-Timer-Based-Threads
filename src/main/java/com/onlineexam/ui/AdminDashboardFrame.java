package com.onlineexam.ui;

import com.onlineexam.model.*;
import com.onlineexam.service.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enterprise Administration Control Center.
 * Provides comprehensive Subject, Topic, Study Material, Question Bank,
 * Examination & Quiz Management, Candidate Results Analytics, and User Administration.
 */
public class AdminDashboardFrame extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(AdminDashboardFrame.class.getName());

    private final User adminUser;
    private final AuthService authService;
    private final ExamService examService;
    private final AttemptService attemptService;
    private final SubjectService subjectService;
    private final TopicService topicService;
    private final LearningService learningService;

    // --- Tab 1: Subject Management Components ---
    private JTable subjectsTable;
    private DefaultTableModel subjectsModel;
    private List<Subject> allSubjects = new ArrayList<>();

    // --- Tab 2: Topic Management Components ---
    private JComboBox<String> topicSubjectFilterCombo;
    private JTable topicsTable;
    private DefaultTableModel topicsModel;
    private List<Topic> allTopics = new ArrayList<>();

    // --- Tab 3: Study Material Management Components ---
    private JComboBox<String> materialTopicFilterCombo;
    private JTable materialsTable;
    private DefaultTableModel materialsModel;
    private List<StudyMaterial> allMaterials = new ArrayList<>();

    // --- Tab 4: Question Bank Components ---
    private JComboBox<String> questionTopicFilterCombo;
    private JTable questionBankTable;
    private DefaultTableModel questionBankModel;
    private List<Question> allQuestions = new ArrayList<>();

    // --- Tab 5: Exam & Quiz Management Components ---
    private JTable examsTable;
    private DefaultTableModel examsModel;
    private List<Exam> allExams = new ArrayList<>();

    // --- Tab 6: Results & Analytics Components ---
    private JTable resultsTable;
    private DefaultTableModel resultsModel;
    private List<StudentExam> allAttempts = new ArrayList<>();
    private JComboBox<String> examFilterCombo;
    private JLabel totalAttemptsLbl;
    private JLabel passRateLbl;
    private JLabel avgScoreLbl;

    // --- Tab 7: User Management Components ---
    private JTable usersTable;
    private DefaultTableModel usersModel;
    private List<User> allUsers = new ArrayList<>();

    // Header & Status Components
    private JButton refreshBtn;
    private JPanel statusBanner;
    private JLabel statusBannerText;
    private JButton statusBannerActionBtn;

    public AdminDashboardFrame(User adminUser, AuthService authService, ExamService examService, AttemptService attemptService) {
        this(adminUser, authService, examService, attemptService, new SubjectService(), new TopicService(), new LearningService());
    }

    public AdminDashboardFrame(User adminUser, AuthService authService, ExamService examService, AttemptService attemptService,
                               SubjectService subjectService, TopicService topicService, LearningService learningService) {
        this.adminUser = adminUser;
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
        setTitle("Administration Control Center - " + adminUser.getFullName());
        setSize(1220, 800);
        setMinimumSize(new Dimension(1040, 680));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_LIGHT);

        // Header Bar with Database Status Banner
        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.add(buildHeaderBar(), BorderLayout.NORTH);
        northContainer.add(buildDbStatusBar(), BorderLayout.SOUTH);
        root.add(northContainer, BorderLayout.NORTH);

        // Management Tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BOLD);
        tabbedPane.setBackground(Color.WHITE);

        tabbedPane.addTab("Subjects", buildSubjectManagementTab());
        tabbedPane.addTab("Topics", buildTopicManagementTab());
        tabbedPane.addTab("Study Materials", buildMaterialManagementTab());
        tabbedPane.addTab("Question Bank", buildQuestionBankTab());
        tabbedPane.addTab("Exams & Quizzes", buildExamManagementTab());
        tabbedPane.addTab("Results & Analytics", buildAnalyticsTab());
        tabbedPane.addTab("User Accounts", buildUserManagementTab());

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

        // Brand & User Info
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        left.setOpaque(false);

        JLabel brandIcon = new JLabel("\u2699");
        brandIcon.setFont(new Font("Segoe UI Emoji", Font.BOLD, 26));
        brandIcon.setForeground(UITheme.ACCENT);

        JPanel meta = new JPanel(new GridLayout(2, 1, 0, 2));
        meta.setOpaque(false);
        JLabel titleLbl = new JLabel(adminUser.getFullName() + " (Administrator)");
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_MAIN);

        JLabel subLbl = new JLabel(adminUser.getEmail() + " | Platform Control Center");
        subLbl.setFont(UITheme.FONT_SMALL);
        subLbl.setForeground(UITheme.TEXT_MUTED);

        meta.add(titleLbl);
        meta.add(subLbl);
        left.add(brandIcon);
        left.add(meta);

        // Actions
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        refreshBtn = UITheme.createButton("Refresh All Data", UITheme.PRIMARY, Color.WHITE, 12, true);
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
    // TAB 1: SUBJECT MANAGEMENT
    // ===================================================================
    private JPanel buildSubjectManagementTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 16));
        tab.setBackground(UITheme.BG_LIGHT);
        tab.setBorder(new EmptyBorder(16, 24, 20, 24));

        JPanel card = UITheme.createCardPanel(new BorderLayout(0, 12));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.setOpaque(false);
        JButton addBtn = UITheme.createButton("+ Create Subject", UITheme.PRIMARY, Color.WHITE, 12, true);
        JButton editBtn = UITheme.createButton("Edit Subject", UITheme.SECONDARY, Color.WHITE, 12, false);
        JButton togglePubBtn = UITheme.createButton("Toggle Publish", new Color(100, 116, 139), Color.WHITE, 12, false);
        JButton delBtn = UITheme.createButton("Delete Subject", UITheme.DANGER, Color.WHITE, 12, false);

        addBtn.addActionListener(e -> showAddOrEditSubjectDialog(null));
        editBtn.addActionListener(e -> {
            Subject s = getSelectedSubject();
            if (s != null) showAddOrEditSubjectDialog(s);
        });
        togglePubBtn.addActionListener(e -> toggleSubjectPublish());
        delBtn.addActionListener(e -> deleteSelectedSubject());

        toolbar.add(addBtn);
        toolbar.add(editBtn);
        toolbar.add(togglePubBtn);
        toolbar.add(delBtn);
        card.add(toolbar, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID", "Subject Name", "Description", "Status", "Topics", "Exams", "Created At"};
        subjectsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        subjectsTable = new JTable(subjectsModel);
        UITheme.styleTable(subjectsTable);
        subjectsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        subjectsTable.getColumnModel().getColumn(1).setPreferredWidth(220);
        subjectsTable.getColumnModel().getColumn(2).setPreferredWidth(300);

        card.add(new JScrollPane(subjectsTable), BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private Subject getSelectedSubject() {
        int row = subjectsTable.getSelectedRow();
        if (row < 0 || row >= allSubjects.size()) {
            JOptionPane.showMessageDialog(this, "Please select a subject from the table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return allSubjects.get(row);
    }

    private void showAddOrEditSubjectDialog(Subject existing) {
        boolean isEdit = existing != null;
        JDialog dlg = new JDialog(this, isEdit ? "Edit Subject" : "Create New Subject", true);
        dlg.setSize(480, 320);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        JTextField nameField = new JTextField(isEdit ? existing.getName() : "");
        JTextArea descArea = new JTextArea(isEdit ? existing.getDescription() : "", 4, 25);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        JCheckBox pubCheck = new JCheckBox("Published (Visible to Students)", isEdit ? existing.isPublished() : true);

        g.gridx = 0; g.gridy = 0; form.add(new JLabel("Subject Name:"), g);
        g.gridx = 1; form.add(nameField, g);

        g.gridx = 0; g.gridy = 1; form.add(new JLabel("Description:"), g);
        g.gridx = 1; form.add(new JScrollPane(descArea), g);

        g.gridx = 1; g.gridy = 2; form.add(pubCheck, g);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton saveBtn = UITheme.createButton("Save Subject", UITheme.PRIMARY, Color.WHITE, 12, true);
        JButton cancelBtn = UITheme.createButton("Cancel", new Color(241, 245, 249), UITheme.TEXT_MAIN, 12, false);

        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Subject name cannot be empty.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                if (isEdit) {
                    existing.setName(name);
                    existing.setDescription(descArea.getText().trim());
                    existing.setPublished(pubCheck.isSelected());
                    subjectService.updateSubject(adminUser, existing);
                } else {
                    Subject s = new Subject(name, descArea.getText().trim(), pubCheck.isSelected());
                    subjectService.createSubject(adminUser, s);
                }
                dlg.dispose();
                refreshAllData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Error saving subject: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancelBtn.addActionListener(e -> dlg.dispose());

        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        dlg.add(form, BorderLayout.CENTER);
        dlg.add(btnPanel, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void toggleSubjectPublish() {
        Subject s = getSelectedSubject();
        if (s == null) return;
        try {
            s.setPublished(!s.isPublished());
            subjectService.updateSubject(adminUser, s);
            refreshAllData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not update status: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedSubject() {
        Subject s = getSelectedSubject();
        if (s == null) return;
        int opt = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete subject: " + s.getName() + "?\nAll related topics must be removed first.", "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt != JOptionPane.YES_OPTION) return;
        try {
            subjectService.deleteSubject(adminUser, s.getId());
            refreshAllData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not delete subject:\n" + ex.getMessage(), "Delete Blocked", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===================================================================
    // TAB 2: TOPIC MANAGEMENT
    // ===================================================================
    private JPanel buildTopicManagementTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 16));
        tab.setBackground(UITheme.BG_LIGHT);
        tab.setBorder(new EmptyBorder(16, 24, 20, 24));

        JPanel card = UITheme.createCardPanel(new BorderLayout(0, 12));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.setOpaque(false);

        JLabel filterLbl = new JLabel("Filter Subject:");
        filterLbl.setFont(UITheme.FONT_BOLD);
        topicSubjectFilterCombo = new JComboBox<>(new String[]{"All Subjects"});
        topicSubjectFilterCombo.addActionListener(e -> filterTopicsTable());

        JButton addBtn = UITheme.createButton("+ Add Topic", UITheme.PRIMARY, Color.WHITE, 12, true);
        JButton editBtn = UITheme.createButton("Edit Topic", UITheme.SECONDARY, Color.WHITE, 12, false);
        JButton togglePubBtn = UITheme.createButton("Toggle Publish", new Color(100, 116, 139), Color.WHITE, 12, false);
        JButton delBtn = UITheme.createButton("Delete Topic", UITheme.DANGER, Color.WHITE, 12, false);

        addBtn.addActionListener(e -> showAddOrEditTopicDialog(null));
        editBtn.addActionListener(e -> {
            Topic t = getSelectedTopic();
            if (t != null) showAddOrEditTopicDialog(t);
        });
        togglePubBtn.addActionListener(e -> toggleTopicPublish());
        delBtn.addActionListener(e -> deleteSelectedTopic());

        toolbar.add(filterLbl);
        toolbar.add(topicSubjectFilterCombo);
        toolbar.add(Box.createHorizontalStrut(15));
        toolbar.add(addBtn);
        toolbar.add(editBtn);
        toolbar.add(togglePubBtn);
        toolbar.add(delBtn);
        card.add(toolbar, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID", "Subject", "Topic Name", "Description", "Order", "Status", "Materials"};
        topicsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        topicsTable = new JTable(topicsModel);
        UITheme.styleTable(topicsTable);
        topicsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        topicsTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        topicsTable.getColumnModel().getColumn(2).setPreferredWidth(220);

        card.add(new JScrollPane(topicsTable), BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private Topic getSelectedTopic() {
        int row = topicsTable.getSelectedRow();
        if (row < 0 || row >= allTopics.size()) {
            JOptionPane.showMessageDialog(this, "Please select a topic from the table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return allTopics.get(row);
    }

    private void filterTopicsTable() {
        topicsModel.setRowCount(0);
        String selSubj = (String) topicSubjectFilterCombo.getSelectedItem();
        Long filterId = null;
        if (selSubj != null && !selSubj.equals("All Subjects")) {
            for (Subject s : allSubjects) {
                if (s.getName().equals(selSubj)) {
                    filterId = s.getId();
                    break;
                }
            }
        }

        for (Topic t : allTopics) {
            if (filterId != null && !filterId.equals(t.getSubjectId())) {
                continue;
            }
            String subjName = allSubjects.stream().filter(s -> s.getId().equals(t.getSubjectId())).map(Subject::getName).findFirst().orElse("Subject #" + t.getSubjectId());
            topicsModel.addRow(new Object[]{
                    t.getId(),
                    subjName,
                    t.getName(),
                    t.getDescription(),
                    t.getDisplayOrder(),
                    t.isPublished() ? "Published" : "Draft",
                    "-"
            });
        }
    }

    private void showAddOrEditTopicDialog(Topic existing) {
        if (allSubjects.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please create at least one subject first.", "No Subjects", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean isEdit = existing != null;
        JDialog dlg = new JDialog(this, isEdit ? "Edit Topic" : "Create New Topic", true);
        dlg.setSize(500, 360);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<Subject> subjCombo = new JComboBox<>(allSubjects.toArray(new Subject[0]));
        if (isEdit) {
            for (int i = 0; i < allSubjects.size(); i++) {
                if (allSubjects.get(i).getId().equals(existing.getSubjectId())) {
                    subjCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        JTextField nameField = new JTextField(isEdit ? existing.getName() : "");
        JTextArea descArea = new JTextArea(isEdit ? existing.getDescription() : "", 3, 20);
        JSpinner orderSpinner = new JSpinner(new SpinnerNumberModel(isEdit ? existing.getDisplayOrder() : 1, 1, 999, 1));
        JCheckBox pubCheck = new JCheckBox("Published", isEdit ? existing.isPublished() : true);

        g.gridx = 0; g.gridy = 0; form.add(new JLabel("Parent Subject:"), g);
        g.gridx = 1; form.add(subjCombo, g);

        g.gridx = 0; g.gridy = 1; form.add(new JLabel("Topic Name:"), g);
        g.gridx = 1; form.add(nameField, g);

        g.gridx = 0; g.gridy = 2; form.add(new JLabel("Description:"), g);
        g.gridx = 1; form.add(new JScrollPane(descArea), g);

        g.gridx = 0; g.gridy = 3; form.add(new JLabel("Display Order:"), g);
        g.gridx = 1; form.add(orderSpinner, g);

        g.gridx = 1; g.gridy = 4; form.add(pubCheck, g);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton saveBtn = UITheme.createButton("Save Topic", UITheme.PRIMARY, Color.WHITE, 12, true);
        JButton cancelBtn = UITheme.createButton("Cancel", new Color(241, 245, 249), UITheme.TEXT_MAIN, 12, false);

        saveBtn.addActionListener(e -> {
            Subject selSubj = (Subject) subjCombo.getSelectedItem();
            String name = nameField.getText().trim();
            if (selSubj == null || name.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Subject and Topic name are required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                if (isEdit) {
                    existing.setSubjectId(selSubj.getId());
                    existing.setName(name);
                    existing.setDescription(descArea.getText().trim());
                    existing.setDisplayOrder((Integer) orderSpinner.getValue());
                    existing.setPublished(pubCheck.isSelected());
                    topicService.updateTopic(adminUser, existing);
                } else {
                    Topic t = new Topic(selSubj.getId(), name, descArea.getText().trim(), (Integer) orderSpinner.getValue(), pubCheck.isSelected());
                    topicService.createTopic(adminUser, t);
                }
                dlg.dispose();
                refreshAllData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Error saving topic: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancelBtn.addActionListener(e -> dlg.dispose());

        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        dlg.add(form, BorderLayout.CENTER);
        dlg.add(btnPanel, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void toggleTopicPublish() {
        Topic t = getSelectedTopic();
        if (t == null) return;
        try {
            t.setPublished(!t.isPublished());
            topicService.updateTopic(adminUser, t);
            refreshAllData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not update topic: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedTopic() {
        Topic t = getSelectedTopic();
        if (t == null) return;
        int opt = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete topic: " + t.getName() + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt != JOptionPane.YES_OPTION) return;
        try {
            topicService.deleteTopic(adminUser, t.getId());
            refreshAllData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not delete topic:\n" + ex.getMessage(), "Delete Blocked", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===================================================================
    // TAB 3: STUDY MATERIAL MANAGEMENT
    // ===================================================================
    private JPanel buildMaterialManagementTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 16));
        tab.setBackground(UITheme.BG_LIGHT);
        tab.setBorder(new EmptyBorder(16, 24, 20, 24));

        JPanel card = UITheme.createCardPanel(new BorderLayout(0, 12));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.setOpaque(false);

        JLabel filterLbl = new JLabel("Filter Topic:");
        filterLbl.setFont(UITheme.FONT_BOLD);
        materialTopicFilterCombo = new JComboBox<>(new String[]{"All Topics"});
        materialTopicFilterCombo.addActionListener(e -> filterMaterialsTable());

        JButton addBtn = UITheme.createButton("+ Add Study Notes", UITheme.PRIMARY, Color.WHITE, 12, true);
        JButton editBtn = UITheme.createButton("Edit Material", UITheme.SECONDARY, Color.WHITE, 12, false);
        JButton togglePubBtn = UITheme.createButton("Toggle Publish", new Color(100, 116, 139), Color.WHITE, 12, false);
        JButton delBtn = UITheme.createButton("Delete", UITheme.DANGER, Color.WHITE, 12, false);

        addBtn.addActionListener(e -> showAddOrEditMaterialDialog(null));
        editBtn.addActionListener(e -> {
            StudyMaterial m = getSelectedMaterial();
            if (m != null) showAddOrEditMaterialDialog(m);
        });
        togglePubBtn.addActionListener(e -> toggleMaterialPublish());
        delBtn.addActionListener(e -> deleteSelectedMaterial());

        toolbar.add(filterLbl);
        toolbar.add(materialTopicFilterCombo);
        toolbar.add(Box.createHorizontalStrut(15));
        toolbar.add(addBtn);
        toolbar.add(editBtn);
        toolbar.add(togglePubBtn);
        toolbar.add(delBtn);
        card.add(toolbar, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID", "Topic", "Title", "Resource URL", "Attachment", "Order", "Status"};
        materialsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        materialsTable = new JTable(materialsModel);
        UITheme.styleTable(materialsTable);
        materialsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        materialsTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        materialsTable.getColumnModel().getColumn(2).setPreferredWidth(260);

        card.add(new JScrollPane(materialsTable), BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private StudyMaterial getSelectedMaterial() {
        int row = materialsTable.getSelectedRow();
        if (row < 0 || row >= allMaterials.size()) {
            JOptionPane.showMessageDialog(this, "Please select a study material from the table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return allMaterials.get(row);
    }

    private void filterMaterialsTable() {
        materialsModel.setRowCount(0);
        String selTopic = (String) materialTopicFilterCombo.getSelectedItem();
        Long filterId = null;
        if (selTopic != null && !selTopic.equals("All Topics")) {
            for (Topic t : allTopics) {
                if (t.getName().equals(selTopic)) {
                    filterId = t.getId();
                    break;
                }
            }
        }

        for (StudyMaterial m : allMaterials) {
            if (filterId != null && !filterId.equals(m.getTopicId())) continue;

            String topicName = allTopics.stream().filter(t -> t.getId().equals(m.getTopicId())).map(Topic::getName).findFirst().orElse("Topic #" + m.getTopicId());
            materialsModel.addRow(new Object[]{
                    m.getId(),
                    topicName,
                    m.getTitle(),
                    m.getResourceUrl() != null ? m.getResourceUrl() : "-",
                    m.getAttachmentPath() != null ? m.getAttachmentPath() : "-",
                    m.getDisplayOrder(),
                    m.isPublished() ? "Published" : "Draft"
            });
        }
    }

    private void showAddOrEditMaterialDialog(StudyMaterial existing) {
        if (allTopics.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please create at least one topic first.", "No Topics", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean isEdit = existing != null;
        JDialog dlg = new JDialog(this, isEdit ? "Edit Study Material" : "Add Study Material", true);
        dlg.setSize(620, 560);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<Topic> topicCombo = new JComboBox<>(allTopics.toArray(new Topic[0]));
        if (isEdit) {
            for (int i = 0; i < allTopics.size(); i++) {
                if (allTopics.get(i).getId().equals(existing.getTopicId())) {
                    topicCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        JTextField titleField = new JTextField(isEdit ? existing.getTitle() : "");
        JTextArea contentArea = new JTextArea(isEdit ? existing.getContent() : "", 10, 30);
        contentArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        JTextField urlField = new JTextField(isEdit && existing.getResourceUrl() != null ? existing.getResourceUrl() : "");
        JTextField attachField = new JTextField(isEdit && existing.getAttachmentPath() != null ? existing.getAttachmentPath() : "");
        attachField.setEditable(false);
        JButton chooseFileBtn = new JButton("Choose Attachment...");
        chooseFileBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(dlg) == JFileChooser.APPROVE_OPTION) {
                try {
                    File sel = fc.getSelectedFile();
                    String savedPath = learningService.saveAttachmentSafely(sel);
                    attachField.setText(savedPath);
                    JOptionPane.showMessageDialog(dlg, "Attachment saved safely into platform storage:\n" + savedPath, "File Validated", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dlg, "Attachment rejected: " + ex.getMessage(), "File Validation Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JSpinner orderSpinner = new JSpinner(new SpinnerNumberModel(isEdit ? existing.getDisplayOrder() : 1, 1, 999, 1));
        JCheckBox pubCheck = new JCheckBox("Published (Visible to Students)", isEdit ? existing.isPublished() : true);

        g.gridx = 0; g.gridy = 0; form.add(new JLabel("Parent Topic:"), g);
        g.gridx = 1; form.add(topicCombo, g);

        g.gridx = 0; g.gridy = 1; form.add(new JLabel("Material Title:"), g);
        g.gridx = 1; form.add(titleField, g);

        g.gridx = 0; g.gridy = 2; form.add(new JLabel("Notes & Code:"), g);
        g.gridx = 1; form.add(new JScrollPane(contentArea), g);

        g.gridx = 0; g.gridy = 3; form.add(new JLabel("Resource URL:"), g);
        g.gridx = 1; form.add(urlField, g);

        g.gridx = 0; g.gridy = 4; form.add(new JLabel("Attachment File:"), g);
        JPanel attachRow = new JPanel(new BorderLayout(6, 0));
        attachRow.add(attachField, BorderLayout.CENTER);
        attachRow.add(chooseFileBtn, BorderLayout.EAST);
        g.gridx = 1; form.add(attachRow, g);

        g.gridx = 0; g.gridy = 5; form.add(new JLabel("Display Order:"), g);
        g.gridx = 1; form.add(orderSpinner, g);

        g.gridx = 1; g.gridy = 6; form.add(pubCheck, g);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton saveBtn = UITheme.createButton("Save Material", UITheme.PRIMARY, Color.WHITE, 12, true);
        JButton cancelBtn = UITheme.createButton("Cancel", new Color(241, 245, 249), UITheme.TEXT_MAIN, 12, false);

        saveBtn.addActionListener(e -> {
            Topic selTopic = (Topic) topicCombo.getSelectedItem();
            String title = titleField.getText().trim();
            if (selTopic == null || title.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Topic and Title are required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                if (isEdit) {
                    existing.setTopicId(selTopic.getId());
                    existing.setTitle(title);
                    existing.setContent(contentArea.getText());
                    existing.setResourceUrl(urlField.getText().trim().isEmpty() ? null : urlField.getText().trim());
                    existing.setAttachmentPath(attachField.getText().trim().isEmpty() ? null : attachField.getText().trim());
                    existing.setDisplayOrder((Integer) orderSpinner.getValue());
                    existing.setPublished(pubCheck.isSelected());
                    learningService.updateMaterial(adminUser, existing);
                } else {
                    StudyMaterial m = new StudyMaterial(selTopic.getId(), title, contentArea.getText(),
                            urlField.getText().trim().isEmpty() ? null : urlField.getText().trim(),
                            attachField.getText().trim().isEmpty() ? null : attachField.getText().trim(),
                            "DOCUMENT", (Integer) orderSpinner.getValue(), pubCheck.isSelected());
                    learningService.createMaterial(adminUser, m);
                }
                dlg.dispose();
                refreshAllData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Error saving study material: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancelBtn.addActionListener(e -> dlg.dispose());

        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        dlg.add(new JScrollPane(form), BorderLayout.CENTER);
        dlg.add(btnPanel, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void toggleMaterialPublish() {
        StudyMaterial m = getSelectedMaterial();
        if (m == null) return;
        try {
            m.setPublished(!m.isPublished());
            learningService.updateMaterial(adminUser, m);
            refreshAllData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not update material: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedMaterial() {
        StudyMaterial m = getSelectedMaterial();
        if (m == null) return;
        int opt = JOptionPane.showConfirmDialog(this, "Delete study material: " + m.getTitle() + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt != JOptionPane.YES_OPTION) return;
        try {
            learningService.deleteMaterial(adminUser, m.getId());
            refreshAllData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not delete material: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===================================================================
    // TAB 4: QUESTION BANK
    // ===================================================================
    private JPanel buildQuestionBankTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 16));
        tab.setBackground(UITheme.BG_LIGHT);
        tab.setBorder(new EmptyBorder(16, 24, 20, 24));

        JPanel card = UITheme.createCardPanel(new BorderLayout(0, 12));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.setOpaque(false);

        JLabel filterLbl = new JLabel("Filter Topic:");
        filterLbl.setFont(UITheme.FONT_BOLD);
        questionTopicFilterCombo = new JComboBox<>(new String[]{"All Topics"});
        questionTopicFilterCombo.addActionListener(e -> filterQuestionsTable());

        JButton addBtn = UITheme.createButton("+ New MCQ Question", UITheme.PRIMARY, Color.WHITE, 12, true);
        JButton editBtn = UITheme.createButton("Edit Question", UITheme.SECONDARY, Color.WHITE, 12, false);
        JButton previewBtn = UITheme.createButton("Preview Question", new Color(100, 116, 139), Color.WHITE, 12, false);
        JButton delBtn = UITheme.createButton("Delete", UITheme.DANGER, Color.WHITE, 12, false);

        addBtn.addActionListener(e -> showAddOrEditQuestionDialog(null));
        editBtn.addActionListener(e -> {
            Question q = getSelectedQuestion();
            if (q != null) showAddOrEditQuestionDialog(q);
        });
        previewBtn.addActionListener(e -> previewSelectedQuestion());
        delBtn.addActionListener(e -> deleteSelectedQuestion());

        toolbar.add(filterLbl);
        toolbar.add(questionTopicFilterCombo);
        toolbar.add(Box.createHorizontalStrut(15));
        toolbar.add(addBtn);
        toolbar.add(editBtn);
        toolbar.add(previewBtn);
        toolbar.add(delBtn);
        card.add(toolbar, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID", "Exam", "Topic", "Question Snippet", "Correct Answer", "Marks", "Status"};
        questionBankModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        questionBankTable = new JTable(questionBankModel);
        UITheme.styleTable(questionBankTable);
        questionBankTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        questionBankTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        questionBankTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        questionBankTable.getColumnModel().getColumn(3).setPreferredWidth(320);

        card.add(new JScrollPane(questionBankTable), BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private Question getSelectedQuestion() {
        int row = questionBankTable.getSelectedRow();
        if (row < 0 || row >= allQuestions.size()) {
            JOptionPane.showMessageDialog(this, "Please select a question from the table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return allQuestions.get(row);
    }

    private void filterQuestionsTable() {
        questionBankModel.setRowCount(0);
        String selTopic = (String) questionTopicFilterCombo.getSelectedItem();
        Long filterId = null;
        if (selTopic != null && !selTopic.equals("All Topics")) {
            for (Topic t : allTopics) {
                if (t.getName().equals(selTopic)) {
                    filterId = t.getId();
                    break;
                }
            }
        }

        for (Question q : allQuestions) {
            if (filterId != null && !filterId.equals(q.getTopicId())) continue;

            String examTitle = allExams.stream().filter(e -> e.getId().equals(q.getExamId())).map(Exam::getTitle).findFirst().orElse("Exam #" + q.getExamId());
            String topicName = allTopics.stream().filter(t -> t.getId().equals(q.getTopicId())).map(Topic::getName).findFirst().orElse(q.getTopicId() != null ? "Topic #" + q.getTopicId() : "General");

            String snippet = q.getQuestionText() != null ? (q.getQuestionText().length() > 50 ? q.getQuestionText().substring(0, 47) + "..." : q.getQuestionText()) : "";

            questionBankModel.addRow(new Object[]{
                    q.getId(),
                    examTitle,
                    topicName,
                    snippet,
                    "Option " + q.getCorrectOption(),
                    q.getMarks(),
                    q.isPublished() ? "Published" : "Draft"
            });
        }
    }

    private void showAddOrEditQuestionDialog(Question existing) {
        if (allExams.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please create at least one examination first.", "No Exams", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean isEdit = existing != null;
        JDialog dlg = new JDialog(this, isEdit ? "Edit Question" : "Add Question to Bank", true);
        dlg.setSize(620, 600);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<Exam> examCombo = new JComboBox<>(allExams.toArray(new Exam[0]));
        JComboBox<Topic> topicCombo = new JComboBox<>(allTopics.toArray(new Topic[0]));
        if (isEdit) {
            for (int i = 0; i < allExams.size(); i++) {
                if (allExams.get(i).getId().equals(existing.getExamId())) {
                    examCombo.setSelectedIndex(i);
                    break;
                }
            }
            if (existing.getTopicId() != null) {
                for (int i = 0; i < allTopics.size(); i++) {
                    if (allTopics.get(i).getId().equals(existing.getTopicId())) {
                        topicCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }

        JTextArea questionTextArea = new JTextArea(isEdit ? existing.getQuestionText() : "", 4, 30);
        JTextField optA = new JTextField(isEdit ? existing.getOptionA() : "");
        JTextField optB = new JTextField(isEdit ? existing.getOptionB() : "");
        JTextField optC = new JTextField(isEdit ? existing.getOptionC() : "");
        JTextField optD = new JTextField(isEdit ? existing.getOptionD() : "");
        JComboBox<Question.Option> correctCombo = new JComboBox<>(Question.Option.values());
        if (isEdit) correctCombo.setSelectedItem(existing.getCorrectOption());

        JSpinner marksSpinner = new JSpinner(new SpinnerNumberModel(isEdit ? existing.getMarks() : 1, 1, 100, 1));
        JTextArea expArea = new JTextArea(isEdit ? existing.getExplanation() : "", 3, 30);
        JCheckBox pubCheck = new JCheckBox("Published", isEdit ? existing.isPublished() : true);

        g.gridx = 0; g.gridy = 0; form.add(new JLabel("Target Exam:"), g);
        g.gridx = 1; form.add(examCombo, g);

        g.gridx = 0; g.gridy = 1; form.add(new JLabel("Associated Topic:"), g);
        g.gridx = 1; form.add(topicCombo, g);

        g.gridx = 0; g.gridy = 2; form.add(new JLabel("Question Text:"), g);
        g.gridx = 1; form.add(new JScrollPane(questionTextArea), g);

        g.gridx = 0; g.gridy = 3; form.add(new JLabel("Option A:"), g);
        g.gridx = 1; form.add(optA, g);

        g.gridx = 0; g.gridy = 4; form.add(new JLabel("Option B:"), g);
        g.gridx = 1; form.add(optB, g);

        g.gridx = 0; g.gridy = 5; form.add(new JLabel("Option C:"), g);
        g.gridx = 1; form.add(optC, g);

        g.gridx = 0; g.gridy = 6; form.add(new JLabel("Option D:"), g);
        g.gridx = 1; form.add(optD, g);

        g.gridx = 0; g.gridy = 7; form.add(new JLabel("Correct Option:"), g);
        g.gridx = 1; form.add(correctCombo, g);

        g.gridx = 0; g.gridy = 8; form.add(new JLabel("Marks:"), g);
        g.gridx = 1; form.add(marksSpinner, g);

        g.gridx = 0; g.gridy = 9; form.add(new JLabel("Explanation:"), g);
        g.gridx = 1; form.add(new JScrollPane(expArea), g);

        g.gridx = 1; g.gridy = 10; form.add(pubCheck, g);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton saveBtn = UITheme.createButton("Save Question", UITheme.PRIMARY, Color.WHITE, 12, true);
        JButton cancelBtn = UITheme.createButton("Cancel", new Color(241, 245, 249), UITheme.TEXT_MAIN, 12, false);

        saveBtn.addActionListener(e -> {
            Exam selExam = (Exam) examCombo.getSelectedItem();
            Topic selTopic = (Topic) topicCombo.getSelectedItem();
            if (selExam == null || questionTextArea.getText().trim().isEmpty() ||
                    optA.getText().trim().isEmpty() || optB.getText().trim().isEmpty() ||
                    optC.getText().trim().isEmpty() || optD.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Question text and all 4 options are required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                if (isEdit) {
                    existing.setExamId(selExam.getId());
                    existing.setTopicId(selTopic != null ? selTopic.getId() : null);
                    existing.setQuestionText(questionTextArea.getText().trim());
                    existing.setOptionA(optA.getText().trim());
                    existing.setOptionB(optB.getText().trim());
                    existing.setOptionC(optC.getText().trim());
                    existing.setOptionD(optD.getText().trim());
                    existing.setCorrectOption((Question.Option) correctCombo.getSelectedItem());
                    existing.setMarks((Integer) marksSpinner.getValue());
                    existing.setExplanation(expArea.getText().trim());
                    existing.setPublished(pubCheck.isSelected());
                    examService.updateQuestion(existing);
                } else {
                    Question q = new Question(
                            null,
                            selExam.getId(),
                            selTopic != null ? selTopic.getId() : null,
                            questionTextArea.getText().trim(),
                            optA.getText().trim(),
                            optB.getText().trim(),
                            optC.getText().trim(),
                            optD.getText().trim(),
                            (Question.Option) correctCombo.getSelectedItem(),
                            (Integer) marksSpinner.getValue(),
                            expArea.getText().trim(),
                            pubCheck.isSelected(),
                            null
                    );
                    examService.createQuestion(q);
                }
                dlg.dispose();
                refreshAllData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Error saving question: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancelBtn.addActionListener(e -> dlg.dispose());

        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        dlg.add(new JScrollPane(form), BorderLayout.CENTER);
        dlg.add(btnPanel, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void previewSelectedQuestion() {
        Question q = getSelectedQuestion();
        if (q == null) return;
        String preview = String.format("""
                QUESTION ID: %d
                
                %s
                
                A) %s
                B) %s
                C) %s
                D) %s
                
                [CONFIDENTIAL] Correct Answer: %s (%d Mark%s)
                
                Explanation:
                %s
                """,
                q.getId(),
                q.getQuestionText(),
                q.getOptionA(),
                q.getOptionB(),
                q.getOptionC(),
                q.getOptionD(),
                q.getCorrectOption(),
                q.getMarks(),
                q.getMarks() > 1 ? "s" : "",
                q.getExplanation() != null ? q.getExplanation() : "None provided.");

        JTextArea ta = new JTextArea(preview, 12, 35);
        ta.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Question Preview", JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteSelectedQuestion() {
        Question q = getSelectedQuestion();
        if (q == null) return;
        int opt = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete Question #" + q.getId() + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt != JOptionPane.YES_OPTION) return;
        try {
            examService.deleteQuestion(q.getId());
            refreshAllData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not delete question: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===================================================================
    // TAB 5: EXAM & QUIZ MANAGEMENT
    // ===================================================================
    private JPanel buildExamManagementTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 16));
        tab.setBackground(UITheme.BG_LIGHT);
        tab.setBorder(new EmptyBorder(16, 24, 20, 24));

        JPanel card = UITheme.createCardPanel(new BorderLayout(0, 12));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.setOpaque(false);

        JButton addBtn = UITheme.createButton("+ Create Assessment", UITheme.PRIMARY, Color.WHITE, 12, true);
        JButton editBtn = UITheme.createButton("Edit Assessment", UITheme.SECONDARY, Color.WHITE, 12, false);
        JButton questionsBtn = UITheme.createButton("Manage Questions", new Color(100, 116, 139), Color.WHITE, 12, false);
        JButton toggleActiveBtn = UITheme.createButton("Toggle Active", UITheme.ACCENT, Color.WHITE, 12, false);
        JButton delBtn = UITheme.createButton("Delete", UITheme.DANGER, Color.WHITE, 12, false);

        addBtn.addActionListener(e -> showAddOrEditExamDialog(null));
        editBtn.addActionListener(e -> {
            Exam ex = getSelectedExam();
            if (ex != null) showAddOrEditExamDialog(ex);
        });
        questionsBtn.addActionListener(e -> {
            Exam ex = getSelectedExam();
            if (ex != null) new ManageQuestionsDialog(this, ex, examService).setVisible(true);
        });
        toggleActiveBtn.addActionListener(e -> toggleExamActive());
        delBtn.addActionListener(e -> deleteSelectedExam());

        toolbar.add(addBtn);
        toolbar.add(editBtn);
        toolbar.add(questionsBtn);
        toolbar.add(toggleActiveBtn);
        toolbar.add(delBtn);
        card.add(toolbar, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID", "Assessment Title", "Type", "Subject", "Duration", "Total Marks", "Pass Marks", "Status"};
        examsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        examsTable = new JTable(examsModel);
        UITheme.styleTable(examsTable);
        examsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        examsTable.getColumnModel().getColumn(1).setPreferredWidth(260);
        examsTable.getColumnModel().getColumn(2).setPreferredWidth(120);

        card.add(new JScrollPane(examsTable), BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private Exam getSelectedExam() {
        int row = examsTable.getSelectedRow();
        if (row < 0 || row >= allExams.size()) {
            JOptionPane.showMessageDialog(this, "Please select an examination from the table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return allExams.get(row);
    }

    private void showAddOrEditExamDialog(Exam existing) {
        boolean isEdit = existing != null;
        JDialog dlg = new JDialog(this, isEdit ? "Edit Assessment" : "Create New Assessment", true);
        dlg.setSize(560, 480);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        JTextField titleField = new JTextField(isEdit ? existing.getTitle() : "");
        JTextArea descArea = new JTextArea(isEdit ? existing.getDescription() : "", 3, 20);
        JComboBox<Exam.AssessmentType> typeCombo = new JComboBox<>(Exam.AssessmentType.values());
        if (isEdit) typeCombo.setSelectedItem(existing.getAssessmentType());

        JComboBox<Subject> subjCombo = new JComboBox<>(allSubjects.toArray(new Subject[0]));
        JComboBox<Topic> topicCombo = new JComboBox<>(allTopics.toArray(new Topic[0]));

        if (isEdit) {
            if (existing.getSubjectId() != null) {
                for (int i = 0; i < allSubjects.size(); i++) {
                    if (allSubjects.get(i).getId().equals(existing.getSubjectId())) {
                        subjCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
            if (existing.getTopicId() != null) {
                for (int i = 0; i < allTopics.size(); i++) {
                    if (allTopics.get(i).getId().equals(existing.getTopicId())) {
                        topicCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }

        JSpinner durationSpinner = new JSpinner(new SpinnerNumberModel(isEdit ? existing.getDurationMinutes() : 15, 1, 300, 1));
        JSpinner totalMarksSpinner = new JSpinner(new SpinnerNumberModel(isEdit ? existing.getTotalMarks() : 100, 1, 1000, 1));
        JSpinner passMarksSpinner = new JSpinner(new SpinnerNumberModel(isEdit ? existing.getPassingMarks() : 40, 1, 1000, 1));
        JCheckBox activeCheck = new JCheckBox("Active (Open to Students)", isEdit ? existing.isActive() : true);

        g.gridx = 0; g.gridy = 0; form.add(new JLabel("Assessment Title:"), g);
        g.gridx = 1; form.add(titleField, g);

        g.gridx = 0; g.gridy = 1; form.add(new JLabel("Assessment Type:"), g);
        g.gridx = 1; form.add(typeCombo, g);

        g.gridx = 0; g.gridy = 2; form.add(new JLabel("Subject:"), g);
        g.gridx = 1; form.add(subjCombo, g);

        g.gridx = 0; g.gridy = 3; form.add(new JLabel("Topic:"), g);
        g.gridx = 1; form.add(topicCombo, g);

        g.gridx = 0; g.gridy = 4; form.add(new JLabel("Description:"), g);
        g.gridx = 1; form.add(new JScrollPane(descArea), g);

        g.gridx = 0; g.gridy = 5; form.add(new JLabel("Duration (Minutes):"), g);
        g.gridx = 1; form.add(durationSpinner, g);

        g.gridx = 0; g.gridy = 6; form.add(new JLabel("Total Marks:"), g);
        g.gridx = 1; form.add(totalMarksSpinner, g);

        g.gridx = 0; g.gridy = 7; form.add(new JLabel("Passing Marks:"), g);
        g.gridx = 1; form.add(passMarksSpinner, g);

        g.gridx = 1; g.gridy = 8; form.add(activeCheck, g);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton saveBtn = UITheme.createButton("Save Assessment", UITheme.PRIMARY, Color.WHITE, 12, true);
        JButton cancelBtn = UITheme.createButton("Cancel", new Color(241, 245, 249), UITheme.TEXT_MAIN, 12, false);

        saveBtn.addActionListener(e -> {
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Title cannot be empty.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Subject selSubj = (Subject) subjCombo.getSelectedItem();
            Topic selTopic = (Topic) topicCombo.getSelectedItem();

            try {
                if (isEdit) {
                    existing.setTitle(title);
                    existing.setSubjectId(selSubj != null ? selSubj.getId() : null);
                    existing.setTopicId(selTopic != null ? selTopic.getId() : null);
                    existing.setDescription(descArea.getText().trim());
                    existing.setAssessmentType((Exam.AssessmentType) typeCombo.getSelectedItem());
                    existing.setDurationMinutes((Integer) durationSpinner.getValue());
                    existing.setTotalMarks((Integer) totalMarksSpinner.getValue());
                    existing.setPassingMarks((Integer) passMarksSpinner.getValue());
                    existing.setActive(activeCheck.isSelected());
                    examService.updateExam(existing);
                } else {
                    Exam ex = new Exam(
                            null,
                            selSubj != null ? selSubj.getId() : null,
                            selTopic != null ? selTopic.getId() : null,
                            title,
                            descArea.getText().trim(),
                            (Integer) durationSpinner.getValue(),
                            (Integer) totalMarksSpinner.getValue(),
                            (Integer) passMarksSpinner.getValue(),
                            (Exam.AssessmentType) typeCombo.getSelectedItem(),
                            activeCheck.isSelected(),
                            null, null
                    );
                    examService.createExam(ex);
                }
                dlg.dispose();
                refreshAllData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Error saving exam: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancelBtn.addActionListener(e -> dlg.dispose());

        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        dlg.add(new JScrollPane(form), BorderLayout.CENTER);
        dlg.add(btnPanel, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void toggleExamActive() {
        Exam ex = getSelectedExam();
        if (ex == null) return;
        try {
            ex.setActive(!ex.isActive());
            examService.updateExam(ex);
            refreshAllData();
        } catch (Exception ex2) {
            JOptionPane.showMessageDialog(this, "Could not update exam: " + ex2.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedExam() {
        Exam ex = getSelectedExam();
        if (ex == null) return;
        int opt = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete examination: " + ex.getTitle() + "?\nAll candidate attempts will be preserved or cascade deleted.", "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt != JOptionPane.YES_OPTION) return;
        try {
            examService.deleteExam(ex.getId());
            refreshAllData();
        } catch (Exception ex2) {
            JOptionPane.showMessageDialog(this, "Could not delete exam: " + ex2.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===================================================================
    // TAB 6: RESULTS & ANALYTICS
    // ===================================================================
    private JPanel buildAnalyticsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 16));
        tab.setBackground(UITheme.BG_LIGHT);
        tab.setBorder(new EmptyBorder(16, 24, 20, 24));

        JPanel statsGrid = new JPanel(new GridLayout(1, 3, 20, 0));
        statsGrid.setOpaque(false);

        totalAttemptsLbl = new JLabel("0");
        passRateLbl = new JLabel("0.0%");
        avgScoreLbl = new JLabel("0.0");

        statsGrid.add(createMetricCard("Total Exam Sessions", totalAttemptsLbl, UITheme.PRIMARY));
        statsGrid.add(createMetricCard("Passing Rate", passRateLbl, new Color(16, 185, 129)));
        statsGrid.add(createMetricCard("Average Score", avgScoreLbl, UITheme.ACCENT));

        JPanel card = UITheme.createCardPanel(new BorderLayout(0, 12));
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        toolbar.setOpaque(false);

        JLabel filterLbl = new JLabel("Filter Examination:");
        filterLbl.setFont(UITheme.FONT_BOLD);
        examFilterCombo = new JComboBox<>(new String[]{"All Examinations"});
        examFilterCombo.addActionListener(e -> filterResultsTable());

        toolbar.add(filterLbl);
        toolbar.add(examFilterCombo);
        card.add(toolbar, BorderLayout.NORTH);

        String[] cols = {"Session #", "Candidate", "Examination", "Score", "Status", "Completion Date"};
        resultsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        resultsTable = new JTable(resultsModel);
        UITheme.styleTable(resultsTable);

        card.add(new JScrollPane(resultsTable), BorderLayout.CENTER);

        JPanel mainWrap = new JPanel(new BorderLayout(0, 16));
        mainWrap.setOpaque(false);
        mainWrap.add(statsGrid, BorderLayout.NORTH);
        mainWrap.add(card, BorderLayout.CENTER);

        tab.add(mainWrap, BorderLayout.CENTER);
        return tab;
    }

    private JPanel createMetricCard(String title, JLabel valLbl, Color accent) {
        JPanel c = UITheme.createCardPanel(new BorderLayout(0, 8));
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                new EmptyBorder(14, 18, 14, 18)
        ));
        JLabel t = new JLabel(title);
        t.setFont(UITheme.FONT_SMALL);
        t.setForeground(UITheme.TEXT_MUTED);

        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valLbl.setForeground(UITheme.TEXT_MAIN);

        c.add(t, BorderLayout.NORTH);
        c.add(valLbl, BorderLayout.CENTER);
        return c;
    }

    private void filterResultsTable() {
        resultsModel.setRowCount(0);
        String sel = (String) examFilterCombo.getSelectedItem();
        Long filterId = null;
        if (sel != null && !sel.equals("All Examinations")) {
            try {
                filterId = Long.parseLong(sel.split(":")[0].trim());
            } catch (Exception ignored) {}
        }

        int total = 0;
        int passed = 0;
        int scoreSum = 0;

        for (StudentExam att : allAttempts) {
            if (filterId != null && !filterId.equals(att.getExamId())) continue;

            total++;
            scoreSum += att.getTotalScore();

            Optional<Exam> exOpt = allExams.stream().filter(e -> e.getId().equals(att.getExamId())).findFirst();
            String title = exOpt.map(Exam::getTitle).orElse("Exam #" + att.getExamId());
            int passMarks = exOpt.map(Exam::getPassingMarks).orElse(0);
            if (att.getTotalScore() >= passMarks) passed++;

            resultsModel.addRow(new Object[]{
                    att.getId(),
                    "Student #" + att.getStudentId(),
                    title,
                    att.getTotalScore() + (exOpt.isPresent() ? " / " + exOpt.get().getTotalMarks() : ""),
                    att.getStatus(),
                    att.getEndTime() != null ? att.getEndTime().toString() : "In Progress"
            });
        }

        totalAttemptsLbl.setText(String.valueOf(total));
        passRateLbl.setText(total > 0 ? String.format("%.1f%%", (double) passed / total * 100.0) : "0.0%");
        avgScoreLbl.setText(total > 0 ? String.format("%.1f", (double) scoreSum / total) : "0.0");
    }

    // ===================================================================
    // TAB 7: USER ACCOUNTS
    // ===================================================================
    private JPanel buildUserManagementTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 16));
        tab.setBackground(UITheme.BG_LIGHT);
        tab.setBorder(new EmptyBorder(16, 24, 20, 24));

        JPanel card = UITheme.createCardPanel(new BorderLayout(0, 12));
        String[] cols = {"User ID", "Username", "Full Name", "Email Address", "Role", "Registered Date"};
        usersModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        usersTable = new JTable(usersModel);
        UITheme.styleTable(usersTable);

        card.add(new JScrollPane(usersTable), BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    // ===================================================================
    // ASYNCHRONOUS REFRESH PIPELINE
    // ===================================================================
    public void refreshAllData() {
        statusBannerText.setText("Connecting to database and synchronizing platform records...");
        statusBannerText.setForeground(UITheme.TEXT_MUTED);
        statusBannerActionBtn.setVisible(false);
        refreshBtn.setEnabled(false);

        new Thread(() -> {
            boolean dbConnected = false;
            String dbCatalogName = "";
            SQLException sqlEx = null;

            List<Subject> fetchedSubjects = new ArrayList<>();
            List<Topic> fetchedTopics = new ArrayList<>();
            List<StudyMaterial> fetchedMaterials = new ArrayList<>();
            List<Question> fetchedQuestions = new ArrayList<>();
            List<Exam> fetchedExams = new ArrayList<>();
            List<StudentExam> fetchedAttempts = new ArrayList<>();
            List<User> fetchedUsers = new ArrayList<>();

            try {
                com.onlineexam.config.DatabaseConnection.ConnectionTestResult testResult =
                        com.onlineexam.config.DatabaseConnection.testConnection();
                dbConnected = testResult.successful();
                dbCatalogName = testResult.databaseName() != null ? testResult.databaseName() : "online_exam_db";

                if (dbConnected) {
                    fetchedSubjects = subjectService.getAllSubjects(adminUser);
                    fetchedTopics = topicService.getAllTopics(adminUser, null);
                    fetchedQuestions = examService.getAllQuestions();
                    fetchedExams = examService.getAllExams();
                    fetchedAttempts = attemptService.getAllAttempts();
                    fetchedUsers = authService.getAllUsers();

                    for (Topic t : fetchedTopics) {
                        fetchedMaterials.addAll(learningService.getAllMaterials(adminUser, t.getId()));
                    }
                } else {
                    sqlEx = new SQLException(testResult.message());
                }
            } catch (SQLException e) {
                sqlEx = e;
                LOGGER.log(Level.WARNING, "Error loading admin dashboard data", e);
            }

            final boolean isConnected = dbConnected;
            final String catalogName = dbCatalogName;
            final SQLException finalEx = sqlEx;
            final List<Subject> finalSubjs = fetchedSubjects;
            final List<Topic> finalTopics = fetchedTopics;
            final List<StudyMaterial> finalMats = fetchedMaterials;
            final List<Question> finalQuestions = fetchedQuestions;
            final List<Exam> finalExams = fetchedExams;
            final List<StudentExam> finalAttempts = fetchedAttempts;
            final List<User> finalUsers = fetchedUsers;

            SwingUtilities.invokeLater(() -> {
                refreshBtn.setEnabled(true);

                if (isConnected) {
                    statusBanner.setBackground(new Color(240, 253, 244));
                    statusBannerText.setText(" \u25CF Connected to MySQL (" + catalogName + ") - Administrator Mode");
                    statusBannerText.setForeground(new Color(22, 101, 52));
                    statusBannerActionBtn.setVisible(false);

                    allSubjects = finalSubjs;
                    allTopics = finalTopics;
                    allMaterials = finalMats;
                    allQuestions = finalQuestions;
                    allExams = finalExams;
                    allAttempts = finalAttempts;
                    allUsers = finalUsers;

                    // Update Subjects Table
                    subjectsModel.setRowCount(0);
                    for (Subject s : finalSubjs) {
                        long tCount = finalTopics.stream().filter(t -> t.getSubjectId().equals(s.getId())).count();
                        long eCount = finalExams.stream().filter(e -> s.getId().equals(e.getSubjectId())).count();
                        subjectsModel.addRow(new Object[]{
                                s.getId(), s.getName(), s.getDescription(), s.isPublished() ? "Published" : "Draft",
                                tCount, eCount, s.getCreatedAt() != null ? s.getCreatedAt().toString() : "-"
                        });
                    }

                    // Update Topic filter dropdown & table
                    topicSubjectFilterCombo.removeAllItems();
                    topicSubjectFilterCombo.addItem("All Subjects");
                    for (Subject s : finalSubjs) topicSubjectFilterCombo.addItem(s.getName());
                    filterTopicsTable();

                    // Update Materials filter dropdown & table
                    materialTopicFilterCombo.removeAllItems();
                    materialTopicFilterCombo.addItem("All Topics");
                    for (Topic t : finalTopics) materialTopicFilterCombo.addItem(t.getName());
                    filterMaterialsTable();

                    // Update Question Bank filter dropdown & table
                    questionTopicFilterCombo.removeAllItems();
                    questionTopicFilterCombo.addItem("All Topics");
                    for (Topic t : finalTopics) questionTopicFilterCombo.addItem(t.getName());
                    filterQuestionsTable();

                    // Update Exams Table
                    examsModel.setRowCount(0);
                    for (Exam ex : finalExams) {
                        String subjName = finalSubjs.stream().filter(s -> s.getId().equals(ex.getSubjectId())).map(Subject::getName).findFirst().orElse("-");
                        examsModel.addRow(new Object[]{
                                ex.getId(), ex.getTitle(), ex.getAssessmentType(), subjName,
                                ex.getDurationMinutes() + " mins", ex.getTotalMarks(), ex.getPassingMarks(),
                                ex.isActive() ? "Active" : "Draft"
                        });
                    }

                    // Update Analytics filter & table
                    examFilterCombo.removeAllItems();
                    examFilterCombo.addItem("All Examinations");
                    for (Exam ex : finalExams) {
                        examFilterCombo.addItem(ex.getId() + ": " + ex.getTitle());
                    }
                    filterResultsTable();

                    // Update Users Table
                    usersModel.setRowCount(0);
                    for (User u : finalUsers) {
                        usersModel.addRow(new Object[]{
                                u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getRole(),
                                u.getCreatedAt() != null ? u.getCreatedAt().toString() : "-"
                        });
                    }

                } else {
                    statusBanner.setBackground(new Color(254, 242, 242));
                    String reason = finalEx != null ? finalEx.getMessage() : "Database disconnected.";
                    statusBannerText.setText(" \u26A0 Database Error: " + reason);
                    statusBannerText.setForeground(new Color(153, 27, 27));
                    statusBannerActionBtn.setVisible(true);
                }
            });
        }).start();
    }

    private void logout() {
        int opt = JOptionPane.showConfirmDialog(this, "Are you sure you want to log out of the administration portal?", "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            dispose();
            new LoginFrame(authService, examService, attemptService).setVisible(true);
        }
    }
}
