package com.onlineexam.ui;

import com.onlineexam.model.Exam;
import com.onlineexam.model.Question;
import com.onlineexam.service.ExamService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Administrative Question Bank Manager.
 * Allows creation, editing, and deletion of multiple-choice questions for an examination.
 */
public class ManageQuestionsDialog extends JDialog {

    private static final Logger LOGGER = Logger.getLogger(ManageQuestionsDialog.class.getName());

    private final Exam exam;
    private final ExamService examService;

    private JTable questionsTable;
    private DefaultTableModel tableModel;
    private List<Question> questionList;

    public ManageQuestionsDialog(Window owner, Exam exam, ExamService examService) {
        super(owner, "Question Bank Management - " + exam.getTitle(), ModalityType.APPLICATION_MODAL);
        this.exam = exam;
        this.examService = examService;

        setSize(950, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BG_LIGHT);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(16, 24, 16, 24));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel titleLbl = new JLabel("Question Bank: " + exam.getTitle());
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.PRIMARY);

        JLabel subLbl = new JLabel("Configure multiple-choice questions, options, point values, and explanations.");
        subLbl.setFont(UITheme.FONT_BODY);
        subLbl.setForeground(UITheme.TEXT_MUTED);

        titleBlock.add(titleLbl);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(subLbl);

        header.add(titleBlock, BorderLayout.CENTER);
        header.add(UITheme.createBadge("Total Marks: " + exam.getTotalMarks(), UITheme.ACCENT_LIGHT, UITheme.ACCENT), BorderLayout.EAST);

        // Table
        String[] columns = {"ID", "Question Text", "Option A", "Option B", "Option C", "Option D", "Correct", "Marks"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        questionsTable = new JTable(tableModel);
        UITheme.styleTable(questionsTable);
        questionsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        questionsTable.getColumnModel().getColumn(1).setPreferredWidth(260);
        questionsTable.getColumnModel().getColumn(6).setPreferredWidth(60);
        questionsTable.getColumnModel().getColumn(7).setPreferredWidth(60);

        JScrollPane scrollPane = new JScrollPane(questionsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);

        JPanel tableWrap = new JPanel(new BorderLayout());
        tableWrap.setOpaque(false);
        tableWrap.setBorder(new EmptyBorder(15, 24, 15, 24));
        tableWrap.add(scrollPane, BorderLayout.CENTER);

        // Action Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR));

        JButton addBtn = UITheme.createButton("+ Add Question", UITheme.SUCCESS_DARK, Color.WHITE, 12, true);
        JButton editBtn = UITheme.createButton("Edit Selected", UITheme.ACCENT, Color.WHITE, 12, true);
        JButton deleteBtn = UITheme.createButton("Delete Selected", UITheme.DANGER, Color.WHITE, 12, true);
        JButton closeBtn = UITheme.createButton("Close", new Color(241, 245, 249), UITheme.TEXT_MAIN, 12, false);

        addBtn.addActionListener(e -> showAddOrEditQuestionDialog(null));
        editBtn.addActionListener(e -> {
            int row = questionsTable.getSelectedRow();
            if (row >= 0 && row < questionList.size()) {
                showAddOrEditQuestionDialog(questionList.get(row));
            } else {
                JOptionPane.showMessageDialog(this, "Please select a question from the table to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            }
        });
        deleteBtn.addActionListener(e -> deleteSelectedQuestion());
        closeBtn.addActionListener(e -> dispose());

        toolbar.add(addBtn);
        toolbar.add(editBtn);
        toolbar.add(deleteBtn);
        toolbar.add(closeBtn);

        add(header, BorderLayout.NORTH);
        add(tableWrap, BorderLayout.CENTER);
        add(toolbar, BorderLayout.SOUTH);

        refreshQuestions();
    }

    private void refreshQuestions() {
        tableModel.setRowCount(0);
        try {
            questionList = examService.getExamQuestions(exam.getId());
            for (Question q : questionList) {
                tableModel.addRow(new Object[]{
                        q.getId(),
                        q.getQuestionText(),
                        q.getOptionA(),
                        q.getOptionB(),
                        q.getOptionC(),
                        q.getOptionD(),
                        q.getCorrectOption(),
                        q.getMarks()
                });
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load questions", e);
        }
    }

    private void deleteSelectedQuestion() {
        int row = questionsTable.getSelectedRow();
        if (row < 0 || row >= questionList.size()) {
            JOptionPane.showMessageDialog(this, "Please select a question to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Question q = questionList.get(row);
        int opt = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete question ID " + q.getId() + "?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            try {
                boolean ok = examService.deleteQuestion(q.getId());
                if (ok) {
                    JOptionPane.showMessageDialog(this, "Question deleted successfully.");
                    refreshQuestions();
                } else {
                    JOptionPane.showMessageDialog(this, "Could not delete question.", "Delete Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showAddOrEditQuestionDialog(Question existing) {
        boolean isEdit = (existing != null);
        JDialog form = new JDialog(this, (isEdit ? "Edit Question" : "Add New Question"), true);
        form.setSize(620, 560);
        form.setLocationRelativeTo(this);
        form.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(6, 6, 6, 6);

        // Question text
        g.gridx = 0; g.gridy = 0;
        panel.add(new JLabel("Question Text:"), g);
        g.gridx = 1;
        JTextArea qText = new JTextArea(isEdit ? existing.getQuestionText() : "", 3, 25);
        qText.setFont(UITheme.FONT_BODY);
        qText.setLineWrap(true);
        qText.setWrapStyleWord(true);
        JScrollPane qScroll = new JScrollPane(qText);
        panel.add(qScroll, g);

        // Options A, B, C, D
        g.gridx = 0; g.gridy = 1; panel.add(new JLabel("Option A:"), g);
        g.gridx = 1; JTextField optA = new JTextField(isEdit ? existing.getOptionA() : ""); panel.add(optA, g);

        g.gridx = 0; g.gridy = 2; panel.add(new JLabel("Option B:"), g);
        g.gridx = 1; JTextField optB = new JTextField(isEdit ? existing.getOptionB() : ""); panel.add(optB, g);

        g.gridx = 0; g.gridy = 3; panel.add(new JLabel("Option C:"), g);
        g.gridx = 1; JTextField optC = new JTextField(isEdit ? existing.getOptionC() : ""); panel.add(optC, g);

        g.gridx = 0; g.gridy = 4; panel.add(new JLabel("Option D:"), g);
        g.gridx = 1; JTextField optD = new JTextField(isEdit ? existing.getOptionD() : ""); panel.add(optD, g);

        // Correct Option
        g.gridx = 0; g.gridy = 5; panel.add(new JLabel("Correct Option:"), g);
        g.gridx = 1;
        JComboBox<Question.Option> correctCombo = new JComboBox<>(Question.Option.values());
        if (isEdit && existing.getCorrectOption() != null) correctCombo.setSelectedItem(existing.getCorrectOption());
        panel.add(correctCombo, g);

        // Marks
        g.gridx = 0; g.gridy = 6; panel.add(new JLabel("Marks:"), g);
        g.gridx = 1;
        JSpinner marksSpinner = new JSpinner(new SpinnerNumberModel(isEdit ? existing.getMarks() : 1, 1, 100, 1));
        panel.add(marksSpinner, g);

        // Explanation
        g.gridx = 0; g.gridy = 7; panel.add(new JLabel("Explanation:"), g);
        g.gridx = 1;
        JTextField explField = new JTextField(isEdit && existing.getExplanation() != null ? existing.getExplanation() : "");
        panel.add(explField, g);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 15));
        btnPanel.setBackground(Color.WHITE);
        JButton saveBtn = UITheme.createButton(isEdit ? "Update Question" : "Save Question", UITheme.ACCENT, Color.WHITE, 12, true);
        JButton cancelBtn = UITheme.createButton("Cancel", new Color(241, 245, 249), UITheme.TEXT_MAIN, 12, false);

        saveBtn.addActionListener(e -> {
            try {
                Question q = isEdit ? existing : new Question();
                q.setExamId(exam.getId());
                q.setQuestionText(qText.getText().trim());
                q.setOptionA(optA.getText().trim());
                q.setOptionB(optB.getText().trim());
                q.setOptionC(optC.getText().trim());
                q.setOptionD(optD.getText().trim());
                q.setCorrectOption((Question.Option) correctCombo.getSelectedItem());
                q.setMarks((Integer) marksSpinner.getValue());
                q.setExplanation(explField.getText().trim());

                if (isEdit) {
                    examService.updateQuestion(q);
                    JOptionPane.showMessageDialog(form, "Question updated successfully.");
                } else {
                    examService.createQuestion(q);
                    JOptionPane.showMessageDialog(form, "Question added successfully.");
                }
                form.dispose();
                refreshQuestions();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(form, "Error: " + ex.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancelBtn.addActionListener(e -> form.dispose());

        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        form.add(panel, BorderLayout.CENTER);
        form.add(btnPanel, BorderLayout.SOUTH);
        form.setVisible(true);
    }
}
