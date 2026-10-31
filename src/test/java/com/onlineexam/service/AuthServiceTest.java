package com.onlineexam.service;

import com.onlineexam.dao.UserDAO;
import com.onlineexam.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite verifying authentication, BCrypt verification,
 * user registration, validation, and role assignment in {@link AuthService}.
 */
public class AuthServiceTest {

    private AuthService authService;
    private StubUserDAO stubUserDAO;

    @BeforeEach
    void setUp() {
        stubUserDAO = new StubUserDAO();
        authService = new AuthService(stubUserDAO);
    }

    @Test
    @DisplayName("Pre-seeded demo credentials authenticate successfully")
    void testPreseededDemoCredentials() {
        Optional<User> adminOpt = authService.authenticate("admin", "admin123");
        assertTrue(adminOpt.isPresent(), "Admin demo account should authenticate");
        assertEquals(User.Role.ADMIN, adminOpt.get().getRole());

        Optional<User> studentOpt = authService.authenticate("student1", "student123");
        assertTrue(studentOpt.isPresent(), "Student demo account should authenticate");
        assertEquals(User.Role.STUDENT, studentOpt.get().getRole());
    }

    @Test
    @DisplayName("Sample data seed BCrypt hashes accurately verify credentials")
    void testSampleDataSeedHashes() {
        assertTrue(com.onlineexam.config.BCrypt.checkpw("admin123", "$2a$10$kiVLO6wlUvcWkZNyaMLk1O5147RAZt2eVWI6klxVEkSNDBQX8Grfe"),
                "admin123 hash in sample_data.sql must verify successfully");
        assertTrue(com.onlineexam.config.BCrypt.checkpw("student123", "$2a$10$Negaj2NhFsVeYJBHpa80R.tGuN1uII/cV4.8Ut5BNYos4y38SU5.a"),
                "student123 hash in sample_data.sql must verify successfully");
    }

    @Test
    @DisplayName("Public registerStudent strictly enforces STUDENT role")
    void testRegisterStudentRoleEnforcement() throws SQLException {
        String user = "pub_student_" + System.currentTimeMillis();
        User registered = authService.registerStudent(user, "Password123!", "Public Candidate", user + "@test.org");
        assertNotNull(registered);
        assertEquals(User.Role.STUDENT, registered.getRole(), "Public registration must only produce STUDENT accounts");
    }

    @Test
    @DisplayName("Admin registration creates user with ADMIN role")
    void testRegisterAdminRoleEnforcement() throws SQLException {
        String user = "sys_admin_" + System.currentTimeMillis();
        User registered = authService.registerAdmin(user, "AdminPass123!", "System Admin", user + "@test.org");
        assertNotNull(registered);
        assertEquals(User.Role.ADMIN, registered.getRole(), "Admin registration must produce ADMIN accounts");
    }

    @Test
    @DisplayName("Registration rejects duplicate username and duplicate email")
    void testDuplicateUsernameAndEmailRejection() throws SQLException {
        String username = "unique_user_" + System.currentTimeMillis();
        String email = username + "@example.com";
        authService.registerStudent(username, "Secret123!", "Unique User", email);

        // Duplicate username with different email
        assertThrows(IllegalArgumentException.class, () ->
                authService.registerStudent(username, "Secret123!", "Duplicate User", "different_" + email));

        // Duplicate email with different username
        assertThrows(IllegalArgumentException.class, () ->
                authService.registerStudent("diff_" + username, "Secret123!", "Another User", email));
    }

    @Test
    @DisplayName("Invalid password strictly rejects authentication")
    void testInvalidPasswordFails() {
        Optional<User> result = authService.authenticate("admin", "incorrect_password");
        assertTrue(result.isEmpty(), "Incorrect password must return empty optional");
    }

