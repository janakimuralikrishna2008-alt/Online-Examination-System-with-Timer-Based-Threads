package com.onlineexam.ui;

import com.onlineexam.model.ExamResult;
import com.onlineexam.model.Question;
import com.onlineexam.model.StudentAnswer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modern Performance Scorecard and Detailed Examination Review Window.
 * Displays overall scoring metrics, pass/fail status, and question-by-question breakdown.
 */
public class ExamResultDialog extends JDialog {

    public ExamResultDialog(Window owner, ExamResult result, List<Question> questions) {
        super(owner, "Examination Performance Scorecard - " + result.examTitle(), ModalityType.APPLICATION_MODAL);
        setSize(880, 680);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BG_LIGHT);

        // Map candidate answers by question ID
        Map<Long, Question.Option> answersMap = new HashMap<>();
        for (StudentAnswer sa : result.evaluatedAnswers()) {
            if (sa.getQuestionId() != null) {
                answersMap.put(sa.getQuestionId(), sa.getSelectedOption());
            }
        }

        // Header Section
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel titleLbl = new JLabel(result.examTitle());
        titleLbl.setFont(UITheme.FONT_TITLE_LARGE);
        titleLbl.setForeground(UITheme.PRIMARY);

        JLabel subLbl = new JLabel("Examination Result & Detailed Answer Key Analysis");
        subLbl.setFont(UITheme.FONT_BODY);
        subLbl.setForeground(UITheme.TEXT_MUTED);

        titleBlock.add(titleLbl);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(subLbl);

        JPanel badgesBlock = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        badgesBlock.setOpaque(false);

        if (result.passed()) {
            badgesBlock.add(UITheme.createBadge("✓ PASSED", UITheme.SUCCESS_LIGHT, UITheme.SUCCESS_DARK));
        } else {
            badgesBlock.add(UITheme.createBadge("✗ NOT PASSED", UITheme.DANGER_LIGHT, UITheme.DANGER_DARK));
        }

        if (result.autoSubmitted()) {
            badgesBlock.add(UITheme.createBadge("AUTO-SUBMITTED (TIMER EXPIRED)", UITheme.WARNING_LIGHT, UITheme.WARNING_DARK));
        } else {
            badgesBlock.add(UITheme.createBadge("MANUAL SUBMISSION", UITheme.ACCENT_LIGHT, UITheme.ACCENT));
        }

        headerPanel.add(titleBlock, BorderLayout.CENTER);
        headerPanel.add(badgesBlock, BorderLayout.EAST);

        // Summary Metric Cards (4 cards)
        JPanel summaryGrid = new JPanel(new GridLayout(1, 4, 12, 12));
        summaryGrid.setOpaque(false);
        summaryGrid.setBorder(new EmptyBorder(15, 25, 10, 25));

        summaryGrid.add(createMetricCard("TOTAL SCORE",
                result.earnedMarks() + " / " + result.totalPossibleMarks(),
                "Pass req: " + result.passingMarks() + " marks",
                result.passed() ? UITheme.SUCCESS_DARK : UITheme.DANGER_DARK));

        summaryGrid.add(createMetricCard("PERCENTAGE",
                result.getFormattedPercentage(),
                result.passed() ? "Goal Achieved" : "Below Threshold",
                UITheme.ACCENT));

        summaryGrid.add(createMetricCard("CORRECT ANSWERS",
                result.correctCount() + " / " + result.totalQuestions(),
                "Accuracy: " + (result.totalQuestions() > 0 ? (result.correctCount() * 100 / result.totalQuestions()) + "%" : "0%"),
                UITheme.SUCCESS_DARK));

        summaryGrid.add(createMetricCard("INCORRECT / SKIPPED",
                result.incorrectCount() + " / " + result.unansweredCount(),
                "Incorrect: " + result.incorrectCount() + " | Skipped: " + result.unansweredCount(),
                UITheme.SECONDARY));

        // Detailed Review Panel (Scrollable list of questions)
        JPanel reviewContainer = new JPanel();
        reviewContainer.setLayout(new BoxLayout(reviewContainer, BoxLayout.Y_AXIS));
        reviewContainer.setOpaque(false);
        reviewContainer.setBorder(new EmptyBorder(10, 25, 20, 25));

        JLabel sectionTitle = new JLabel("Question-by-Question Diagnostic Review");
        sectionTitle.setFont(UITheme.FONT_TITLE);
        sectionTitle.setForeground(UITheme.PRIMARY);
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        reviewContainer.add(sectionTitle);
        reviewContainer.add(Box.createVerticalStrut(12));

