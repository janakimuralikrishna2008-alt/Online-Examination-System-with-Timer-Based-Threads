package com.onlineexam.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing educational Study Material (notes, formatted text, code examples, resource links, and attachments).
 */
public class StudyMaterial {

    private Long id;
    private Long topicId;
    private String title;
    private String content;
    private String resourceUrl;
    private String attachmentPath;
    private String attachmentType;
    private int displayOrder = 1;
    private boolean published = true;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public StudyMaterial() {}

    public StudyMaterial(Long topicId, String title, String content, int displayOrder, boolean published) {
        this(topicId, title, content, null, null, null, displayOrder, published);
    }

    public StudyMaterial(Long topicId, String title, String content, String resourceUrl,
                         String attachmentPath, String attachmentType, int displayOrder, boolean published) {
        this.topicId = topicId;
        setTitle(title);
        this.content = content;
        this.resourceUrl = resourceUrl;
        this.attachmentPath = attachmentPath;
        this.attachmentType = attachmentType;
        this.displayOrder = displayOrder;
        this.published = published;
    }


    public StudyMaterial(Long id, Long topicId, String title, String content, String resourceUrl,
                         String attachmentPath, String attachmentType, int displayOrder, boolean published,
                         Long createdBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.topicId = topicId;
        setTitle(title);
        this.content = content;
        this.resourceUrl = resourceUrl;
        this.attachmentPath = attachmentPath;
        this.attachmentType = attachmentType;
        this.displayOrder = displayOrder;
        this.published = published;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void validate() {
        if (topicId == null || topicId <= 0) {
            throw new IllegalArgumentException("Study material must be associated with a valid topic.");
        }
        if (title == null || title.trim().isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty.");
        }
        if (displayOrder < 0) {
            throw new IllegalArgumentException("Display order must be non-negative.");
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = (title != null) ? title.trim() : null;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public void setResourceUrl(String resourceUrl) {
        this.resourceUrl = resourceUrl;
    }

    public String getAttachmentPath() {
        return attachmentPath;
    }

    public void setAttachmentPath(String attachmentPath) {
        this.attachmentPath = attachmentPath;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
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
        if (!(o instanceof StudyMaterial other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return title != null ? title : "StudyMaterial#" + id;
    }
}
