package com.onlineexam.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Immutable report representing the evaluated outcome of an examination attempt.
 */
public record ExamResult(
        Long attemptId,
        Long studentId,
        Long examId,
        String examTitle,
        int totalQuestions,
        int correctCount,
        int incorrectCount,
        int unansweredCount,
        int totalPossibleMarks,
        int earnedMarks,
        int passingMarks,
        double percentage,
        boolean passed,
        boolean autoSubmitted,
        LocalDateTime submittedAt,
        List<StudentAnswer> evaluatedAnswers
) {
    public ExamResult {
        if (evaluatedAnswers == null) {
            evaluatedAnswers = Collections.emptyList();
        } else {
            evaluatedAnswers = Collections.unmodifiableList(evaluatedAnswers);
        }
    }

    /**
     * Helper to format percentage to 2 decimal places.
     */
    public String getFormattedPercentage() {
        return String.format("%.2f%%", percentage);
    }
}