        int qIdx = 1;
        for (Question q : questions) {
            Question.Option candidateChoice = answersMap.get(q.getId());
            boolean isCorrect = candidateChoice != null && q.isCorrect(candidateChoice);
            boolean isSkipped = candidateChoice == null;

            JPanel qCard = UITheme.createCard();
            qCard.setLayout(new BorderLayout(10, 10));
            qCard.setAlignmentX(Component.LEFT_ALIGNMENT);
            qCard.setMaximumSize(new Dimension(Short.MAX_VALUE, 260));

            // Question top: Number + Status
            JPanel qTop = new JPanel(new BorderLayout());
            qTop.setOpaque(false);

            JLabel qNum = new JLabel(String.format("Question %d of %d  (%d Marks)", qIdx++, questions.size(), q.getMarks()));
            qNum.setFont(UITheme.FONT_BOLD);
            qNum.setForeground(UITheme.PRIMARY);

            JLabel statusBadge;
            if (isCorrect) {
                statusBadge = UITheme.createBadge("✓ CORRECT", UITheme.SUCCESS_LIGHT, UITheme.SUCCESS_DARK);
            } else if (isSkipped) {
                statusBadge = UITheme.createBadge("SKIPPED", UITheme.SECONDARY_LIGHT, UITheme.SECONDARY);
            } else {
                statusBadge = UITheme.createBadge("✗ INCORRECT", UITheme.DANGER_LIGHT, UITheme.DANGER_DARK);
            }

            qTop.add(qNum, BorderLayout.WEST);
            qTop.add(statusBadge, BorderLayout.EAST);

            // Question Text
            JTextArea qTextArea = new JTextArea(q.getQuestionText());
            qTextArea.setFont(UITheme.FONT_QUESTION);
            qTextArea.setWrapStyleWord(true);
            qTextArea.setLineWrap(true);
            qTextArea.setOpaque(false);
            qTextArea.setEditable(false);
            qTextArea.setBorder(new EmptyBorder(4, 0, 8, 0));

            // Answers Breakdown
            JPanel answersPanel = new JPanel(new GridLayout(2, 1, 4, 4));
            answersPanel.setOpaque(false);

            String candidateText = isSkipped ? "(No option selected)" : candidateChoice + ": " + q.getOptionText(candidateChoice);
            JLabel candidateLbl = new JLabel("Your Answer: " + candidateText);
            candidateLbl.setFont(UITheme.FONT_BODY);
            candidateLbl.setForeground(isCorrect ? UITheme.SUCCESS_DARK : (isSkipped ? UITheme.SECONDARY : UITheme.DANGER_DARK));

            String correctText = q.getCorrectOption() + ": " + q.getOptionText(q.getCorrectOption());
            JLabel correctLbl = new JLabel("Correct Answer: " + correctText);
            correctLbl.setFont(UITheme.FONT_BOLD);
            correctLbl.setForeground(UITheme.SUCCESS_DARK);

            answersPanel.add(candidateLbl);
            answersPanel.add(correctLbl);

            // Explanation box if present
            JPanel bodyBox = new JPanel();
            bodyBox.setLayout(new BoxLayout(bodyBox, BoxLayout.Y_AXIS));
            bodyBox.setOpaque(false);
            bodyBox.add(qTextArea);
            bodyBox.add(answersPanel);

            if (q.getExplanation() != null && !q.getExplanation().isBlank()) {
                JPanel explPanel = new JPanel(new BorderLayout());
                explPanel.setBackground(new Color(248, 250, 252));
                explPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1, true),
                        new EmptyBorder(6, 10, 6, 10)
                ));
                JLabel explTitle = new JLabel("Explanation: " + q.getExplanation());
                explTitle.setFont(UITheme.FONT_SMALL);
                explTitle.setForeground(UITheme.TEXT_MUTED);
                explPanel.add(explTitle, BorderLayout.CENTER);
                bodyBox.add(Box.createVerticalStrut(8));
                bodyBox.add(explPanel);
            }

            qCard.add(qTop, BorderLayout.NORTH);
            qCard.add(bodyBox, BorderLayout.CENTER);

            reviewContainer.add(qCard);
            reviewContainer.add(Box.createVerticalStrut(12));
        }

        JScrollPane scrollPane = new JScrollPane(reviewContainer);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        // Center container combining metrics + review
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(summaryGrid, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // Footer Action
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR));

        JButton closeBtn = UITheme.createButton("Return to Dashboard", UITheme.ACCENT, Color.WHITE, 13, true);
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);

        add(headerPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    private JPanel createMetricCard(String label, String value, String subtext, Color valueColor) {
        JPanel card = UITheme.createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel titleLbl = new JLabel(label);
        titleLbl.setFont(UITheme.FONT_SMALL);
        titleLbl.setForeground(UITheme.TEXT_MUTED);

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(UITheme.FONT_TITLE_LARGE);
        valLbl.setForeground(valueColor);

        JLabel subLbl = new JLabel(subtext);
        subLbl.setFont(UITheme.FONT_SMALL);
        subLbl.setForeground(UITheme.TEXT_MUTED);

        card.add(titleLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(valLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(subLbl);
        return card;
    }
}
