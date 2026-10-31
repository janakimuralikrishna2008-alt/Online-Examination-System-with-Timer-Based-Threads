package com.onlineexam.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link User} entity and validation rules.
 */
public class UserModelTest {

    @Test
    @DisplayName("Valid user passes validation without exceptions")
    void testValidUser() {
        User user = new User("johndoe", "hashed_secret_123", "John Doe", "john.doe@example.com", User.Role.STUDENT);
        assertDoesNotThrow(user::validate);
        assertEquals("johndoe", user.getUsername());
        assertEquals("john.doe@example.com", user.getEmail());
        assertTrue(user.isStudent());
        assertFalse(user.isAdmin());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "a", "ab"})
    @DisplayName("Short username throws IllegalArgumentException")
    void testShortUsername(String shortUsername) {
        User user = new User(shortUsername, "hashed_pw", "Name", "email@test.com", User.Role.STUDENT);
        Exception ex = assertThrows(IllegalArgumentException.class, user::validate);
        assertTrue(ex.getMessage().contains("Username must be between 3 and 50"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-email", "user@", "@domain.com", "user@domain"})
    @DisplayName("Invalid email formats throw IllegalArgumentException")
    void testInvalidEmail(String invalidEmail) {
        User user = new User("validuser", "hashed_pw", "Valid Name", invalidEmail, User.Role.STUDENT);
        Exception ex = assertThrows(IllegalArgumentException.class, user::validate);
        assertTrue(ex.getMessage().contains("Invalid email format"));
    }

    @Test
    @DisplayName("Missing password hash throws IllegalArgumentException")
    void testMissingPasswordHash() {
        User user = new User("validuser", "   ", "Valid Name", "valid@test.com", User.Role.STUDENT);
        Exception ex = assertThrows(IllegalArgumentException.class, user::validate);
        assertTrue(ex.getMessage().contains("Password hash cannot be empty"));
    }

    @Test
    @DisplayName("Security: toString must not expose password hash")
    void testToStringSecurity() {
        User user = new User("alice", "SUPER_SECRET_HASH", "Alice Smith", "alice@example.com", User.Role.STUDENT);
        String stringRepresentation = user.toString();
        assertFalse(stringRepresentation.contains("SUPER_SECRET_HASH"), "Password hash should be protected in toString");
        assertTrue(stringRepresentation.contains("[PROTECTED]"));
    }

    @Test
    @DisplayName("Role.fromString handles case insensitivity and fallbacks")
    void testRoleParsing() {
        assertEquals(User.Role.ADMIN, User.Role.fromString("admin"));
        assertEquals(User.Role.ADMIN, User.Role.fromString("ADMIN"));
        assertEquals(User.Role.STUDENT, User.Role.fromString("student"));
        assertEquals(User.Role.STUDENT, User.Role.fromString("UNKNOWN_ROLE"));
        assertEquals(User.Role.STUDENT, User.Role.fromString(null));
    }
}
