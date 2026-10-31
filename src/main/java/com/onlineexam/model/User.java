package com.onlineexam.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Entity representing a system user (Student or Administrator).
 */
public class User {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public enum Role {
        STUDENT,
        ADMIN;

        public static Role fromString(String value) {
            if (value == null) {
                return STUDENT;
            }
            try {
                return Role.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return STUDENT;
            }
        }
    }

    private Long id;
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private Role role = Role.STUDENT;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User() {
    }

    public User(String username, String passwordHash, String fullName, String email, Role role) {
        setUsername(username);
        this.passwordHash = passwordHash;
        setFullName(fullName);
        setEmail(email);
        setRole(role);
    }

    public User(Long id, String username, String passwordHash, String fullName, String email, Role role,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        setUsername(username);
        this.passwordHash = passwordHash;
        setFullName(fullName);
        setEmail(email);
        setRole(role);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Validates user entity data integrity.
     *
     * @throws IllegalArgumentException if any field is invalid
     */
    public void validate() {
        if (username == null || username.trim().length() < 3 || username.trim().length() > 50) {
            throw new IllegalArgumentException("Username must be between 3 and 50 characters.");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be empty.");
        }
        if (fullName == null || fullName.trim().isBlank()) {
            throw new IllegalArgumentException("Full name cannot be empty.");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        if (role == null) {
            throw new IllegalArgumentException("User role cannot be null.");
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (username != null) {
            this.username = username.trim();
        } else {
            this.username = null;
        }
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        if (fullName != null) {
            this.fullName = fullName.trim();
        } else {
            this.fullName = null;
        }
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email != null) {
            this.email = email.trim().toLowerCase();
        } else {
            this.email = null;
        }
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = (role != null) ? role : Role.STUDENT;
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

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isStudent() {
        return role == Role.STUDENT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return Objects.equals(id, other.id) ||
                (username != null && username.equalsIgnoreCase(other.username));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username != null ? username.toLowerCase() : null);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", passwordHash='[PROTECTED]'" +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", createdAt=" + createdAt +
                '}';
    }
}
