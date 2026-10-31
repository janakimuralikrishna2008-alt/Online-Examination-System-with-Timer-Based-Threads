package com.onlineexam.web.dto;

import java.util.List;

public class AiDto {

    public record AiStudyRequest(
            Long topicId,
            String question
    ) {}

    public record AiStudyResponse(
            String answer,
            String topicName,
            List<String> sourceMaterials,
            boolean sourceBased,
            String providerStatus
    ) {}

    public record AiDraftQuestionRequest(
            Long topicId,
            String difficulty,
            String promptNotes
    ) {}

    public record AiDraftQuestionResponse(
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            String correctOption,
            String explanation,
            int marks,
            String providerStatus
    ) {}

    public record AiRecommendationDto(
            Long topicId,
            String topicName,
            String subjectName,
            String reason,
            String priority
    ) {}

    public record AiPerformanceInsightsResponse(
            int totalAttempts,
            double overallPassRate,
            double averageScorePercentage,
            List<TopicScoreMetric> topicMetrics,
            List<String> keyRecommendations,
            String providerStatus
    ) {}

    public record TopicScoreMetric(
            Long topicId,
            String topicName,
            double averageScore,
            int attemptCount,
            String status
    ) {}
}
