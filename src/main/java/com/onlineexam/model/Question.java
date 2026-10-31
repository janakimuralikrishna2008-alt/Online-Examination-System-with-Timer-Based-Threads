package com.onlineexam.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing an Examination Multiple-Choice Question.
 */
public class Question {

    public enum Option {
        A, B, C, D;

        public static Option fromString(String value) {
            if (value == null) {
                return null;
            }
            try {
                return Option.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private Long id;
    private Long examId;
    private Long topicId;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private Option correctOption;
    private int marks = 1;
    private String explanation;
    private boolean published = true;
    private LocalDateTime createdAt;

    public Question() {
    }

    public Question(Long examId, String questionText, String optionA, String optionB, String optionC, String optionD,
                    Option correctOption, int marks, String explanation) {
        this.examId = examId;
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctOption = correctOption;
        this.marks = marks;
        this.explanation = explanation;
    }

    public Question(Long id, Long examId, String questionText, String optionA, String optionB, String optionC,
                    String optionD, Option correctOption, int marks, String explanation, LocalDateTime createdAt) {
        this.id = id;
        this.examId = examId;
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctOption = correctOption;
        this.marks = marks;
        this.explanation = explanation;
        this.createdAt = createdAt;
    }

    public Question(Long id, Long examId, Long topicId, String questionText, String optionA, String optionB,
                    String optionC, String optionD, Option correctOption, int marks, String explanation,
                    boolean published, LocalDateTime createdAt) {
        this.id = id;
        this.examId = examId;
        this.topicId = topicId;
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctOption = correctOption;
        this.marks = marks;
        this.explanation = explanation;
        this.published = published;
        this.createdAt = createdAt;
    }

    /**
     * Validates question completeness and choice integrity.
     */
    public void validate() {
        if (examId == null || examId <= 0) {
            throw new IllegalArgumentException("Question must be associated with a valid exam ID.");
        }
        if (questionText == null || questionText.trim().isBlank()) {
            throw new IllegalArgumentException("Question text cannot be empty.");
        }
        if (optionA == null || optionA.trim().isBlank() ||
            optionB == null || optionB.trim().isBlank() ||
            optionC == null || optionC.trim().isBlank() ||
            optionD == null || optionD.trim().isBlank()) {
            throw new IllegalArgumentException("All four answer options (A, B, C, D) must be provided.");
        }
        if (correctOption == null) {
            throw new IllegalArgumentException("A correct answer option (A, B, C, or D) must be specified.");
        }
        if (marks <= 0) {
            throw new IllegalArgumentException("Question marks must be greater than 0.");
        }
    }

    public String getOptionText(Option option) {
        if (option == null) return null;
        return switch (option) {
            case A -> optionA;
            case B -> optionB;
            case C -> optionC;
            case D -> optionD;
        };
    }

    public boolean isCorrect(Option selected) {
        return selected != null && selected == correctOption;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = (questionText != null) ? questionText.trim() : null;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = (optionA != null) ? optionA.trim() : null;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = (optionB != null) ? optionB.trim() : null;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = (optionC != null) ? optionC.trim() : null;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = (optionD != null) ? optionD.trim() : null;
    }

    public Option getCorrectOption() {
        return correctOption;
    }

    public void setCorrectOption(Option correctOption) {
        this.correctOption = correctOption;
    }

    public int getMarks() {
        return marks;
    }

    public void setMarks(int marks) {
        this.marks = marks;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Question other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Question{" +
                "id=" + id +
                ", examId=" + examId +
                ", questionText='" + questionText + '\'' +
                ", correctOption=" + correctOption +
                ", marks=" + marks +
                '}';
    }
}
