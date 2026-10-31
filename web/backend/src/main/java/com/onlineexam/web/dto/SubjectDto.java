package com.onlineexam.web.dto;

import java.util.List;

public class SubjectDto {

    public record SubjectResponse(
            Long id,
            String name,
            String description,
            boolean isPublished,
            int topicCount,
            int examCount
    ) {}

    public record CreateSubjectRequest(
            String name,
            String description,
            Boolean isPublished,
            String code
    ) {
        public CreateSubjectRequest(String name, String description, Boolean isPublished) {
            this(name, description, isPublished, null);
        }

        public boolean getEffectiveIsPublished() {
            return isPublished == null || isPublished;
        }
    }

    public record TopicResponse(
            Long id,
            Long subjectId,
            String subjectName,
            String name,
            String description,
            int displayOrder,
            boolean isPublished,
            double studentProgressPercentage,
            int materialCount,
            int questionCount
    ) {}

    public record CreateTopicRequest(
            Long subjectId,
            String name,
            String description,
            Integer displayOrder,
            Integer orderIndex,
            Boolean isPublished
    ) {
        public CreateTopicRequest(Long subjectId, String name, String description,
                                  Integer displayOrder, Boolean isPublished) {
            this(subjectId, name, description, displayOrder, null, isPublished);
        }

        public int getEffectiveDisplayOrder() {
            if (displayOrder != null && displayOrder > 0) return displayOrder;
            if (orderIndex != null && orderIndex > 0) return orderIndex;
            return 1;
        }

        public boolean getEffectiveIsPublished() {
            return isPublished == null || isPublished;
        }
    }
}
