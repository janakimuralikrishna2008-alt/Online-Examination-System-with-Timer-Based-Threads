package com.onlineexam.web.dto;

import java.util.List;

public class ExamDto {

    public record ExamResponse(
            Long id,
            String title,
            String description,
            int durationMinutes,
            int totalMarks,
            int passingMarks,
            boolean isActive,
            Long subjectId,
            String subjectName,
            Long topicId,
            String topicName,
            String assessmentType,
            int questionCount,
            String studentAttemptStatus
    ) {}

    public record CreateExamRequest(
            String title,
            String description,
            Integer durationMinutes,
            Integer totalMarks,
            Integer passingMarks,
            Boolean isActive,
            Long subjectId,
            Long topicId,
            String assessmentType,
            String examType
    ) {
        public CreateExamRequest(String title, String description, Integer durationMinutes,
                                 Integer totalMarks, Integer passingMarks, Boolean isActive,
                                 Long subjectId, Long topicId, String assessmentType) {
            this(title, description, durationMinutes, totalMarks, passingMarks, isActive, subjectId, topicId, assessmentType, null);
        }

        public boolean getEffectiveIsActive() {
            return isActive == null || isActive;
        }

        public int getEffectiveDurationMinutes() {
            return durationMinutes != null && durationMinutes > 0 ? durationMinutes : 15;
        }

        public int getEffectiveTotalMarks() {
            return totalMarks != null && totalMarks > 0 ? totalMarks : 100;
        }

        public int getEffectivePassingMarks() {
            return passingMarks != null && passingMarks >= 0 ? passingMarks : 40;
        }

        public String getEffectiveAssessmentType() {
            if (assessmentType != null && !assessmentType.isBlank()) {
                return assessmentType;
            }
            if (examType != null && !examType.isBlank()) {
                return examType.equalsIgnoreCase("PRACTICE") || examType.equalsIgnoreCase("PRACTICE_QUIZ")
                        ? "PRACTICE_QUIZ" : "FORMAL_EXAM";
            }
            return "FORMAL_EXAM";
        }
    }

    public record QuestionResponse(
            Long id,
            Long examId,
            Long topicId,
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            String correctOption,
            int marks,
            String explanation,
            boolean isPublished
    ) {}

    public record CreateQuestionRequest(
            Long examId,
            Long topicId,
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            String correctOption,
            Integer marks,
            String explanation,
            Boolean isPublished
    ) {
        public boolean getEffectiveIsPublished() {
            return isPublished == null || isPublished;
        }

        public int getEffectiveMarks() {
            return marks != null && marks > 0 ? marks : 1;
        }
    }
}
