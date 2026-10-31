package com.onlineexam.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AttemptDto {

    public record StartAttemptResponse(
            Long attemptId,
            Long examId,
            String examTitle,
            String examType,
            String subjectName,
            int durationMinutes,
            long remainingSeconds,
            LocalDateTime startedAt,
            LocalDateTime startTime,
            LocalDateTime deadline,
            LocalDateTime deadlineTime,
            List<QuestionTakingDto> questions
    ) {}

    public record QuestionTakingDto(
            Long id,
            Long questionId,
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            int marks,
            String selectedOption
    ) {}

    public record SaveAnswerRequest(
            Long questionId,
            String selectedOption
    ) {}

    public record SubmitAttemptRequest(
            boolean autoSubmitted
    ) {}

    public record ScorecardResponse(
            Long attemptId,
            Long examId,
            String examTitle,
            String subjectName,
            String examType,
            int score,
            int earnedMarks,
            int totalMarks,
            int totalPossibleMarks,
            double percentage,
            boolean isPassed,
            boolean passed,
            int passingMarks,
            boolean autoSubmitted,
            String status,
            LocalDateTime startedAt,
            LocalDateTime startTime,
            LocalDateTime completedAt,
            LocalDateTime endTime,
            List<QuestionReviewDto> questions,
            List<QuestionReviewDto> questionReviews
    ) {}

    public record QuestionReviewDto(
            Long questionId,
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            String selectedOption,
            String correctOption,
            boolean isCorrect,
            int marksEarned,
            int marksAwarded,
            int maxMarks,
            int possibleMarks,
            String explanation
    ) {}

    public record AttemptHistorySummaryDto(
            Long attemptId,
            Long id,
            Long examId,
            String examTitle,
            String subjectName,
            String assessmentType,
            String examType,
            int score,
            int totalScore,
            int totalMarks,
            double percentage,
            boolean isPassed,
            boolean passed,
            String status,
            LocalDateTime startedAt,
            LocalDateTime startTime,
            LocalDateTime completedAt,
            LocalDateTime endTime
    ) {}
}
