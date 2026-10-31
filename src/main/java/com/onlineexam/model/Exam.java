package com.onlineexam.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing an Examination.
 */
public class Exam {

    public enum AssessmentType {
        FORMAL_EXAM,
        PRACTICE_QUIZ;

        public static AssessmentType fromString(String value) {
            if (value == null) return FORMAL_EXAM;
            try {
                return AssessmentType.valueOf(value.trim().toUpperCase());
            } catch (Exception e) {
                return FORMAL_EXAM;
            }
        }
    }

    private Long id;
    private Long subjectId;
    private Long topicId;
    private String title;
    private String description;
    private int durationMinutes = 15;
    private int totalMarks = 100;
    private int passingMarks = 40;
    private AssessmentType assessmentType = AssessmentType.FORMAL_EXAM;
    private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Exam() {
    }

    public Exam(String title, String description, int durationMinutes, int totalMarks, int passingMarks, boolean active) {
        setTitle(title);
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.totalMarks = totalMarks;
        this.passingMarks = passingMarks;
        this.active = active;
    }

    public Exam(Long id, String title, String description, int durationMinutes, int totalMarks, int passingMarks,
                boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        setTitle(title);
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.totalMarks = totalMarks;
        this.passingMarks = passingMarks;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Exam(Long id, Long subjectId, Long topicId, String title, String description, int durationMinutes,
                int totalMarks, int passingMarks, AssessmentType assessmentType, boolean active,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.subjectId = subjectId;
        this.topicId = topicId;
        setTitle(title);
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.totalMarks = totalMarks;
        this.passingMarks = passingMarks;
        this.assessmentType = (assessmentType != null) ? assessmentType : AssessmentType.FORMAL_EXAM;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Validates exam parameters.
     *
     * @throws IllegalArgumentException if fields violate constraints
     */
    public void validate() {
        if (title == null || title.trim().isBlank()) {
            throw new IllegalArgumentException("Exam title cannot be empty.");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration must be greater than 0 minutes.");
        }
        if (totalMarks <= 0) {
            throw new IllegalArgumentException("Total marks must be greater than 0.");
        }
        if (passingMarks < 0 || passingMarks > totalMarks) {
            throw new IllegalArgumentException("Passing marks must be between 0 and total marks (" + totalMarks + ").");
        }
    }

    public int getDurationSeconds() {
        return durationMinutes * 60;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public AssessmentType getAssessmentType() {
        return assessmentType;
    }

    public void setAssessmentType(AssessmentType assessmentType) {
        this.assessmentType = (assessmentType != null) ? assessmentType : AssessmentType.FORMAL_EXAM;
    }

    public boolean isPracticeQuiz() {
        return assessmentType == AssessmentType.PRACTICE_QUIZ;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = (title != null) ? title.trim() : null;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(int totalMarks) {
        this.totalMarks = totalMarks;
    }

    public int getPassingMarks() {
        return passingMarks;
    }

    public void setPassingMarks(int passingMarks) {
        this.passingMarks = passingMarks;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Exam other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Exam{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", durationMinutes=" + durationMinutes +
                ", totalMarks=" + totalMarks +
                ", passingMarks=" + passingMarks +
                ", active=" + active +
                '}';
    }
}
