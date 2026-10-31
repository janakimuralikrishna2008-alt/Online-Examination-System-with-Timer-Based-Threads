package com.onlineexam.service;

import com.onlineexam.config.BCrypt;
import com.onlineexam.dao.UserDAO;
import com.onlineexam.model.User;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Business service managing user authentication, registration, and user administration.
 * Integrates {@link UserDAO} and {@link BCrypt} secure password hashing with fallback
 * memory registry support for offline / demo mode.
 */
public class AuthService {

    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());

    private final UserDAO userDAO;

    // In-memory fallback registry for offline demonstration or testing
    private static final Map<String, User> MEMORY_USERS = new ConcurrentHashMap<>();
    private static final Map<String, String> MEMORY_PASSWORDS = new ConcurrentHashMap<>();
    private static long nextMemoryId = 1000L;

    static {
        initDefaultMemoryUsers();
    }

    private static void initDefaultMemoryUsers() {
        // Standard default accounts:
        // admin / admin123
        // student1 / student123
        // student2 / student123
        registerMemoryUser(1L, "admin", "admin123", "System Administrator", "admin@onlineexam.com", User.Role.ADMIN);
        registerMemoryUser(2L, "student1", "student123", "Alice Johnson", "alice.student@onlineexam.com", User.Role.STUDENT);
        registerMemoryUser(3L, "student2", "student123", "Bob Smith", "bob.student@onlineexam.com", User.Role.STUDENT);
    }

    private static void registerMemoryUser(Long id, String username, String rawPassword, String fullName, String email, User.Role role) {
        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(10));
        User u = new User(id, username, hash, fullName, email, role, LocalDateTime.now(), LocalDateTime.now());
        MEMORY_USERS.put(username.toLowerCase(), u);
        MEMORY_PASSWORDS.put(username.toLowerCase(), hash);
    }

    public AuthService() {
        this(new UserDAO());
    }

    public AuthService(UserDAO userDAO) {
        if (userDAO == null) {
            throw new IllegalArgumentException("UserDAO cannot be null.");
        }
        this.userDAO = userDAO;
    }

    private volatile boolean lastAuthUsedDatabase = false;
    private volatile String lastDatabaseError = null;

    public boolean isLastAuthUsedDatabase() {
        return lastAuthUsedDatabase;
    }

    public String getLastDatabaseError() {
        return lastDatabaseError;
    }

    /**
     * Authenticates a user by username or email and plain text password.
     *
     * @param identifier username or email address
     * @param password raw plain text password
     * @return Optional containing authenticated User if valid, empty otherwise
     */
    public Optional<User> authenticate(String identifier, String password) {
        if (identifier == null || identifier.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }

        String trimmed = identifier.trim();
        lastAuthUsedDatabase = false;
        lastDatabaseError = null;

        // 1. Attempt lookup in MySQL database
        try {
            Optional<User> userOpt = userDAO.findByUsername(trimmed);
            if (userOpt.isEmpty() && trimmed.contains("@")) {
                userOpt = userDAO.findByEmail(trimmed);
            }

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (BCrypt.checkpw(password, user.getPasswordHash())) {
                    lastAuthUsedDatabase = true;
                    LOGGER.info("Authenticated user from database: " + user.getUsername());
                    return Optional.of(user);
                } else {
                    LOGGER.warning("Invalid password for database user: " + user.getUsername());
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            lastDatabaseError = e.getMessage();
            LOGGER.log(Level.WARNING, "Database authentication failed (SQLException): " + e.getMessage() + ". Checking offline fallback...");
        } catch (Exception e) {
            lastDatabaseError = e.getMessage();
            LOGGER.log(Level.WARNING, "Database authentication failed: " + e.getMessage() + ". Checking offline fallback...");
        }

        // 2. Fallback to memory registry (offline mode or demo accounts)
        User memUser = MEMORY_USERS.get(trimmed.toLowerCase());
        if (memUser == null && trimmed.contains("@")) {
            for (User u : MEMORY_USERS.values()) {
                if (trimmed.equalsIgnoreCase(u.getEmail())) {
                    memUser = u;
                    break;
                }
            }
        }

        if (memUser != null) {
            String storedHash = MEMORY_PASSWORDS.get(memUser.getUsername().toLowerCase());
            if (storedHash != null && BCrypt.checkpw(password, storedHash)) {
                LOGGER.info("Authenticated demo user from memory registry: " + memUser.getUsername());
                return Optional.of(memUser);
            }
        }

        return Optional.empty();
    }

    /**
     * Public candidate registration strictly constrained to STUDENT role.
     * Prevents public privilege escalation to administrator roles.
     */
    public User registerStudent(String username, String rawPassword, String fullName, String email) throws SQLException {
        return register(username, rawPassword, fullName, email, User.Role.STUDENT);
    }

    /**
     * Administrator registration assigning ADMIN role.
     * Invoked after server-side invitation code validation.
     */
    public User registerAdmin(String username, String rawPassword, String fullName, String email) throws SQLException {
        return register(username, rawPassword, fullName, email, User.Role.ADMIN);
    }

    /**
     * Registers a new student or admin account.
     *
     * @param username chosen username (3-50 chars)
     * @param rawPassword plain text password (minimum 6 chars)
     * @param fullName full display name
     * @param email valid email address
     * @param role user role (STUDENT or ADMIN)
     * @return created User entity
     * @throws IllegalArgumentException on invalid input or username/email collision
     */
    public User register(String username, String rawPassword, String fullName, String email, User.Role role) throws SQLException {
        if (username == null || username.trim().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters.");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
        if (fullName == null || fullName.trim().isBlank()) {
            throw new IllegalArgumentException("Full name cannot be blank.");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Valid email is required.");
        }
        if (role == null) {
            role = User.Role.STUDENT;
        }

        String trimmedUser = username.trim();
        String trimmedEmail = email.trim().toLowerCase();

        // Check duplicate username and email in memory registry
        if (MEMORY_USERS.containsKey(trimmedUser.toLowerCase())) {
            throw new IllegalArgumentException("Username '" + trimmedUser + "' is already taken.");
        }
        for (User u : MEMORY_USERS.values()) {
            if (trimmedEmail.equalsIgnoreCase(u.getEmail())) {
                throw new IllegalArgumentException("Email address '" + trimmedEmail + "' is already registered.");
            }
        }

        String passwordHash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(10));
        User newUser = new User(trimmedUser, passwordHash, fullName.trim(), trimmedEmail, role);

        // Try persisting to database first
        try {
            Optional<User> existingUser = userDAO.findByUsername(trimmedUser);
            if (existingUser.isPresent()) {
                throw new IllegalArgumentException("Username '" + trimmedUser + "' already exists in database.");
            }
            Optional<User> existingEmail = userDAO.findByEmail(trimmedEmail);
            if (existingEmail.isPresent()) {
                throw new IllegalArgumentException("Email address '" + trimmedEmail + "' is already registered in database.");
            }
            User created = userDAO.create(newUser);
            // Also mirror to memory registry
            MEMORY_USERS.put(trimmedUser.toLowerCase(), created);
            MEMORY_PASSWORDS.put(trimmedUser.toLowerCase(), passwordHash);
            return created;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Database unavailable during registration, saving to memory registry only", e);
            // If DB is offline, register in memory store
            synchronized (MEMORY_USERS) {
                newUser.setId(++nextMemoryId);
                newUser.setCreatedAt(LocalDateTime.now());
                newUser.setUpdatedAt(LocalDateTime.now());
                MEMORY_USERS.put(trimmedUser.toLowerCase(), newUser);
                MEMORY_PASSWORDS.put(trimmedUser.toLowerCase(), passwordHash);
                return newUser;
            }
        }
    }

    /**
     * Retrieves all registered users (database if available, else in-memory catalog).
     */
    public List<User> getAllUsers() {
        try {
            List<User> dbUsers = userDAO.findAll();
            if (!dbUsers.isEmpty()) {
                return dbUsers;
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to fetch users from database", e);
        }
        return new ArrayList<>(MEMORY_USERS.values());
    }

    /**
     * Finds a user by ID.
     */
    public Optional<User> getUserById(Long id) {
        if (id == null) return Optional.empty();
        try {
            Optional<User> dbUser = userDAO.findById(id);
            if (dbUser.isPresent()) return dbUser;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Database user lookup failed", e);
        }
        return MEMORY_USERS.values().stream().filter(u -> id.equals(u.getId())).findFirst();
    }

    /**
     * Deletes a user by ID.
     */
    public boolean deleteUser(Long id) {
        if (id == null) return false;
        boolean deletedDb = false;
        try {
            deletedDb = userDAO.delete(id);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Database delete failed", e);
        }

        boolean deletedMem = MEMORY_USERS.values().removeIf(u -> id.equals(u.getId()));
        return deletedDb || deletedMem;
    }
}
