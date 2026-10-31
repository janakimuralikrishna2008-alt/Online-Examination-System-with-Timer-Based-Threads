package com.onlineexam.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing an individual response submitted by a student for an exam question.
 */
public class StudentAnswer {

    private Long id;
    private Long studentExamId;
    private Long questionId;
    private Question.Option selectedOption;
    private boolean correct = false;
    private LocalDateTime answeredAt;

    public StudentAnswer() {
    }

    public StudentAnswer(Long studentExamId, Long questionId, Question.Option selectedOption, boolean correct) {
        this.studentExamId = studentExamId;
        this.questionId = questionId;
        this.selectedOption = selectedOption;
        this.correct = correct;
    }

    public StudentAnswer(Long id, Long studentExamId, Long questionId, Question.Option selectedOption,
                         boolean correct, LocalDateTime answeredAt) {
        this.id = id;
        this.studentExamId = studentExamId;
        this.questionId = questionId;
        this.selectedOption = selectedOption;
        this.correct = correct;
        this.answeredAt = answeredAt;
    }

    public void validate() {
        if (studentExamId == null || studentExamId <= 0) {
            throw new IllegalArgumentException("Student exam session ID must be valid and positive.");
        }
        if (questionId == null || questionId <= 0) {
            throw new IllegalArgumentException("Question ID must be valid and positive.");
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentExamId() {
        return studentExamId;
    }

    public void setStudentExamId(Long studentExamId) {
        this.studentExamId = studentExamId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Question.Option getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(Question.Option selectedOption) {
        this.selectedOption = selectedOption;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(LocalDateTime answeredAt) {
        this.answeredAt = answeredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentAnswer other)) return false;
        return Objects.equals(studentExamId, other.studentExamId) &&
               Objects.equals(questionId, other.questionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentExamId, questionId);
    }

    @Override
    public String toString() {
        return "StudentAnswer{" +
                "id=" + id +
                ", studentExamId=" + studentExamId +
                ", questionId=" + questionId +
                ", selectedOption=" + selectedOption +
                ", correct=" + correct +
                '}';
    }
}