    @Test
    @DisplayName("Non-existent user identifier returns empty optional")
    void testUnknownUserFails() {
        Optional<User> result = authService.authenticate("unknown_user_999", "admin123");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Empty or null credentials return empty optional")
    void testEmptyCredentials() {
        assertTrue(authService.authenticate(null, "pass").isEmpty());
        assertTrue(authService.authenticate("admin", null).isEmpty());
        assertTrue(authService.authenticate("   ", "pass").isEmpty());
        assertTrue(authService.authenticate("admin", "   ").isEmpty());
    }

    @Test
    @DisplayName("Register new student hashes password and enables authentication")
    void testRegisterNewStudent() throws SQLException {
        String username = "student_test_" + System.currentTimeMillis();
        String pass = "SecurePass123!";

        User user = authService.register(username, pass, "Test Candidate", username + "@example.com", User.Role.STUDENT);
        assertNotNull(user);
        assertEquals(username, user.getUsername());
        assertEquals(User.Role.STUDENT, user.getRole());

        // Authenticate with new account
        Optional<User> authOpt = authService.authenticate(username, pass);
        assertTrue(authOpt.isPresent());
        assertEquals(username, authOpt.get().getUsername());
    }

    @Test
    @DisplayName("Registration rejects short usernames and passwords")
    void testRegistrationValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                authService.register("ab", "pass123", "Short User", "short@test.com", User.Role.STUDENT));

        assertThrows(IllegalArgumentException.class, () ->
                authService.register("valid_user", "123", "Short Pass", "pass@test.com", User.Role.STUDENT));

        assertThrows(IllegalArgumentException.class, () ->
                authService.register("valid_user", "pass123", "   ", "blank@test.com", User.Role.STUDENT));
    }

    @Test
    @DisplayName("getAllUsers returns active user accounts")
    void testGetAllUsers() {
        List<User> list = authService.getAllUsers();
        assertNotNull(list);
        assertFalse(list.isEmpty());
    }

    @Test
    @DisplayName("Database connection failure is captured and tracked during authentication fallback")
    void testDatabaseErrorTracksErrorState() {
        stubUserDAO.setSimulateDatabaseFailure(true);
        Optional<User> userOpt = authService.authenticate("student1", "student123");
        assertTrue(userOpt.isPresent(), "Demo fallback allows authentication");
        assertFalse(authService.isLastAuthUsedDatabase(), "Must flag that database was not used");
        assertNotNull(authService.getLastDatabaseError(), "Must record database error message");
        assertTrue(authService.getLastDatabaseError().contains("Simulated database failure"));
    }

    @Test
    @DisplayName("Successful database authentication sets isLastAuthUsedDatabase flag")
    void testSuccessfulDatabaseAuthenticationSetsFlag() throws SQLException {
        User dbStudent = new User("db_student", com.onlineexam.config.BCrypt.hashpw("student123", com.onlineexam.config.BCrypt.gensalt()), "DB Student", "db@student.com", User.Role.STUDENT);
        stubUserDAO.create(dbStudent);

        Optional<User> userOpt = authService.authenticate("db_student", "student123");
        assertTrue(userOpt.isPresent());
        assertTrue(authService.isLastAuthUsedDatabase());
        assertNull(authService.getLastDatabaseError());
    }

    // --- Stub DAO for Unit Testing ---

    private static class StubUserDAO extends UserDAO {
        private final Map<Long, User> db = new HashMap<>();
        private boolean simulateDatabaseFailure = false;

        void setSimulateDatabaseFailure(boolean fail) {
            this.simulateDatabaseFailure = fail;
        }

        @Override
        public Optional<User> findById(Long id) throws SQLException {
            if (simulateDatabaseFailure) {
                throw new SQLException("Simulated database failure: connection refused");
            }
            return Optional.ofNullable(db.get(id));
        }

        @Override
        public Optional<User> findByUsername(String username) throws SQLException {
            if (simulateDatabaseFailure) {
                throw new SQLException("Simulated database failure: connection refused");
            }
            return db.values().stream().filter(u -> u.getUsername().equalsIgnoreCase(username)).findFirst();
        }

        @Override
        public Optional<User> findByEmail(String email) throws SQLException {
            if (simulateDatabaseFailure) {
                throw new SQLException("Simulated database failure: connection refused");
            }
            return db.values().stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
        }

        @Override
        public User create(User user) throws SQLException {
            long newId = db.size() + 100L;
            user.setId(newId);
            db.put(newId, user);
            return user;
        }

        @Override
        public List<User> findAll() throws SQLException {
            if (simulateDatabaseFailure) {
                throw new SQLException("Simulated database failure: connection refused");
            }
            return new ArrayList<>(db.values());
        }

        @Override
        public boolean delete(Long id) {
            return db.remove(id) != null;
        }
    }
}
