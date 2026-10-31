package com.onlineexam.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StudyMaterial} entity model.
 */
public class StudyMaterialModelTest {

    @Test
    @DisplayName("StudyMaterial creation and validation with valid properties")
    void testValidStudyMaterial() {
        StudyMaterial m = new StudyMaterial(10L, "Java Concurrency Guide", "# Notes\ncode", "https://example.com", "storage/notes.pdf", "PDF", 1, true);
        assertDoesNotThrow(m::validate);
        assertEquals(10L, m.getTopicId());
        assertEquals("Java Concurrency Guide", m.getTitle());
        assertEquals("PDF", m.getAttachmentType());
        assertTrue(m.isPublished());
    }

    @Test
    @DisplayName("StudyMaterial validation throws on empty title or invalid topic ID")
    void testInvalidStudyMaterial() {
        StudyMaterial noTopic = new StudyMaterial(null, "Title", "Content", null, null, null, 1, true);
        assertThrows(IllegalArgumentException.class, noTopic::validate);

        StudyMaterial emptyTitle = new StudyMaterial(10L, "", "Content", null, null, null, 1, true);
        assertThrows(IllegalArgumentException.class, emptyTitle::validate);

        StudyMaterial negativeOrder = new StudyMaterial(10L, "Title", "Content", null, null, null, -1, true);
        assertThrows(IllegalArgumentException.class, negativeOrder::validate);
    }
}
