package com.onlineexam.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing an active or completed Examination Session for a Student.
 * Integrates with the countdown timer thread via {@code timeRemainingSeconds}.
 */
public class StudentExam {

    public enum ExamStatus {
        NOT_STARTED,
        IN_PROGRESS,
        SUBMITTED,
        AUTO_SUBMITTED;

        public static ExamStatus fromString(String value) {
            if (value == null) {
                return NOT_STARTED;
            }
            try {
                return ExamStatus.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return NOT_STARTED;
            }
        }
    }

    private Long id;
    private Long studentId;
    private Long examId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int timeRemainingSeconds;
    private int totalScore = 0;
    private ExamStatus status = ExamStatus.NOT_STARTED;
    private LocalDateTime submittedAt;

    public StudentExam() {
    }

    public StudentExam(Long studentId, Long examId, int timeRemainingSeconds) {
        this.studentId = studentId;
        this.examId = examId;
        this.timeRemainingSeconds = timeRemainingSeconds;
        this.status = ExamStatus.NOT_STARTED;
    }

    public StudentExam(Long id, Long studentId, Long examId, LocalDateTime startTime, LocalDateTime endTime,
                       int timeRemainingSeconds, int totalScore, ExamStatus status, LocalDateTime submittedAt) {
        this.id = id;
        this.studentId = studentId;
        this.examId = examId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timeRemainingSeconds = timeRemainingSeconds;
        this.totalScore = totalScore;
        this.status = (status != null) ? status : ExamStatus.NOT_STARTED;
        this.submittedAt = submittedAt;
    }

    public void validate() {
        if (studentId == null || studentId <= 0) {
            throw new IllegalArgumentException("Student ID must be valid and positive.");
        }
        if (examId == null || examId <= 0) {
            throw new IllegalArgumentException("Exam ID must be valid and positive.");
        }
        if (timeRemainingSeconds < 0) {
            throw new IllegalArgumentException("Time remaining cannot be negative.");
        }
    }

    public boolean isCompleted() {
        return status == ExamStatus.SUBMITTED || status == ExamStatus.AUTO_SUBMITTED;
    }

    public boolean isInProgress() {
        return status == ExamStatus.IN_PROGRESS;
    }

    public void decrementTime(int seconds) {
        this.timeRemainingSeconds = Math.max(0, this.timeRemainingSeconds - seconds);
        if (this.timeRemainingSeconds == 0 && isInProgress()) {
            this.status = ExamStatus.AUTO_SUBMITTED;
        }
    }

    /**
     * Formats remaining time as MM:SS or HH:MM:SS string.
     */
    public String getFormattedTimeRemaining() {
        int hours = timeRemainingSeconds / 3600;
        int minutes = (timeRemainingSeconds % 3600) / 60;
        int seconds = timeRemainingSeconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public int getTimeRemainingSeconds() {
        return timeRemainingSeconds;
    }

    public void setTimeRemainingSeconds(int timeRemainingSeconds) {
        this.timeRemainingSeconds = Math.max(0, timeRemainingSeconds);
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public ExamStatus getStatus() {
        return status;
    }

    public void setStatus(ExamStatus status) {
        this.status = (status != null) ? status : ExamStatus.NOT_STARTED;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentExam other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "StudentExam{" +
                "id=" + id +
                ", studentId=" + studentId +
                ", examId=" + examId +
                ", timeRemainingSeconds=" + timeRemainingSeconds +
                ", totalScore=" + totalScore +
                ", status=" + status +
                '}';
    }
}
