package com.onlineexam.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing a specific learning Topic within a Subject.
 */
public class Topic {

    private Long id;
    private Long subjectId;
    private String name;
    private String description;
    private int displayOrder = 1;
    private boolean published = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Topic() {}

    public Topic(Long subjectId, String name, String description, int displayOrder, boolean published) {
        this.subjectId = subjectId;
        setName(name);
        this.description = description;
        this.displayOrder = displayOrder;
        this.published = published;
    }

    public Topic(Long id, Long subjectId, String name, String description, int displayOrder,
                 boolean published, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.subjectId = subjectId;
        setName(name);
        this.description = description;
        this.displayOrder = displayOrder;
        this.published = published;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void validate() {
        if (subjectId == null || subjectId <= 0) {
            throw new IllegalArgumentException("Topic must be associated with a valid subject.");
        }
        if (name == null || name.trim().isBlank()) {
            throw new IllegalArgumentException("Topic name cannot be empty.");
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

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = (name != null) ? name.trim() : null;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
        if (!(o instanceof Topic other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name != null ? name : "Topic#" + id;
    }
}
