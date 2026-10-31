package com.onlineexam.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity tracking a student's completion of a study material item.
 */
public class StudentMaterialProgress {

    private Long id;
    private Long studentId;
    private Long materialId;
    private boolean completed = true;
    private LocalDateTime completedAt;

    public StudentMaterialProgress() {}

    public StudentMaterialProgress(Long studentId, Long materialId, boolean completed) {
        this.studentId = studentId;
        this.materialId = materialId;
        this.completed = completed;
        this.completedAt = LocalDateTime.now();
    }

    public StudentMaterialProgress(Long id, Long studentId, Long materialId, boolean completed, LocalDateTime completedAt) {
        this.id = id;
        this.studentId = studentId;
        this.materialId = materialId;
        this.completed = completed;
        this.completedAt = completedAt;
    }

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

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentMaterialProgress other)) return false;
        return Objects.equals(studentId, other.studentId) && Objects.equals(materialId, other.materialId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId, materialId);
    }
}
