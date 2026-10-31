package com.onlineexam.web.dto;

public class StudyMaterialDto {

    public record MaterialResponse(
            Long id,
            Long topicId,
            String topicName,
            String title,
            String content,
            String resourceUrl,
            String attachmentPath,
            String attachmentType,
            int displayOrder,
            boolean isPublished,
            boolean isCompleted
    ) {}

    public record CreateMaterialRequest(
            Long topicId,
            String title,
            String content,
            String resourceUrl,
            String attachmentPath,
            String attachmentType,
            String contentType,
            Integer displayOrder,
            Integer orderIndex,
            Boolean isPublished
    ) {
        public CreateMaterialRequest(Long topicId, String title, String content,
                                     String resourceUrl, String attachmentPath, String attachmentType,
                                     Integer displayOrder, Boolean isPublished) {
            this(topicId, title, content, resourceUrl, attachmentPath, attachmentType, null, displayOrder, null, isPublished);
        }

        public int getEffectiveDisplayOrder() {
            if (displayOrder != null && displayOrder > 0) return displayOrder;
            if (orderIndex != null && orderIndex > 0) return orderIndex;
            return 1;
        }

        public String getEffectiveAttachmentType() {
            if (attachmentType != null && !attachmentType.isBlank()) return attachmentType;
            if (contentType != null && !contentType.isBlank()) return contentType;
            return "NOTES";
        }

        public boolean getEffectiveIsPublished() {
            return isPublished == null || isPublished;
        }
    }

    public record ProgressResponse(
            Long topicId,
            double progressPercentage,
            int completedCount,
            int totalCount
    ) {}
}
